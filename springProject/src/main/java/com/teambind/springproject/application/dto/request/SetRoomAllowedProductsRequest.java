package com.teambind.springproject.application.dto.request;

import java.util.List;

/**
 * 룸 허용 상품 설정 요청 DTO.
 *
 * @param productIds 허용할 PLACE 상품 ID 목록
 */
public record SetRoomAllowedProductsRequest(
		List<Long> productIds
) {
	
	public SetRoomAllowedProductsRequest {
		if (productIds == null) {
			throw new IllegalArgumentException("Product IDs cannot be null");
		}
	}
}
