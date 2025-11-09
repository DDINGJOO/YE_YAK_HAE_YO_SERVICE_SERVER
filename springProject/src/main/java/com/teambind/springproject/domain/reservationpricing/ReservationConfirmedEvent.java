package com.teambind.springproject.domain.reservationpricing;

import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.RoomId;
import java.time.LocalDateTime;

/**
 * 예약이 확정되었을 때 발생하는 Domain Event.
 *
 * @param reservationId 예약 ID
 * @param roomId 룸 ID
 * @param confirmedAt 확정 시각
 */
public record ReservationConfirmedEvent(
    ReservationId reservationId,
    RoomId roomId,
    LocalDateTime confirmedAt
) {

  public ReservationConfirmedEvent {
    if (reservationId == null) {
      throw new IllegalArgumentException("Reservation ID cannot be null");
    }
    if (roomId == null) {
      throw new IllegalArgumentException("Room ID cannot be null");
    }
    if (confirmedAt == null) {
      throw new IllegalArgumentException("Confirmed at cannot be null");
    }
  }

  /**
   * ReservationPricing으로부터 이벤트를 생성합니다.
   */
  public static ReservationConfirmedEvent from(final ReservationPricing reservationPricing) {
    return new ReservationConfirmedEvent(
        reservationPricing.getReservationId(),
        reservationPricing.getRoomId(),
        LocalDateTime.now()
    );
  }
}
