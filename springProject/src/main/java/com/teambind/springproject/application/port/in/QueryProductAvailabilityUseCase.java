package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.ProductAvailabilityRequest;
import com.teambind.springproject.application.dto.response.ProductAvailabilityResponse;

/**
 * 상품 재고 가용성 조회 Use Case.
 * 특정 시간대에 예약 가능한 상품 목록과 각 상품의 가용 수량을 조회합니다.
 */
public interface QueryProductAvailabilityUseCase {
	
	/**
	 * 상품 재고 가용성을 조회합니다.
	 *
	 * @param request 조회 요청 (roomId, placeId, timeSlots)
	 * @return 가용한 상품 목록 및 수량
	 */
	ProductAvailabilityResponse queryAvailability(ProductAvailabilityRequest request);
}
