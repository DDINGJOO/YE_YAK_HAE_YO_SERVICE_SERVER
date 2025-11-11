package com.teambind.springproject.domain.reservationpricing;

import com.teambind.springproject.domain.shared.ReservationId;
import com.teambind.springproject.domain.shared.RoomId;

import java.time.LocalDateTime;

/**
 * 예약이 취소되었을 때 발생하는 Domain Event.
 *
 * @param reservationId 예약 ID
 * @param roomId        룸 ID
 * @param cancelledAt   취소 시각
 */
public record ReservationCancelledEvent(
		ReservationId reservationId,
		RoomId roomId,
		LocalDateTime cancelledAt
) {
	
	public ReservationCancelledEvent {
		if (reservationId == null) {
			throw new IllegalArgumentException("Reservation ID cannot be null");
		}
		if (roomId == null) {
			throw new IllegalArgumentException("Room ID cannot be null");
		}
		if (cancelledAt == null) {
			throw new IllegalArgumentException("Cancelled at cannot be null");
		}
	}
	
	/**
	 * ReservationPricing으로부터 이벤트를 생성합니다.
	 */
	public static ReservationCancelledEvent from(final ReservationPricing reservationPricing) {
		return new ReservationCancelledEvent(
				reservationPricing.getReservationId(),
				reservationPricing.getRoomId(),
				LocalDateTime.now()
		);
	}
}
