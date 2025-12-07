package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.RoomId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Room ID 리스트 기반 가격 정책 배치 조회 UseCase.
 * 여러 Room ID를 받아 해당 Room들의 가격 정보를 한 번에 조회합니다.
 */
public interface GetRoomsPricingBatchUseCase {

	/**
	 * Room ID 리스트를 기반으로 가격 정책들을 조회합니다.
	 *
	 * @param roomIds 조회할 Room ID 리스트
	 * @return 요청된 Room들의 가격 정책 리스트
	 */
	List<PricingPolicy> getPricingByRoomIds(List<RoomId> roomIds);

	/**
	 * Room ID 리스트를 기반으로 가격 정책과 특정 날짜의 시간대별 가격을 조회합니다.
	 *
	 * @param roomIds 조회할 Room ID 리스트
	 * @param date    조회할 날짜 (Optional)
	 * @return 요청된 Room들의 가격 정책 리스트
	 */
	List<PricingPolicy> getPricingByRoomIds(List<RoomId> roomIds, Optional<LocalDate> date);
}