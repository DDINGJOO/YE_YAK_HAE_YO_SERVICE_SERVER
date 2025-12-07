package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;

import java.util.List;
import java.util.Optional;

/**
 * PricingPolicy Aggregate를 영속화하기 위한 Repository Port.
 * Hexagonal Architecture의 출력 포트(Output Port)입니다.
 */
public interface PricingPolicyRepository {
	
	/**
	 * RoomId로 가격 정책을 조회합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 가격 정책 (없으면 Optional.empty())
	 */
	Optional<PricingPolicy> findById(RoomId roomId);
	
	/**
	 * 가격 정책을 저장합니다.
	 * 새로운 정책이면 INSERT, 기존 정책이면 UPDATE합니다.
	 *
	 * @param policy 저장할 가격 정책
	 * @return 저장된 가격 정책
	 */
	PricingPolicy save(PricingPolicy policy);
	
	/**
	 * RoomId로 가격 정책을 삭제합니다.
	 *
	 * @param roomId 룸 ID
	 */
	void deleteById(RoomId roomId);
	
	/**
	 * RoomId에 해당하는 가격 정책이 존재하는지 확인합니다.
	 *
	 * @param roomId 룸 ID
	 * @return 존재하면 true, 아니면 false
	 */
	boolean existsById(RoomId roomId);

	/**
	 * PlaceId로 모든 가격 정책을 조회합니다.
	 * 한 Place에 속한 모든 Room의 가격 정책을 한 번에 조회합니다.
	 *
	 * @param placeId 장소 ID
	 * @return 해당 Place의 모든 가격 정책 리스트
	 */
	List<PricingPolicy> findAllByPlaceId(PlaceId placeId);
}
