package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.GetRoomsPricingBatchUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.RoomId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Room ID 리스트 기반 가격 정책 배치 조회 서비스.
 * GetRoomsPricingBatchUseCase의 구현체입니다.
 */
@Service
@Transactional(readOnly = true)
public class RoomsPricingBatchService implements GetRoomsPricingBatchUseCase {

	private final PricingPolicyRepository pricingPolicyRepository;

	public RoomsPricingBatchService(final PricingPolicyRepository pricingPolicyRepository) {
		this.pricingPolicyRepository = pricingPolicyRepository;
	}

	/**
	 * Room ID 리스트를 기반으로 가격 정책들을 조회합니다.
	 *
	 * @param roomIds 조회할 Room ID 리스트
	 * @return 요청된 Room들의 가격 정책 리스트
	 */
	@Override
	public List<PricingPolicy> getPricingByRoomIds(final List<RoomId> roomIds) {
		validateRoomIds(roomIds);
		return pricingPolicyRepository.findAllByRoomIds(roomIds);
	}

	/**
	 * Room ID 리스트를 기반으로 가격 정책과 특정 날짜의 시간대별 가격을 조회합니다.
	 *
	 * @param roomIds 조회할 Room ID 리스트
	 * @param date    조회할 날짜 (Optional)
	 * @return 요청된 Room들의 가격 정책 리스트
	 */
	@Override
	public List<PricingPolicy> getPricingByRoomIds(final List<RoomId> roomIds, final Optional<LocalDate> date) {
		validateRoomIds(roomIds);

		final List<PricingPolicy> policies = pricingPolicyRepository.findAllByRoomIds(roomIds);

		// date 파라미터가 제공된 경우, Controller에서 날짜별 가격 계산을 수행
		return policies;
	}

	private void validateRoomIds(final List<RoomId> roomIds) {
		if (roomIds == null) {
			throw new IllegalArgumentException("Room IDs cannot be null");
		}
		if (roomIds.isEmpty()) {
			throw new IllegalArgumentException("Room IDs cannot be empty");
		}
		if (roomIds.stream().anyMatch(id -> id == null)) {
			throw new IllegalArgumentException("Room ID list cannot contain null values");
		}
	}
}