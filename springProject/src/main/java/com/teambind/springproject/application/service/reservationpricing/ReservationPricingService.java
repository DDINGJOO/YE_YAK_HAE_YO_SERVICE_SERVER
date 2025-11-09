package com.teambind.springproject.application.service.reservationpricing;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductAvailabilityService;
import com.teambind.springproject.domain.product.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
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
 * CreateReservationUseCase를 구현합니다.
 */
@Service
@Transactional
public class ReservationPricingService implements CreateReservationUseCase {

  private static final Logger logger = LoggerFactory.getLogger(ReservationPricingService.class);

  private final PricingPolicyRepository pricingPolicyRepository;
  private final ProductRepository productRepository;
  private final ReservationPricingRepository reservationPricingRepository;
  private final ProductAvailabilityService productAvailabilityService;

  public ReservationPricingService(
      final PricingPolicyRepository pricingPolicyRepository,
      final ProductRepository productRepository,
      final ReservationPricingRepository reservationPricingRepository,
      final ProductAvailabilityService productAvailabilityService) {
    this.pricingPolicyRepository = pricingPolicyRepository;
    this.productRepository = productRepository;
    this.reservationPricingRepository = reservationPricingRepository;
    this.productAvailabilityService = productAvailabilityService;
  }

  @Override
  public ReservationPricingResponse createReservation(final CreateReservationRequest request) {
    logger.info("Creating reservation: roomId={}, timeSlots={}, products={}",
        request.roomId(), request.timeSlots().size(), request.products().size());

    final RoomId roomId = RoomId.of(request.roomId());

    // 1. 가격 정책 조회
    final PricingPolicy pricingPolicy = pricingPolicyRepository.findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException(
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
        productBreakdowns
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
        .orElseThrow(() -> new IllegalArgumentException(
            "Reservation not found: " + reservationId));

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
        .orElseThrow(() -> new IllegalArgumentException(
            "Reservation not found: " + reservationId));

    reservation.cancel();
    final ReservationPricing savedReservation = reservationPricingRepository.save(reservation);

    logger.info("Successfully cancelled reservation: reservationId={}", reservationId);

    return ReservationPricingResponse.from(savedReservation);
  }

  /**
   * 상품 목록을 조회합니다.
   */
  private List<Product> fetchProducts(final List<ProductRequest> productRequests) {
    final List<Product> products = new ArrayList<>();
    for (final ProductRequest productRequest : productRequests) {
      final Product product = productRepository.findById(ProductId.of(productRequest.productId()))
          .orElseThrow(() -> new IllegalArgumentException(
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
        throw new IllegalArgumentException(
            "Product is not available: productId=" + product.getProductId().getValue()
                + ", requestedQuantity=" + productRequest.quantity());
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
}
