package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;

import java.util.List;

/**
 * 룸에서 이용 가능한 상품 조회 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 *
 * 일반 사용자가 예약 시 선택 가능한 상품 목록을 조회하는 기능을 제공합니다.
 * 재고 정보는 포함하지 않으며, 단순 리스트업 용도입니다.
 *
 * 조회되는 상품:
 * - ROOM Scope 상품: 해당 roomId를 가진 상품
 * - PLACE Scope 상품: RoomAllowedProduct 테이블에 허용된 상품
 * - RESERVATION Scope 상품: 모든 룸에서 사용 가능한 상품
 */
public interface GetAvailableProductsForRoomUseCase {

	/**
	 * 특정 룸에서 이용 가능한 모든 상품을 조회합니다.
	 *
	 * @param roomId 룸 ID
	 * @param placeId 플레이스 ID
	 * @return 이용 가능한 상품 목록
	 */
	List<ProductResponse> getAvailableProducts(RoomId roomId, PlaceId placeId);
}