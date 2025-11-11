package com.teambind.springproject.domain.reservationpricing.exception;

/**
 * 예약 가격 정보를 찾을 수 없을 때 발생하는 예외.
 */
public class ReservationPricingNotFoundException extends ReservationPricingException {
	
	public ReservationPricingNotFoundException() {
		super(ReservationPricingErrorCode.RESERVATION_PRICING_NOT_FOUND);
	}
	
	public ReservationPricingNotFoundException(final String message) {
		super(ReservationPricingErrorCode.RESERVATION_PRICING_NOT_FOUND, message);
	}
	
	public ReservationPricingNotFoundException(final Long reservationId) {
		super(ReservationPricingErrorCode.RESERVATION_PRICING_NOT_FOUND,
				"Reservation pricing not found: reservationId=" + reservationId);
	}
	
	@Override
	public String getExceptionType() {
		return "ReservationPricingNotFoundException";
	}
}
