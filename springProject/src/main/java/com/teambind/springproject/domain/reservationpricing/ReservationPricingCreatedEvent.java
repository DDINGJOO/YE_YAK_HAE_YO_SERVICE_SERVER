package com.teambind.springproject.domain.reservationpricing;

import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.RoomId;
import java.time.LocalDateTime;

/**
 * 예약 가격이 생성되었을 때 발생하는 Domain Event.
 *
 * @param reservationId 예약 ID
 * @param roomId 룸 ID
 * @param totalPrice 총 가격
 * @param calculatedAt 계산 시각
 */
public record ReservationPricingCreatedEvent(
    ReservationId reservationId,
    RoomId roomId,
    Money totalPrice,
    LocalDateTime calculatedAt
) {

  public ReservationPricingCreatedEvent {
    if (reservationId == null) {
      throw new IllegalArgumentException("Reservation ID cannot be null");
    }
    if (roomId == null) {
      throw new IllegalArgumentException("Room ID cannot be null");
    }
    if (totalPrice == null) {
      throw new IllegalArgumentException("Total price cannot be null");
    }
    if (calculatedAt == null) {
      throw new IllegalArgumentException("Calculated at cannot be null");
    }
  }

  /**
   * ReservationPricing으로부터 이벤트를 생성합니다.
   */
  public static ReservationPricingCreatedEvent from(final ReservationPricing reservationPricing) {
    return new ReservationPricingCreatedEvent(
        reservationPricing.getReservationId(),
        reservationPricing.getRoomId(),
        reservationPricing.getTotalPrice(),
        reservationPricing.getCalculatedAt()
    );
  }
}
