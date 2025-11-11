package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;

import java.util.List;

/**
 * 상품 조회 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface GetProductUseCase {
	
	/**
	 * ProductId로 상품을 조회합니다.
	 *
	 * @param productId 상품 ID
	 * @return 상품 정보
	 * @throws java.util.NoSuchElementException 상품이 존재하지 않는 경우
	 */
	ProductResponse getById(ProductId productId);
	
	/**
	 * 모든 상품 목록을 조회합니다.
	 *
	 * @return 상품 목록
	 */
	List<ProductResponse> getAll();
	
	/**
	 * PlaceId로 상품 목록을 조회합니다.
	 * PLACE 범위와 ROOM 범위 상품을 모두 포함합니다.
	 *
	 * @param placeId 플레이스 ID
	 * @return 상품 목록
	 */
	List<ProductResponse> getByPlaceId(PlaceId placeId);
	
	/**
	 * RoomId로 상품 목록을 조회합니다.
	 * ROOM 범위 상품만 반환합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 상품 목록
	 */
	List<ProductResponse> getByRoomId(RoomId roomId);
	
	/**
	 * ProductScope로 상품 목록을 조회합니다.
	 *
	 * @param scope 상품 범위
	 * @return 상품 목록
	 */
	List<ProductResponse> getByScope(ProductScope scope);
}
