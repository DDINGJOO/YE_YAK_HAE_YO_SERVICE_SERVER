package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.shared.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예약 가격 응답 DTO.
 */
public record ReservationPricingResponse(
		Long reservationId,
		Long roomId,
		ReservationStatus status,
		BigDecimal totalPrice,
		LocalDateTime calculatedAt
) {
	
	/**
	 * ReservationPricing 도메인 객체로부터 Response DTO를 생성합니다.
	 *
	 * @param reservationPricing 예약 가격 도메인 객체
	 * @return ReservationPricingResponse
	 */
	public static ReservationPricingResponse from(final ReservationPricing reservationPricing) {
		return new ReservationPricingResponse(
				reservationPricing.getReservationId().getValue(),
				reservationPricing.getRoomId().getValue(),
				reservationPricing.getStatus(),
				reservationPricing.getTotalPrice().getAmount(),
				reservationPricing.getCalculatedAt()
		);
	}
}
