package com.teambind.springproject.adapter.out.persistence.pricingpolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PricingPolicyEntity를 위한 Spring Data JPA Repository.
 */
public interface PricingPolicyJpaRepository extends
		JpaRepository<PricingPolicyEntity, RoomIdEmbeddable> {

	/**
	 * PlaceId로 모든 PricingPolicyEntity를 조회합니다.
	 * fetch join을 사용하여 N+1 문제를 방지합니다.
	 *
	 * @param placeId 장소 ID
	 * @return 해당 Place의 모든 가격 정책 엔티티
	 */
	@Query("SELECT p FROM PricingPolicyEntity p " +
			"LEFT JOIN FETCH p.timeRangePrices " +
			"WHERE p.placeId = :placeId")
	List<PricingPolicyEntity> findAllByPlaceId(@Param("placeId") Long placeId);

	/**
	 * Room ID 리스트로 PricingPolicyEntity들을 조회합니다.
	 * fetch join을 사용하여 N+1 문제를 방지합니다.
	 *
	 * @param roomIds Room ID 리스트
	 * @return 요청된 Room들의 가격 정책 엔티티
	 */
	@Query("SELECT DISTINCT p FROM PricingPolicyEntity p " +
			"LEFT JOIN FETCH p.timeRangePrices " +
			"WHERE p.roomId IN :roomIds")
	List<PricingPolicyEntity> findAllByRoomIdIn(@Param("roomIds") List<RoomIdEmbeddable> roomIds);
}
