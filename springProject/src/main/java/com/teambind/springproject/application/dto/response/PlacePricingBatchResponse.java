package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.PlaceId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PlaceId 기반 배치 조회 응답 DTO.
 * 특정 Place에 속한 모든 Room의 가격 정보를 포함합니다.
 *
 * @param placeId Place ID
 * @param rooms   해당 Place의 Room 가격 정보 리스트
 */
public record PlacePricingBatchResponse(
		Long placeId,
		List<RoomPricingInfo> rooms
) {

	/**
	 * PricingPolicy 리스트로부터 PlacePricingBatchResponse를 생성합니다.
	 *
	 * @param placeId  장소 ID
	 * @param policies 가격 정책 리스트
	 * @return PlacePricingBatchResponse
	 */
	public static PlacePricingBatchResponse from(
			final PlaceId placeId,
			final List<PricingPolicy> policies) {

		final List<RoomPricingInfo> roomInfos = policies.stream()
				.map(RoomPricingInfo::from)
				.collect(Collectors.toList());

		return new PlacePricingBatchResponse(
				placeId.getValue(),
				roomInfos
		);
	}

	/**
	 * PricingPolicy 리스트와 날짜로부터 시간대별 가격을 포함한 응답을 생성합니다.
	 *
	 * @param placeId  장소 ID
	 * @param policies 가격 정책 리스트
	 * @param date     조회할 날짜
	 * @return PlacePricingBatchResponse with time slot prices
	 */
	public static PlacePricingBatchResponse fromWithDate(
			final PlaceId placeId,
			final List<PricingPolicy> policies,
			final LocalDate date) {

		final List<RoomPricingInfo> roomInfos = policies.stream()
				.map(policy -> RoomPricingInfo.fromWithDate(policy, date))
				.collect(Collectors.toList());

		return new PlacePricingBatchResponse(
				placeId.getValue(),
				roomInfos
		);
	}

	/**
	 * PricingPolicy 리스트와 Optional 날짜로부터 응답을 생성합니다.
	 *
	 * @param placeId  장소 ID
	 * @param policies 가격 정책 리스트
	 * @param date     조회할 날짜 (Optional)
	 * @return PlacePricingBatchResponse
	 */
	public static PlacePricingBatchResponse from(
			final PlaceId placeId,
			final List<PricingPolicy> policies,
			final Optional<LocalDate> date) {

		if (date.isPresent()) {
			return fromWithDate(placeId, policies, date.get());
		} else {
			return from(placeId, policies);
		}
	}

	/**
	 * Room이 없는 경우 빈 응답을 생성합니다.
	 *
	 * @param placeId 장소 ID
	 * @return 빈 Room 리스트를 가진 응답
	 */
	public static PlacePricingBatchResponse empty(final PlaceId placeId) {
		return new PlacePricingBatchResponse(
				placeId.getValue(),
				List.of()
		);
	}

	/**
	 * 응답이 비어있는지 확인합니다.
	 *
	 * @return Room이 없으면 true
	 */
	public boolean isEmpty() {
		return rooms == null || rooms.isEmpty();
	}
}