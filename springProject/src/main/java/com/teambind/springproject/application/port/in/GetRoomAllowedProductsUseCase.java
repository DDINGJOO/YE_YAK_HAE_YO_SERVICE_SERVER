package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.response.RoomAllowedProductsResponse;

/**
 * 룸 허용 상품 조회 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface GetRoomAllowedProductsUseCase {
	
	/**
	 * 특정 룸의 허용 상품 목록을 조회합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 허용된 상품 정보 (매핑이 없으면 빈 리스트)
	 */
	RoomAllowedProductsResponse getAllowedProducts(Long roomId);
}
