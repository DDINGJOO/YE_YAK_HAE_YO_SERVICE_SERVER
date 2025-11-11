package com.teambind.springproject.application.service.product;

import com.teambind.springproject.application.dto.request.ProductAvailabilityRequest;
import com.teambind.springproject.application.dto.response.AvailableProductDto;
import com.teambind.springproject.application.dto.response.ProductAvailabilityResponse;
import com.teambind.springproject.application.port.in.QueryProductAvailabilityUseCase;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.ProductAvailabilityService;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 재고 가용성 조회 Application Service.
 */
@Service
@Transactional(readOnly = true)
public class ProductAvailabilityQueryService implements QueryProductAvailabilityUseCase {

  private static final Logger logger = LoggerFactory.getLogger(
      ProductAvailabilityQueryService.class);

  private final ProductRepository productRepository;
  private final ReservationPricingRepository reservationPricingRepository;
  private final ProductAvailabilityService productAvailabilityService;

  public ProductAvailabilityQueryService(
      final ProductRepository productRepository,
      final ReservationPricingRepository reservationPricingRepository,
      final ProductAvailabilityService productAvailabilityService) {
    this.productRepository = productRepository;
    this.reservationPricingRepository = reservationPricingRepository;
    this.productAvailabilityService = productAvailabilityService;
  }

  @Override
  public ProductAvailabilityResponse queryAvailability(
      final ProductAvailabilityRequest request) {

    logger.info("Querying product availability: roomId={}, placeId={}, timeSlots={}",
        request.roomId(), request.placeId(), request.timeSlots().size());

    final PlaceId placeId = PlaceId.of(request.placeId());
    final RoomId roomId = RoomId.of(request.roomId());

    // 1. 룸에서 접근 가능한 모든 상품 목록 조회 (PLACE, ROOM, RESERVATION scope)
    final List<Product> products = productRepository.findAccessibleProducts(placeId, roomId);

    // 2. 각 상품별 가용 수량 계산
    final List<AvailableProductDto> availableProducts = products.stream()
        .map(product -> {
          // Scope별로 overlapping reservations 조회
          final List<com.teambind.springproject.domain.reservationpricing.ReservationPricing> overlappingReservations =
              getOverlappingReservations(product, request.timeSlots(), placeId, roomId);

          final int availableQuantity = productAvailabilityService.calculateAvailableQuantity(
              product,
              request.timeSlots(),
              overlappingReservations
          );

          return new AvailableProductDto(
              product.getProductId().getValue(),
              product.getName(),
              product.getPricingStrategy().getInitialPrice().getAmount(),
              availableQuantity,
              product.getTotalQuantity()
          );
        })
        .toList();

    logger.info("Found {} available products for roomId={}", availableProducts.size(),
        request.roomId());

    return new ProductAvailabilityResponse(
        request.roomId(),
        request.placeId(),
        availableProducts
    );
  }

  /**
   * 상품의 Scope에 따라 시간대가 겹치는 예약 목록을 조회합니다.
   *
   * @param product 상품
   * @param timeSlots 시간 슬롯 목록
   * @param placeId 플레이스 ID
   * @param roomId 룸 ID
   * @return 겹치는 예약 목록 (RESERVATION Scope인 경우 빈 리스트)
   */
  private List<com.teambind.springproject.domain.reservationpricing.ReservationPricing> getOverlappingReservations(
      final Product product,
      final List<java.time.LocalDateTime> timeSlots,
      final PlaceId placeId,
      final RoomId roomId) {

    if (timeSlots == null || timeSlots.isEmpty()) {
      return List.of();
    }

    final java.time.LocalDateTime start = timeSlots.get(0);
    final java.time.LocalDateTime end = timeSlots.get(timeSlots.size() - 1);

    return switch (product.getScope()) {
      case RESERVATION -> List.of();  // RESERVATION Scope는 시간과 무관
      case PLACE -> reservationPricingRepository.findByPlaceIdAndTimeRange(
          placeId,
          start,
          end,
          List.of(
              com.teambind.springproject.domain.shared.ReservationStatus.PENDING,
              com.teambind.springproject.domain.shared.ReservationStatus.CONFIRMED
          )
      );
      case ROOM -> reservationPricingRepository.findByRoomIdAndTimeRange(
          roomId,
          start,
          end,
          List.of(
              com.teambind.springproject.domain.shared.ReservationStatus.PENDING,
              com.teambind.springproject.domain.shared.ReservationStatus.CONFIRMED
          )
      );
    };
  }
}
