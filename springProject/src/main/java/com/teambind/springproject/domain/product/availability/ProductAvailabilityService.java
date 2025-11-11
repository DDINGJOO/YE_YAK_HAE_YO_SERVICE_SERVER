package com.teambind.springproject.domain.product.availability;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * <p>Strategy Pattern을 적용하여 Scope별 검증 로직을 분리했습니다.
 * 각 Scope는 자신에 맞는 ScopedAvailabilityChecker 구현체를 가집니다.
 *
 * @see Product
 * @see ProductScope
 * @see ScopedAvailabilityChecker
 */
@Service
public class ProductAvailabilityService {

  private final Map<ProductScope, ScopedAvailabilityChecker> checkers;

  public ProductAvailabilityService() {
    final TimeScopedChecker timeScopedChecker = new TimeScopedChecker();
    this.checkers = Map.of(
        ProductScope.RESERVATION, new ReservationScopedChecker(),
        ProductScope.PLACE, timeScopedChecker,
        ProductScope.ROOM, timeScopedChecker
    );
  }

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

    final ScopedAvailabilityChecker checker = checkers.get(product.getScope());
    return checker.isAvailable(product, requestedSlots, requestedQuantity, overlappingReservations);
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

    final ScopedAvailabilityChecker checker = checkers.get(product.getScope());
    return checker.calculateAvailableQuantity(product, requestedSlots, overlappingReservations);
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
}
