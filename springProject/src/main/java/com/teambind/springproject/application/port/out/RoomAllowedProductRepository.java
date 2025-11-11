package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.shared.ProductId;

import java.util.List;

/**
 * 룸별 허용 상품 매핑을 영속화하기 위한 Repository Port.
 * Hexagonal Architecture의 출력 포트(Output Port)입니다.
 */
public interface RoomAllowedProductRepository {
	
	/**
	 * 특정 룸에서 허용된 PLACE 상품 ID 목록을 조회합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 허용된 상품 ID 목록 (매핑이 없으면 빈 리스트)
	 */
	List<ProductId> findAllowedProductIdsByRoomId(Long roomId);
	
	/**
	 * 특정 룸의 허용 상품 목록을 저장합니다.
	 * 기존 매핑은 모두 삭제되고 새로운 매핑이 저장됩니다.
	 *
	 * @param roomId     룸 ID
	 * @param productIds 허용할 상품 ID 목록
	 */
	void saveAll(Long roomId, List<ProductId> productIds);
	
	/**
	 * 특정 룸의 모든 허용 상품 매핑을 삭제합니다.
	 *
	 * @param roomId 룸 ID
	 */
	void deleteByRoomId(Long roomId);
	
	/**
	 * 특정 룸에 허용 상품 매핑이 존재하는지 확인합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 매핑이 존재하면 true, 아니면 false
	 */
	boolean existsByRoomId(Long roomId);
}
