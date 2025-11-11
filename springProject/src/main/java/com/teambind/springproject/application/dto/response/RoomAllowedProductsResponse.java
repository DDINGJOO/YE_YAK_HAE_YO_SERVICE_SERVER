package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.shared.ProductId;

import java.util.List;

/**
 * 룸 허용 상품 응답 DTO.
 *
 * @param roomId            룸 ID
 * @param allowedProductIds 허용된 PLACE 상품 ID 목록
 */
public record RoomAllowedProductsResponse(
		Long roomId,
		List<Long> allowedProductIds
) {
	
	/**
	 * Domain Model로부터 Response를 생성합니다.
	 *
	 * @param roomId     룸 ID
	 * @param productIds 허용된 상품 ID 목록
	 * @return RoomAllowedProductsResponse
	 */
	public static RoomAllowedProductsResponse from(
			final Long roomId,
			final List<ProductId> productIds) {
		final List<Long> allowedProductIds = productIds.stream()
				.map(ProductId::getValue)
				.toList();
		return new RoomAllowedProductsResponse(roomId, allowedProductIds);
	}
}
