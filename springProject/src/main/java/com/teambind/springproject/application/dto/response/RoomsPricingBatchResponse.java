package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Room ID 리스트 기반 배치 조회 응답 DTO.
 * 요청된 Room들의 가격 정보를 포함합니다.
 *
 * @param rooms 요청된 Room들의 가격 정보 리스트
 */
public record RoomsPricingBatchResponse(
		List<RoomPricingInfo> rooms
) {

	/**
	 * PricingPolicy 리스트로부터 RoomsPricingBatchResponse를 생성합니다.
	 *
	 * @param policies 가격 정책 리스트
	 * @return RoomsPricingBatchResponse
	 */
	public static RoomsPricingBatchResponse from(final List<PricingPolicy> policies) {
		final List<RoomPricingInfo> roomInfos = policies.stream()
				.map(RoomPricingInfo::from)
				.collect(Collectors.toList());

		return new RoomsPricingBatchResponse(roomInfos);
	}

	/**
	 * PricingPolicy 리스트와 날짜로부터 시간대별 가격을 포함한 응답을 생성합니다.
	 *
	 * @param policies 가격 정책 리스트
	 * @param date     조회할 날짜
	 * @return RoomsPricingBatchResponse with time slot prices
	 */
	public static RoomsPricingBatchResponse fromWithDate(
			final List<PricingPolicy> policies,
			final LocalDate date) {

		final List<RoomPricingInfo> roomInfos = policies.stream()
				.map(policy -> RoomPricingInfo.fromWithDate(policy, date))
				.collect(Collectors.toList());

		return new RoomsPricingBatchResponse(roomInfos);
	}

	/**
	 * PricingPolicy 리스트와 Optional 날짜로부터 응답을 생성합니다.
	 *
	 * @param policies 가격 정책 리스트
	 * @param date     조회할 날짜 (Optional)
	 * @return RoomsPricingBatchResponse
	 */
	public static RoomsPricingBatchResponse from(
			final List<PricingPolicy> policies,
			final Optional<LocalDate> date) {

		if (date.isPresent()) {
			return fromWithDate(policies, date.get());
		} else {
			return from(policies);
		}
	}

	/**
	 * 빈 응답을 생성합니다.
	 *
	 * @return 빈 Room 리스트를 가진 응답
	 */
	public static RoomsPricingBatchResponse empty() {
		return new RoomsPricingBatchResponse(List.of());
	}

	/**
	 * 응답이 비어있는지 확인합니다.
	 *
	 * @return Room이 없으면 true
	 */
	public boolean isEmpty() {
		return rooms == null || rooms.isEmpty();
	}

	/**
	 * 조회된 Room 개수를 반환합니다.
	 *
	 * @return Room 개수
	 */
	public int size() {
		return rooms != null ? rooms.size() : 0;
	}
}