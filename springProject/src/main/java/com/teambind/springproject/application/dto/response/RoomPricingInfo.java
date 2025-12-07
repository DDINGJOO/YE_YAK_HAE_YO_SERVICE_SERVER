package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 개별 Room의 가격 정보를 담는 DTO.
 * PlacePricingBatchResponse의 구성 요소입니다.
 *
 * @param roomId         룸 ID
 * @param timeSlot       시간 단위 (HOUR/HALFHOUR)
 * @param defaultPrice   기본 가격
 * @param timeSlotPrices 시간대별 가격 (선택적)
 */
public record RoomPricingInfo(
		Long roomId,
		String timeSlot,
		BigDecimal defaultPrice,
		Map<String, BigDecimal> timeSlotPrices
) {

	/**
	 * PricingPolicy 도메인 객체로부터 RoomPricingInfo를 생성합니다.
	 *
	 * @param policy 가격 정책
	 * @return RoomPricingInfo
	 */
	public static RoomPricingInfo from(final PricingPolicy policy) {
		return new RoomPricingInfo(
				policy.getRoomId().getValue(),
				policy.getTimeSlot().name(),
				policy.getDefaultPrice().getAmount(),
				null // 날짜 없이 조회 시 시간대별 가격은 null
		);
	}

	/**
	 * PricingPolicy와 날짜를 기반으로 시간대별 가격을 포함한 RoomPricingInfo를 생성합니다.
	 *
	 * @param policy 가격 정책
	 * @param date   조회할 날짜
	 * @return RoomPricingInfo with time slot prices
	 */
	public static RoomPricingInfo fromWithDate(final PricingPolicy policy, final LocalDate date) {
		final Map<String, BigDecimal> prices = calculateTimeSlotPrices(policy, date);

		return new RoomPricingInfo(
				policy.getRoomId().getValue(),
				policy.getTimeSlot().name(),
				policy.getDefaultPrice().getAmount(),
				prices
		);
	}

	/**
	 * 특정 날짜의 시간대별 가격을 계산합니다.
	 * 운영 시간을 00:00 ~ 23:59로 가정하고 TimeSlot 단위로 계산합니다.
	 *
	 * @param policy 가격 정책
	 * @param date   조회할 날짜
	 * @return 시간대별 가격 Map (시간 문자열 -> 가격)
	 */
	private static Map<String, BigDecimal> calculateTimeSlotPrices(
			final PricingPolicy policy,
			final LocalDate date) {

		final Map<String, BigDecimal> prices = new LinkedHashMap<>();
		final LocalDateTime startOfDay = date.atTime(LocalTime.MIN);
		final LocalDateTime endOfDay = date.atTime(LocalTime.of(23, 59));

		// 하루 전체의 가격 내역 계산
		final PricingPolicy.PriceBreakdown breakdown = policy.calculatePriceBreakdown(
				startOfDay,
				endOfDay
		);

		// 각 슬롯별 가격을 Map으로 변환
		for (final PricingPolicy.SlotPrice slotPrice : breakdown.getSlotPrices()) {
			final String timeKey = slotPrice.slotTime().toLocalTime().toString();
			prices.put(timeKey, slotPrice.price().getAmount());
		}

		return prices;
	}
}