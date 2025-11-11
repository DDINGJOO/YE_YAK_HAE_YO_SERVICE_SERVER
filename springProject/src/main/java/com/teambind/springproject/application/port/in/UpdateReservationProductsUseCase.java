package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.UpdateProductsRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;

/**
 * 예약 상품 업데이트 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface UpdateReservationProductsUseCase {
	
	/**
	 * 예약의 상품 목록을 업데이트하고 가격을 재계산합니다.
	 * PENDING 상태의 예약에서만 상품 업데이트가 가능합니다.
	 * 재고를 검증하고, 성공 시 재고 락(Soft Lock)이 발생합니다.
	 *
	 * @param reservationId 예약 ID
	 * @param request       상품 업데이트 요청 (productId, quantity 목록)
	 * @return 업데이트된 예약 정보
	 * @throws IllegalStateException    PENDING 상태가 아닌 경우
	 * @throws IllegalArgumentException 재고 부족 시
	 */
	ReservationPricingResponse updateProducts(Long reservationId, UpdateProductsRequest request);
}
