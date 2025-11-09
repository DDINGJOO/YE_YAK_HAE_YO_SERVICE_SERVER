package com.teambind.springproject.domain.reservationpricing.exception;

import com.teambind.springproject.domain.shared.ReservationStatus;

/**
 * 예약 상태 전이가 유효하지 않을 때 발생하는 예외.
 */
public class InvalidReservationStatusException extends ReservationPricingException {

  public InvalidReservationStatusException() {
    super(ReservationPricingErrorCode.INVALID_RESERVATION_STATUS);
  }

  public InvalidReservationStatusException(final String message) {
    super(ReservationPricingErrorCode.INVALID_RESERVATION_STATUS, message);
  }

  public InvalidReservationStatusException(final ReservationStatus currentStatus,
      final String attemptedAction) {
    super(ReservationPricingErrorCode.INVALID_RESERVATION_STATUS,
        "Invalid reservation status transition: currentStatus=" + currentStatus
            + ", attemptedAction=" + attemptedAction);
  }

  @Override
  public String getExceptionType() {
    return "InvalidReservationStatusException";
  }
}
