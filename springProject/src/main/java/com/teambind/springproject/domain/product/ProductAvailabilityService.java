package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.ProductId;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 상품 재고 가용성을 검증하는 Domain Service.
 *
 * <p>이 서비스는 Product Aggregate의 복잡한 재고 검증 로직을 담당합니다.
 * Scope별로 다른 재고 확인 방식을 적용합니다:
 * <ul>
 *   <li>PLACE: 플레이스 전체의 시간대별 재고 집계</li>
 *   <li>ROOM: 특정 룸의 시간대별 재고 집계</li>
 *   <li>RESERVATION: 단순 총 재고만 확인 (시간 무관)</li>
 * </ul>
 *
 * <p>DDD Domain Service 패턴을 따릅니다:
 * <ul>
 *   <li>상태를 갖지 않음 (Stateless)</li>
 *   <li>여러 Aggregate를 조율하는 로직 포함</li>
 *   <li>순수한 도메인 객체만 의존 (Infrastructure 계층 의존 제거)</li>
 * </ul>
 *
 * @see Product
 * @see ProductScope
 */
@Service
public class ProductAvailabilityService {

  /**
   * 상품이 요청한 시간대와 수량에 대해 가용한지 확인합니다.
   *
   * @param product 확인할 상품
   * @param requestedSlots 요청 시간 슬롯 목록 (PLACE, ROOM Scope에서 필요)
   * @param requestedQuantity 요청 수량
   * @param overlappingReservations 시간대가 겹치는 예약 목록 (PLACE, ROOM Scope에서 필요)
   * @return 가용하면 true, 아니면 false
   * @throws IllegalArgumentException 수량이 0 이하이거나, Scope에 따라 필수 파라미터가 누락된 경우
   */
  public boolean isAvailable(
      final Product product,
      final List<LocalDateTime> requestedSlots,
      final int requestedQuantity,
      final List<ReservationPricing> overlappingReservations) {

    validateRequestedQuantity(requestedQuantity);

    return switch (product.getScope()) {
      case RESERVATION -> checkSimpleStockAvailability(product, requestedQuantity);
      case PLACE -> checkPlaceScopedAvailability(
          product, requestedSlots, requestedQuantity, overlappingReservations);
      case ROOM -> checkRoomScopedAvailability(
          product, requestedSlots, requestedQuantity, overlappingReservations);
    };
  }

  /**
   * 상품의 가용 수량을 계산합니다.
   * 총 재고에서 요청 시간대에 이미 사용 중인 최대 수량을 뺀 값을 반환합니다.
   *
   * @param product 확인할 상품
   * @param requestedSlots 요청 시간 슬롯 목록
   * @param overlappingReservations 시간대가 겹치는 예약 목록
   * @return 가용한 수량 (0 이상)
   */
  public int calculateAvailableQuantity(
      final Product product,
      final List<LocalDateTime> requestedSlots,
      final List<ReservationPricing> overlappingReservations) {

    return switch (product.getScope()) {
      case RESERVATION -> product.getTotalQuantity();
      case PLACE -> calculatePlaceScopedAvailableQuantity(product, requestedSlots, overlappingReservations);
      case ROOM -> calculateRoomScopedAvailableQuantity(product, requestedSlots, overlappingReservations);
    };
  }

  /**
   * PLACE Scope 상품의 가용 수량을 계산합니다.
   *
   * @param product 확인할 상품
   * @param requestedSlots 요청 시간 슬롯 목록
   * @param overlappingReservations 시간대가 겹치는 예약 목록
   * @return 가용한 수량
   */
  private int calculatePlaceScopedAvailableQuantity(
      final Product product,
      final List<LocalDateTime> requestedSlots,
      final List<ReservationPricing> overlappingReservations) {

    validateTimeSlots(requestedSlots);

    final int maxUsedQuantity = requestedSlots.stream()
        .mapToInt(slot -> calculateUsedAtSlot(overlappingReservations, product.getProductId(),
            slot))
        .max()
        .orElse(0);

    return Math.max(0, product.getTotalQuantity() - maxUsedQuantity);
  }

  /**
   * ROOM Scope 상품의 가용 수량을 계산합니다.
   *
   * @param product 확인할 상품
   * @param requestedSlots 요청 시간 슬롯 목록
   * @param overlappingReservations 시간대가 겹치는 예약 목록
   * @return 가용한 수량
   */
  private int calculateRoomScopedAvailableQuantity(
      final Product product,
      final List<LocalDateTime> requestedSlots,
      final List<ReservationPricing> overlappingReservations) {

    validateTimeSlots(requestedSlots);

    final int maxUsedQuantity = requestedSlots.stream()
        .mapToInt(slot -> calculateUsedAtSlot(overlappingReservations, product.getProductId(),
            slot))
        .max()
        .orElse(0);

    return Math.max(0, product.getTotalQuantity() - maxUsedQuantity);
  }

