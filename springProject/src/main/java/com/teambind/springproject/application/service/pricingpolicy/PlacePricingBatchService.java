package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.GetPlacePricingBatchUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.PlaceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PlaceId 기반 가격 정책 배치 조회 서비스.
 * GetPlacePricingBatchUseCase의 구현체입니다.
 */
@Service
@Transactional(readOnly = true)
public class PlacePricingBatchService implements GetPlacePricingBatchUseCase {

	private final PricingPolicyRepository pricingPolicyRepository;

	public PlacePricingBatchService(final PricingPolicyRepository pricingPolicyRepository) {
		this.pricingPolicyRepository = pricingPolicyRepository;
	}

	/**
	 * PlaceId를 기반으로 모든 Room의 가격 정책을 조회합니다.
	 *
	 * @param placeId 조회할 장소 ID
	 * @return 해당 Place에 속한 모든 Room의 가격 정책 리스트
	 */
	@Override
	public List<PricingPolicy> getPricingByPlace(final PlaceId placeId) {
		validatePlaceId(placeId);
		return pricingPolicyRepository.findAllByPlaceId(placeId);
	}

	/**
	 * PlaceId를 기반으로 모든 Room의 가격 정책과 특정 날짜의 시간대별 가격을 조회합니다.
	 *
	 * @param placeId 조회할 장소 ID
	 * @param date    조회할 날짜 (Optional)
	 * @return 해당 Place에 속한 모든 Room의 가격 정책 리스트
	 */
	@Override
	public List<PricingPolicy> getPricingByPlace(final PlaceId placeId, final Optional<LocalDate> date) {
		validatePlaceId(placeId);

		final List<PricingPolicy> policies = pricingPolicyRepository.findAllByPlaceId(placeId);

		// date 파라미터가 제공된 경우, 추가적인 처리를 위해 정책들을 반환
		// Controller에서 날짜별 가격 계산을 수행
		return policies;
	}

	private void validatePlaceId(final PlaceId placeId) {
		if (placeId == null) {
			throw new IllegalArgumentException("PlaceId cannot be null");
		}
	}
}