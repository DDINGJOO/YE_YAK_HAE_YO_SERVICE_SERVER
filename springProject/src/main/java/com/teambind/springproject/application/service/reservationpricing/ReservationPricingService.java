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
import com.teambind.springproject.domain.product.availability.ProductAvailabilityService;
import com.teambind.springproject.domain.product.vo.ProductPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.exception.ProductNotAvailableException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    validateProductAvailability(products, request.products(), request.timeSlots());

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
    validateProductAvailability(products, request.products(), timeSlots);

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
   * N+1 쿼리를 방지하기 위해 일괄 조회합니다.
   */
  private List<Product> fetchProducts(final List<ProductRequest> productRequests) {
    // 1. ProductId 목록 추출
    final List<ProductId> productIds = productRequests.stream()
        .map(req -> ProductId.of(req.productId()))
        .toList();

    // 2. 일괄 조회 (1번의 쿼리)
    final List<Product> foundProducts = productRepository.findAllById(productIds);

    // 3. 존재하지 않는 상품 검증
    if (foundProducts.size() != productIds.size()) {
      final List<ProductId> foundIds = foundProducts.stream()
          .map(Product::getProductId)
          .toList();
      final List<Long> missingIds = productIds.stream()
          .filter(id -> !foundIds.contains(id))
          .map(ProductId::getValue)
          .toList();
      throw new ReservationPricingNotFoundException(
          "Products not found: " + missingIds);
    }

    // 4. 요청 순서대로 정렬 (productRequests 순서 보장)
    final java.util.Map<ProductId, Product> productMap = foundProducts.stream()
        .collect(Collectors.toMap(Product::getProductId, p -> p));

    return productIds.stream()
        .map(productMap::get)
        .toList();
  }

  /**
   * 상품 재고 가용성을 검증합니다.
   *
   * @param products 검증할 상품 목록
   * @param productRequests 상품 요청 목록 (수량 포함)
   * @param timeSlots 예약 시간 슬롯 목록
   * @throws ProductNotAvailableException 재고가 부족한 경우
   */
  private void validateProductAvailability(
      final List<Product> products,
      final List<ProductRequest> productRequests,
      final List<LocalDateTime> timeSlots) {

    for (int i = 0; i < products.size(); i++) {
      final Product product = products.get(i);
      final ProductRequest productRequest = productRequests.get(i);

      // Scope별로 overlapping reservations 조회
      final List<ReservationPricing> overlappingReservations =
          getOverlappingReservations(product, timeSlots);

      final boolean available = productAvailabilityService.isAvailable(
          product,
          timeSlots,
          productRequest.quantity(),
          overlappingReservations
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
   * 상품의 Scope에 따라 시간대가 겹치는 예약 목록을 조회합니다.
   *
   * @param product 상품
   * @param timeSlots 시간 슬롯 목록
   * @return 겹치는 예약 목록 (RESERVATION Scope인 경우 빈 리스트)
   */
  private List<ReservationPricing> getOverlappingReservations(
      final Product product,
      final List<LocalDateTime> timeSlots) {

    if (timeSlots == null || timeSlots.isEmpty()) {
      return List.of();
    }

    final LocalDateTime start = timeSlots.get(0);
    final LocalDateTime end = timeSlots.get(timeSlots.size() - 1);

    return switch (product.getScope()) {
      case RESERVATION -> List.of();  // RESERVATION Scope는 시간과 무관
      case PLACE -> reservationPricingRepository.findByPlaceIdAndTimeRange(
          product.getPlaceId(),
          start,
          end,
          List.of(
              ReservationStatus.PENDING,
              ReservationStatus.CONFIRMED
          )
      );
      case ROOM -> reservationPricingRepository.findByRoomIdAndTimeRange(
          product.getRoomId(),
          start,
          end,
          List.of(
              ReservationStatus.PENDING,
              ReservationStatus.CONFIRMED
          )
      );
    };
  }
}
