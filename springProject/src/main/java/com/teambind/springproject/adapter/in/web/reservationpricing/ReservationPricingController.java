package com.teambind.springproject.adapter.in.web.reservationpricing;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 가격 관리 REST Controller.
 */
@RestController
@RequestMapping("/api/reservations/pricing")
@Validated
public class ReservationPricingController {

  private final CreateReservationUseCase createReservationUseCase;

  public ReservationPricingController(final CreateReservationUseCase createReservationUseCase) {
    this.createReservationUseCase = createReservationUseCase;
  }

  /**
   * 예약 가격 계산 및 생성.
   *
   * @param request 예약 생성 요청
   * @return 생성된 예약 정보
   */
  @PostMapping
  public ResponseEntity<ReservationPricingResponse> createReservation(
      @RequestBody @Valid final CreateReservationRequest request) {

    final ReservationPricingResponse response = createReservationUseCase.createReservation(
        request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 예약 확정.
   *
   * @param reservationId 예약 ID
   * @return 확정된 예약 정보
   */
  @PutMapping("/{reservationId}/confirm")
  public ResponseEntity<ReservationPricingResponse> confirmReservation(
      @PathVariable @Positive(message = "Reservation ID must be positive") final Long reservationId) {

    final ReservationPricingResponse response = createReservationUseCase.confirmReservation(
        reservationId);

    return ResponseEntity.ok(response);
  }

  /**
   * 예약 취소.
   *
   * @param reservationId 예약 ID
   * @return 취소된 예약 정보
   */
  @PutMapping("/{reservationId}/cancel")
  public ResponseEntity<ReservationPricingResponse> cancelReservation(
      @PathVariable @Positive(message = "Reservation ID must be positive") final Long reservationId) {

    final ReservationPricingResponse response = createReservationUseCase.cancelReservation(
        reservationId);

    return ResponseEntity.ok(response);
  }
}
