package com.teambind.springproject.adapter.in.web.reservationpricing;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.UpdateProductsRequest;
import com.teambind.springproject.application.dto.response.PricePreviewResponse;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CalculateReservationPriceUseCase;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.in.UpdateReservationProductsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 가격 관리 REST Controller.
 */
@RestController
@RequestMapping("/api/v1/reservations")
@Validated
public class ReservationPricingController {
	
	private final CreateReservationUseCase createReservationUseCase;
	private final CalculateReservationPriceUseCase calculateReservationPriceUseCase;
	private final UpdateReservationProductsUseCase updateReservationProductsUseCase;
	
	public ReservationPricingController(
			final CreateReservationUseCase createReservationUseCase,
			final CalculateReservationPriceUseCase calculateReservationPriceUseCase,
			final UpdateReservationProductsUseCase updateReservationProductsUseCase) {
		this.createReservationUseCase = createReservationUseCase;
		this.calculateReservationPriceUseCase = calculateReservationPriceUseCase;
		this.updateReservationProductsUseCase = updateReservationProductsUseCase;
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
	
	/**
	 * 예약 가격 미리보기.
	 * 예약을 생성하지 않고 가격만 계산하여 반환합니다.
	 *
	 * @param request 예약 요청 정보
	 * @return 가격 미리보기 (시간대 가격 + 상품별 가격 + 총 합계)
	 */
	@PostMapping("/preview")
	public ResponseEntity<PricePreviewResponse> previewPrice(
			@RequestBody @Valid final CreateReservationRequest request) {
		
		final PricePreviewResponse response = calculateReservationPriceUseCase.calculatePrice(request);
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 예약 상품 업데이트 및 가격 재계산.
	 * PENDING 상태의 예약에서만 상품 업데이트가 가능합니다.
	 *
	 * @param reservationId 예약 ID
	 * @param request       상품 업데이트 요청
	 * @return 업데이트된 예약 정보
	 */
	@PutMapping("/{reservationId}/products")
	public ResponseEntity<ReservationPricingResponse> updateProducts(
			@PathVariable @Positive(message = "Reservation ID must be positive") final Long reservationId,
			@RequestBody @Valid final UpdateProductsRequest request) {
		
		final ReservationPricingResponse response = updateReservationProductsUseCase.updateProducts(
				reservationId, request);
		
		return ResponseEntity.ok(response);
	}
}
