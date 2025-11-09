package com.teambind.springproject.domain.shared;

/**
 * 예약 상태를 표현하는 Enum.
 */
public enum ReservationStatus {

  /**
   * 예약 대기 상태.
   * 예약이 생성되었으나 아직 확정되지 않은 상태입니다.
   */
  PENDING,

  /**
   * 예약 확정 상태.
   * 예약이 확정되어 실제 이용 예정인 상태입니다.
   */
  CONFIRMED,

  /**
   * 예약 취소 상태.
   * 예약이 취소되어 재고가 복원된 상태입니다.
   */
  CANCELLED
}
