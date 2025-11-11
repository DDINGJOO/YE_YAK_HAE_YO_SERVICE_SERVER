package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;

/**
 * 가격 정책 생성 Use Case.
 * RoomCreatedEvent 수신 시 기본 가격 정책을 생성합니다.
 */
public interface CreatePricingPolicyUseCase {
	
	/**
	 * RoomCreatedEvent로부터 받은 정보로 기본 가격 정책을 생성합니다.
	 * 초기 가격은 0원으로 설정되며, 이후 관리자가 API를 통해 수정합니다.
	 *
	 * @param roomId   룸 ID
	 * @param placeId  장소 ID
	 * @param timeSlot 시간 단위
	 * @return 생성된 가격 정책
	 */
	PricingPolicy createDefaultPolicy(RoomId roomId, PlaceId placeId, TimeSlot timeSlot);
}
