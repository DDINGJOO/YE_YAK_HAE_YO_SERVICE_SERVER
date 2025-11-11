package com.teambind.springproject.domain.pricingpolicy;

import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeRange;

/**
 * 특정 요일의 특정 시간대에 적용되는 가격을 표현하는 Value Object.
 * Record로 구현하여 불변성을 보장합니다.
 */
public record TimeRangePrice(
		DayOfWeek dayOfWeek,
		TimeRange timeRange,
		Money pricePerSlot
) {
	
	public TimeRangePrice {
		if (dayOfWeek == null) {
			throw new IllegalArgumentException("Day of week cannot be null");
		}
		if (timeRange == null) {
			throw new IllegalArgumentException("Time range cannot be null");
		}
		if (pricePerSlot == null) {
			throw new IllegalArgumentException("Price per slot cannot be null");
		}
	}
	
	/**
	 * 다른 TimeRangePrice와 겹치는지 확인합니다.
	 * 같은 요일에서 시간 범위가 겹치면 true를 반환합니다.
	 *
	 * @param other 비교할 다른 TimeRangePrice
	 * @return 겹치면 true, 아니면 false
	 */
	public boolean overlaps(final TimeRangePrice other) {
		if (other == null) {
			throw new IllegalArgumentException("Other TimeRangePrice cannot be null");
		}
		return this.dayOfWeek == other.dayOfWeek && this.timeRange.overlaps(other.timeRange);
	}
}
