package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;

import java.util.List;

/**
 * 가격 정책 업데이트 Use Case.
 * 기본 가격, 시간대별 가격, TimeSlot을 수정합니다.
 */
public interface UpdatePricingPolicyUseCase {
	
	/**
	 * 가격 정책의 기본 가격을 업데이트합니다.
	 *
	 * @param roomId       룸 ID
	 * @param defaultPrice 새로운 기본 가격
	 * @return 업데이트된 가격 정책
	 */
	PricingPolicy updateDefaultPrice(RoomId roomId, Money defaultPrice);
	
	/**
	 * 가격 정책의 시간대별 가격을 업데이트합니다.
	 * 기존 시간대별 가격은 모두 삭제되고 새로운 가격으로 대체됩니다.
	 *
	 * @param roomId          룸 ID
	 * @param timeRangePrices 시간대별 가격 리스트
	 * @return 업데이트된 가격 정책
	 */
	PricingPolicy updateTimeRangePrices(RoomId roomId, List<TimeRangePrice> timeRangePrices);
	
	/**
	 * 가격 정책의 TimeSlot을 업데이트합니다.
	 * Room의 운영 시간 정책 변경 시 사용됩니다.
	 *
	 * @param roomId      룸 ID
	 * @param newTimeSlot 새로운 TimeSlot
	 * @return 업데이트된 가격 정책
	 */
	PricingPolicy updateTimeSlot(RoomId roomId, TimeSlot newTimeSlot);
}
