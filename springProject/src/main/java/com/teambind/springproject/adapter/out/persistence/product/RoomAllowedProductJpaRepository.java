package com.teambind.springproject.adapter.out.persistence.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * RoomAllowedProduct Entity를 위한 Spring Data JPA Repository.
 */
public interface RoomAllowedProductJpaRepository extends
		JpaRepository<RoomAllowedProductEntity, Long> {
	
	/**
	 * 특정 룸의 허용 상품 매핑 목록을 조회합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 허용 상품 엔티티 목록
	 */
	List<RoomAllowedProductEntity> findByRoomId(Long roomId);
	
	/**
	 * 특정 룸의 모든 허용 상품 매핑을 삭제합니다.
	 *
	 * @param roomId 룸 ID
	 */
	@Modifying
	@Query("DELETE FROM RoomAllowedProductEntity r WHERE r.roomId = :roomId")
	void deleteByRoomId(@Param("roomId") Long roomId);
	
	/**
	 * 특정 룸에 허용 상품 매핑이 존재하는지 확인합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 매핑이 존재하면 true, 아니면 false
	 */
	boolean existsByRoomId(Long roomId);
}
