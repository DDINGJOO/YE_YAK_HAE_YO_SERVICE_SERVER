package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.PlaceId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PlaceId 기반 가격 정책 배치 조회 UseCase.
 * 특정 Place에 속한 모든 Room의 가격 정보를 한 번에 조회합니다.
 */
public interface GetPlacePricingBatchUseCase {

	/**
	 * PlaceId를 기반으로 모든 Room의 가격 정책을 조회합니다.
	 *
	 * @param placeId 조회할 장소 ID
	 * @return 해당 Place에 속한 모든 Room의 가격 정책 리스트
	 */
	List<PricingPolicy> getPricingByPlace(PlaceId placeId);

	/**
	 * PlaceId를 기반으로 모든 Room의 가격 정책과 특정 날짜의 시간대별 가격을 조회합니다.
	 *
	 * @param placeId 조회할 장소 ID
	 * @param date    조회할 날짜 (Optional)
	 * @return 해당 Place에 속한 모든 Room의 가격 정책 리스트
	 */
	List<PricingPolicy> getPricingByPlace(PlaceId placeId, Optional<LocalDate> date);
}