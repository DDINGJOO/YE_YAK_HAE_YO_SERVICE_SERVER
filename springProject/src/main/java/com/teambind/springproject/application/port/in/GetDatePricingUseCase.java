package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.shared.RoomId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 특정 날짜의 시간대별 가격 조회 Use Case.
 */
public interface GetDatePricingUseCase {

	/**
	 * 특정 날짜의 시간대별 가격을 조회합니다.
	 * 시작 시간(예: "11:00")을 키로, 해당 타임슬롯의 가격을 값으로 가지는 Map을 반환합니다.
	 *
	 * @param roomId 룸 ID
	 * @param date   조회할 날짜
	 * @return 시간대별 가격 Map (시작 시간 -> 가격)
	 * @throws IllegalStateException 가격 정책이 존재하지 않을 시
	 */
	Map<String, BigDecimal> getPricingByDate(RoomId roomId, LocalDate date);
}