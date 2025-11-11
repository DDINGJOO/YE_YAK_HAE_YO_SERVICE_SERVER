package com.teambind.springproject.domain.product;

import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scope별 상품 재고 가용성 검증 전략 인터페이스.
 *
 * <p>Strategy Pattern을 적용하여 ProductScope별로 다른 재고 검증 로직을 캡슐화합니다.
 * 각 Scope(RESERVATION, PLACE, ROOM)에 따라 구현체가 달라집니다.
 *
 * <p>주요 이점:
 * <ul>
 *   <li>OCP 준수: 새 Scope 추가 시 구현체만 추가하면 됨</li>
 *   <li>SRP 준수: 각 Checker가 단일 Scope의 로직만 담당</li>
 *   <li>테스트 용이성: 각 Checker를 독립적으로 테스트 가능</li>
 * </ul>
 *
 * @see ProductAvailabilityService
 * @see ProductScope
 */
public interface ScopedAvailabilityChecker {

  /**
   * 상품이 요청한 시간대와 수량에 대해 가용한지 확인합니다.
   *
   * @param product                 확인할 상품
   * @param requestedSlots          요청 시간 슬롯 목록 (시간 무관 Scope는 무시 가능)
   * @param requestedQuantity       요청 수량
   * @param overlappingReservations 시간대가 겹치는 예약 목록 (시간 무관 Scope는 무시 가능)
   * @return 가용하면 true, 아니면 false
   * @throws IllegalArgumentException 필수 파라미터가 누락된 경우
   */
  boolean isAvailable(
      Product product,
      List<LocalDateTime> requestedSlots,
      int requestedQuantity,
      List<ReservationPricing> overlappingReservations);

  /**
   * 상품의 가용 수량을 계산합니다.
   *
   * @param product                 확인할 상품
   * @param requestedSlots          요청 시간 슬롯 목록 (시간 무관 Scope는 무시 가능)
   * @param overlappingReservations 시간대가 겹치는 예약 목록 (시간 무관 Scope는 무시 가능)
   * @return 가용한 수량 (0 이상)
   * @throws IllegalArgumentException 필수 파라미터가 누락된 경우
   */
  int calculateAvailableQuantity(
      Product product,
      List<LocalDateTime> requestedSlots,
      List<ReservationPricing> overlappingReservations);
}
