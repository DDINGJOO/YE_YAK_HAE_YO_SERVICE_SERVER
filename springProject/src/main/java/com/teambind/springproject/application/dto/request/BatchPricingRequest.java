package com.teambind.springproject.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/**
 * Room ID 리스트 기반 배치 가격 조회 요청 DTO.
 *
 * @param roomIds 조회할 Room ID 리스트
 * @param date    조회할 날짜 (선택적, 시간대별 가격 조회용)
 */
public record BatchPricingRequest(
		@NotNull(message = "Room IDs cannot be null")
		@NotEmpty(message = "Room IDs cannot be empty")
		List<@NotNull @Positive(message = "Room ID must be positive") Long> roomIds,

		LocalDate date
) {
	/**
	 * 날짜 없이 Room ID 리스트만으로 요청을 생성합니다.
	 *
	 * @param roomIds Room ID 리스트
	 * @return BatchPricingRequest
	 */
	public static BatchPricingRequest of(final List<Long> roomIds) {
		return new BatchPricingRequest(roomIds, null);
	}

	/**
	 * Room ID 리스트와 날짜를 포함한 요청을 생성합니다.
	 *
	 * @param roomIds Room ID 리스트
	 * @param date    조회할 날짜
	 * @return BatchPricingRequest
	 */
	public static BatchPricingRequest of(final List<Long> roomIds, final LocalDate date) {
		return new BatchPricingRequest(roomIds, date);
	}

	/**
	 * 날짜가 포함되었는지 확인합니다.
	 *
	 * @return 날짜가 있으면 true
	 */
	public boolean hasDate() {
		return date != null;
	}
}