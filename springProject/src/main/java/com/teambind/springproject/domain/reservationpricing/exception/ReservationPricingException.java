package com.teambind.springproject.domain.reservationpricing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 예약 가격 도메인 예외 기본 클래스.
 */
@Getter
public abstract class ReservationPricingException extends RuntimeException {

  private final ReservationPricingErrorCode errorCode;
  private final HttpStatus httpStatus;

  protected ReservationPricingException(final ReservationPricingErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.httpStatus = errorCode.getStatus();
  }

  protected ReservationPricingException(final ReservationPricingErrorCode errorCode,
      final String message) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatus = errorCode.getStatus();
  }

  protected ReservationPricingException(final ReservationPricingErrorCode errorCode,
      final String message,
      final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.httpStatus = errorCode.getStatus();
  }

  public abstract String getExceptionType();
}
