package com.teambind.springproject.application.service.reservationpricing;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.request.UpdateProductsRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.in.UpdateReservationProductsUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductAvailabilityService;
import com.teambind.springproject.domain.product.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.exception.ProductNotAvailableException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.RoomId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 가격 계산 Application Service.
 * CreateReservationUseCase와 UpdateReservationProductsUseCase를 구현합니다.
 */
@Service
@Transactional
public class ReservationPricingService implements CreateReservationUseCase,
    UpdateReservationProductsUseCase {

  private static final Logger logger = LoggerFactory.getLogger(ReservationPricingService.class);

  private final PricingPolicyRepository pricingPolicyRepository;
  private final ProductRepository productRepository;
  private final ReservationPricingRepository reservationPricingRepository;
  private final ProductAvailabilityService productAvailabilityService;
  private final long pendingTimeoutMinutes;

  public ReservationPricingService(
      final PricingPolicyRepository pricingPolicyRepository,
      final ProductRepository productRepository,
      final ReservationPricingRepository reservationPricingRepository,
      final ProductAvailabilityService productAvailabilityService,
      final com.teambind.springproject.common.config.ReservationConfiguration reservationConfiguration) {
    this.pricingPolicyRepository = pricingPolicyRepository;
    this.productRepository = productRepository;
    this.reservationPricingRepository = reservationPricingRepository;
    this.productAvailabilityService = productAvailabilityService;
    this.pendingTimeoutMinutes = reservationConfiguration.getPending().getTimeoutMinutes();
  }

  @Override
  public ReservationPricingResponse createReservation(final CreateReservationRequest request) {
    logger.info("Creating reservation: roomId={}, timeSlots={}, products={}",
        request.roomId(), request.timeSlots().size(), request.products().size());

    final RoomId roomId = RoomId.of(request.roomId());

    // 1. 가격 정책 조회
    final PricingPolicy pricingPolicy = pricingPolicyRepository.findById(roomId)
        .orElseThrow(() -> new ReservationPricingNotFoundException(
            "Pricing policy not found for roomId: " + request.roomId()));

    // 2. 상품 목록 조회
    final List<Product> products = fetchProducts(request.products());

    // 3. 재고 검증
    validateProductAvailability(products, request);

    // 4. 시간대별 가격 계산
    final TimeSlotPriceBreakdown timeSlotBreakdown = calculateTimeSlotBreakdown(
        pricingPolicy, request.timeSlots());

    // 5. 상품별 가격 계산
    final List<ProductPriceBreakdown> productBreakdowns = calculateProductBreakdowns(
        products, request.products());

    // 6. 예약 가격 계산 및 생성
    final ReservationPricing reservationPricing = ReservationPricing.calculate(
        ReservationId.of(null),  // Auto-generated
        roomId,
        timeSlotBreakdown,
        productBreakdowns,
        pendingTimeoutMinutes
    );

    // 7. 저장
    final ReservationPricing savedReservation = reservationPricingRepository.save(
        reservationPricing);

    logger.info("Successfully created reservation: reservationId={}, totalPrice={}",
        savedReservation.getReservationId().getValue(),
        savedReservation.getTotalPrice().getAmount());

    return ReservationPricingResponse.from(savedReservation);
  }

  @Override
  public ReservationPricingResponse confirmReservation(final Long reservationId) {
    logger.info("Confirming reservation: reservationId={}", reservationId);

    final ReservationPricing reservation = reservationPricingRepository
        .findById(ReservationId.of(reservationId))
        .orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));

    reservation.confirm();
    final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

    logger.info("Successfully confirmed reservation: reservationId={}", reservationId);

    return ReservationPricingResponse.from(savedReservation);
  }

  @Override
  public ReservationPricingResponse cancelReservation(final Long reservationId) {
    logger.info("Cancelling reservation: reservationId={}", reservationId);

    final ReservationPricing reservation = reservationPricingRepository
        .findById(ReservationId.of(reservationId))
        .orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));

    reservation.cancel();
    final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

    logger.info("Successfully cancelled reservation: reservationId={}", reservationId);

    return ReservationPricingResponse.from(savedReservation);
  }

  @Override
  public ReservationPricingResponse updateProducts(
      final Long reservationId,
      final UpdateProductsRequest request) {

    logger.info("Updating products for reservation: reservationId={}, products={}",
        reservationId, request.products().size());

    // 1. 예약 조회
    final ReservationPricing reservation = reservationPricingRepository
        .findById(ReservationId.of(reservationId))
        .orElseThrow(() -> new ReservationPricingNotFoundException(reservationId));

    // 2. 상품 목록 조회
    final List<Product> products = fetchProducts(request.products());

    // 3. 재고 검증 (예약의 시간대 정보 추출)
    final List<LocalDateTime> timeSlots = extractTimeSlots(reservation);
    validateProductAvailabilityForUpdate(products, request.products(), timeSlots);

    // 4. 상품별 가격 계산
    final List<ProductPriceBreakdown> productBreakdowns = calculateProductBreakdowns(
        products, request.products());

    // 5. 예약 상품 업데이트 (도메인 메서드 호출)
    reservation.updateProducts(productBreakdowns);

    // 6. 저장
    final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

    logger.info("Successfully updated products for reservation: reservationId={}, totalPrice={}",
        reservationId, savedReservation.getTotalPrice().getAmount());

    return ReservationPricingResponse.from(savedReservation);
  }

  /**
   * 상품 목록을 조회합니다.
   */
  private List<Product> fetchProducts(final List<ProductRequest> productRequests) {
    final List<Product> products = new ArrayList<>();
    for (final ProductRequest productRequest : productRequests) {
      final Product product = productRepository.findById(ProductId.of(productRequest.productId()))
          .orElseThrow(() -> new ReservationPricingNotFoundException(
              "Product not found: " + productRequest.productId()));
      products.add(product);
    }
    return products;
  }

  /**
   * 상품 재고 가용성을 검증합니다.
   */
  private void validateProductAvailability(
      final List<Product> products,
      final CreateReservationRequest request) {

    for (int i = 0; i < products.size(); i++) {
      final Product product = products.get(i);
      final ProductRequest productRequest = request.products().get(i);

      final boolean available = productAvailabilityService.isAvailable(
          product,
          request.timeSlots(),
          productRequest.quantity(),
          reservationPricingRepository
      );

      if (!available) {
        throw new ProductNotAvailableException(
            product.getProductId().getValue(), productRequest.quantity());
      }
    }
  }

  /**
   * 시간대별 가격 내역을 계산합니다.
   */
  private TimeSlotPriceBreakdown calculateTimeSlotBreakdown(
      final PricingPolicy pricingPolicy,
      final List<LocalDateTime> timeSlots) {

    final LocalDateTime start = timeSlots.get(0);
    final LocalDateTime end = timeSlots.get(timeSlots.size() - 1)
        .plusMinutes(pricingPolicy.getTimeSlot().getMinutes());

    final PricingPolicy.PriceBreakdown priceBreakdown =
        pricingPolicy.calculatePriceBreakdown(start, end);

    // PriceBreakdown을 TimeSlotPriceBreakdown으로 변환
    final Map<LocalDateTime, Money> slotPrices = new HashMap<>();
    for (final PricingPolicy.SlotPrice slotPrice : priceBreakdown.getSlotPrices()) {
      slotPrices.put(slotPrice.slotTime(), slotPrice.price());
    }

    return new TimeSlotPriceBreakdown(slotPrices, pricingPolicy.getTimeSlot());
  }

  /**
   * 상품별 가격 내역을 계산합니다.
   */
  private List<ProductPriceBreakdown> calculateProductBreakdowns(
      final List<Product> products,
      final List<ProductRequest> productRequests) {

    final List<ProductPriceBreakdown> breakdowns = new ArrayList<>();
    for (int i = 0; i < products.size(); i++) {
      final Product product = products.get(i);
      final ProductRequest productRequest = productRequests.get(i);

      final ProductPriceBreakdown breakdown = product.calculatePrice(productRequest.quantity());
      breakdowns.add(breakdown);
    }
    return breakdowns;
  }

  /**
   * 예약의 TimeSlotPriceBreakdown에서 시간대 목록을 추출합니다.
   */
  private List<LocalDateTime> extractTimeSlots(final ReservationPricing reservation) {
    final TimeSlotPriceBreakdown breakdown = reservation.getTimeSlotBreakdown();
    return new ArrayList<>(breakdown.slotPrices().keySet());
  }

  /**
   * 상품 업데이트 시 재고 가용성을 검증합니다.
   */
  private void validateProductAvailabilityForUpdate(
      final List<Product> products,
      final List<ProductRequest> productRequests,
      final List<LocalDateTime> timeSlots) {

    for (int i = 0; i < products.size(); i++) {
      final Product product = products.get(i);
      final ProductRequest productRequest = productRequests.get(i);

      final boolean available = productAvailabilityService.isAvailable(
          product,
          timeSlots,
          productRequest.quantity(),
          reservationPricingRepository
      );

      if (!available) {
        throw new ProductNotAvailableException(
            product.getProductId().getValue(), productRequest.quantity());
      }
    }
  }
}
