package com.teambind.springproject.domain.reservationpricing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 예약 가격 도메인 관련 에러 코드.
 */
@Getter
public enum ReservationPricingErrorCode {
	// Reservation Pricing 관련 에러 (RESERVATION_0XX)
	RESERVATION_PRICING_NOT_FOUND("RESERVATION_001", "Reservation pricing not found",
			HttpStatus.NOT_FOUND),
	PRODUCT_NOT_AVAILABLE("RESERVATION_002", "Product is not available",
			HttpStatus.BAD_REQUEST),
	INVALID_RESERVATION_STATUS("RESERVATION_003", "Invalid reservation status transition",
			HttpStatus.BAD_REQUEST),
	PRICING_POLICY_NOT_FOUND("RESERVATION_004", "Pricing policy not found for the room",
			HttpStatus.NOT_FOUND),
	PRODUCT_NOT_FOUND("RESERVATION_005", "Product not found",
			HttpStatus.NOT_FOUND),
	;
	
	private final String errCode;
	private final String message;
	private final HttpStatus status;
	
	ReservationPricingErrorCode(final String errCode, final String message,
	                            final HttpStatus status) {
		this.errCode = errCode;
		this.message = message;
		this.status = status;
	}
	
	@Override
	public String toString() {
		return "ReservationPricingErrorCode{"
				+ " status='"
				+ status
				+ '\''
				+ "errCode='"
				+ errCode
				+ '\''
				+ ", message='"
				+ message
				+ '\''
				+ '}';
	}
}
