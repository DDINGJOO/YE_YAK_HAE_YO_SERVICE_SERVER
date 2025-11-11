package com.teambind.springproject.domain.product.relationship;

import com.teambind.springproject.domain.shared.ProductId;

/**
 * 룸별 허용 상품을 나타내는 Value Object (Record).
 * 플레이스 어드민이 룸별로 허용할 PLACE Scope 상품을 관리합니다.
 *
 * @param roomId    룸 ID (Place 서비스 참조)
 * @param productId 허용된 PLACE 상품 ID
 */
public record RoomAllowedProduct(
		Long roomId,
		ProductId productId
) {
	
	/**
	 * Compact Constructor - 불변식 검증.
	 */
	public RoomAllowedProduct {
		if (roomId == null) {
			throw new IllegalArgumentException("Room ID cannot be null");
		}
		if (roomId <= 0) {
			throw new IllegalArgumentException("Room ID must be positive: " + roomId);
		}
		if (productId == null) {
			throw new IllegalArgumentException("Product ID cannot be null");
		}
	}
}
