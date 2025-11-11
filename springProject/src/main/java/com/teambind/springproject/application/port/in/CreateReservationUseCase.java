package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;

/**
 * 예약 생성 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface CreateReservationUseCase {
	
	/**
	 * 예약을 생성합니다.
	 * 가격 정책을 조회하고, 상품 재고를 검증한 후, 가격을 계산하여 예약을 생성합니다.
	 * 초기 상태는 PENDING입니다.
	 *
	 * @param request 예약 생성 요청
	 * @return 생성된 예약 정보
	 * @throws IllegalArgumentException 재고 부족 시
	 */
	ReservationPricingResponse createReservation(CreateReservationRequest request);
	
	/**
	 * 예약을 확정합니다.
	 * 상태를 PENDING에서 CONFIRMED로 변경합니다.
	 *
	 * @param reservationId 예약 ID
	 * @return 확정된 예약 정보
	 */
	ReservationPricingResponse confirmReservation(Long reservationId);
	
	/**
	 * 예약을 취소합니다.
	 * 상태를 CANCELLED로 변경합니다.
	 *
	 * @param reservationId 예약 ID
	 * @return 취소된 예약 정보
	 */
	ReservationPricingResponse cancelReservation(Long reservationId);
}
