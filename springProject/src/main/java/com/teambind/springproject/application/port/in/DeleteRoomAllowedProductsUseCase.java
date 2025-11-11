package com.teambind.springproject.application.port.in;

/**
 * 룸 허용 상품 삭제 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface DeleteRoomAllowedProductsUseCase {
	
	/**
	 * 특정 룸의 모든 허용 상품 매핑을 삭제합니다.
	 *
	 * @param roomId 룸 ID
	 */
	void deleteAllowedProducts(Long roomId);
}