  /**
   * RESERVATION Scope 상품의 재고 가용성을 확인합니다.
   * 시간과 무관하게 총 재고량만 확인합니다.
   *
   * @param product 확인할 상품
   * @param requestedQuantity 요청 수량
   * @return 가용하면 true, 아니면 false
   */
  private boolean checkSimpleStockAvailability(
      final Product product,
      final int requestedQuantity) {

    return requestedQuantity <= product.getTotalQuantity();
  }

  /**
   * PLACE Scope 상품의 재고 가용성을 확인합니다.
   * 플레이스 전체에서 요청 시간대별로 사용 중인 최대 재고를 계산합니다.
   *
   * @param product 확인할 상품
   * @param requestedSlots 요청 시간 슬롯 목록
   * @param requestedQuantity 요청 수량
   * @param overlappingReservations 시간대가 겹치는 예약 목록
   * @return 가용하면 true, 아니면 false
   */
  private boolean checkPlaceScopedAvailability(
      final Product product,
      final List<LocalDateTime> requestedSlots,
      final int requestedQuantity,
      final List<ReservationPricing> overlappingReservations) {

    validateTimeSlots(requestedSlots);

    // 각 슬롯별 최대 사용량 계산
    final int maxUsedQuantity = requestedSlots.stream()
        .mapToInt(slot -> calculateUsedAtSlot(overlappingReservations, product.getProductId(),
            slot))
        .max()
        .orElse(0);

    return maxUsedQuantity + requestedQuantity <= product.getTotalQuantity();
  }

  /**
   * ROOM Scope 상품의 재고 가용성을 확인합니다.
   * 특정 룸에서 요청 시간대별로 사용 중인 최대 재고를 계산합니다.
   *
   * @param product 확인할 상품
   * @param requestedSlots 요청 시간 슬롯 목록
   * @param requestedQuantity 요청 수량
   * @param overlappingReservations 시간대가 겹치는 예약 목록
   * @return 가용하면 true, 아니면 false
   */
  private boolean checkRoomScopedAvailability(
      final Product product,
      final List<LocalDateTime> requestedSlots,
      final int requestedQuantity,
      final List<ReservationPricing> overlappingReservations) {

    validateTimeSlots(requestedSlots);

    // 각 슬롯별 최대 사용량 계산
    final int maxUsedQuantity = requestedSlots.stream()
        .mapToInt(slot -> calculateUsedAtSlot(overlappingReservations, product.getProductId(),
            slot))
        .max()
        .orElse(0);

    return maxUsedQuantity + requestedQuantity <= product.getTotalQuantity();
  }

  /**
   * 특정 시간 슬롯에서 해당 상품이 사용 중인 수량을 계산합니다.
   *
   * @param reservations 예약 목록
   * @param productId 상품 ID
   * @param slot 확인할 시간 슬롯
   * @return 사용 중인 수량
   */
  private int calculateUsedAtSlot(
      final List<ReservationPricing> reservations,
      final ProductId productId,
      final LocalDateTime slot) {

    return reservations.stream()
        .filter(reservation -> reservation.getTimeSlotBreakdown().slotPrices().containsKey(slot))
        .flatMap(reservation -> reservation.getProductBreakdowns().stream())
        .filter(breakdown -> breakdown.productId().equals(productId))
        .mapToInt(ProductPriceBreakdown::quantity)
        .sum();
  }

  /**
   * 요청 수량이 유효한지 검증합니다.
   *
   * @param requestedQuantity 요청 수량
   * @throws IllegalArgumentException 수량이 0 이하인 경우
   */
  private void validateRequestedQuantity(final int requestedQuantity) {
    if (requestedQuantity <= 0) {
      throw new IllegalArgumentException(
          "Requested quantity must be positive: " + requestedQuantity);
    }
  }

  /**
   * 시간 슬롯 목록이 유효한지 검증합니다.
   *
   * @param requestedSlots 시간 슬롯 목록
   * @throws IllegalArgumentException 슬롯이 null이거나 비어있는 경우
   */
  private void validateTimeSlots(final List<LocalDateTime> requestedSlots) {
    if (requestedSlots == null || requestedSlots.isEmpty()) {
      throw new IllegalArgumentException("Requested time slots cannot be null or empty");
    }
  }
}
