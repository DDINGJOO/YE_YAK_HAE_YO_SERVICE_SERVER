package com.teambind.springproject.application.dto.response;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 특정 날짜의 시간대별 가격 응답 DTO.
 * 시작 시간(예: "11:00")을 키로, 해당 타임슬롯의 가격을 값으로 가지는 Map을 반환합니다.
 */
public record DatePricingResponse(
		Map<String, BigDecimal> timeSlotPrices
) {

	public DatePricingResponse {
		if (timeSlotPrices == null) {
			throw new IllegalArgumentException("Time slot prices cannot be null");
		}
	}

	/**
	 * 시간대별 가격 Map으로부터 Response DTO를 생성합니다.
	 *
	 * @param timeSlotPrices 시간대별 가격 Map (시작 시간 -> 가격)
	 * @return DatePricingResponse
	 */
	public static DatePricingResponse of(final Map<String, BigDecimal> timeSlotPrices) {
		return new DatePricingResponse(timeSlotPrices);
	}
}