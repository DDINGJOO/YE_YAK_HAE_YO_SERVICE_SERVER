package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.CopyPricingPolicyUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.exception.CannotCopyDifferentPlaceException;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.RoomId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가격 정책 복사 서비스.
 * 같은 PlaceId를 가진 룸 간에만 가격 정책을 복사할 수 있습니다.
 */
@Service
@Transactional
public class CopyPricingPolicyService implements CopyPricingPolicyUseCase {
	
	private static final Logger logger = LoggerFactory.getLogger(CopyPricingPolicyService.class);
	
	private final PricingPolicyRepository pricingPolicyRepository;
	
	public CopyPricingPolicyService(final PricingPolicyRepository pricingPolicyRepository) {
		this.pricingPolicyRepository = pricingPolicyRepository;
	}
	
	@Override
	public PricingPolicy copyFromRoom(final RoomId targetRoomId, final RoomId sourceRoomId) {
		logger.info("Copying pricing policy from roomId={} to roomId={}",
				sourceRoomId.getValue(), targetRoomId.getValue());
		
		// 원본 정책 조회
		final PricingPolicy sourcePolicy = pricingPolicyRepository.findById(sourceRoomId)
				.orElseThrow(() -> new PricingPolicyNotFoundException(
						"Source pricing policy not found for roomId: " + sourceRoomId.getValue()));
		
		// 대상 정책 조회
		final PricingPolicy targetPolicy = pricingPolicyRepository.findById(targetRoomId)
				.orElseThrow(() -> new PricingPolicyNotFoundException(
						"Target pricing policy not found for roomId: " + targetRoomId.getValue()));
		
		// 같은 PlaceId 검증
		if (!sourcePolicy.getPlaceId().equals(targetPolicy.getPlaceId())) {
			logger.warn("Cannot copy pricing policy between different places. "
							+ "Source placeId={}, Target placeId={}",
					sourcePolicy.getPlaceId().getValue(), targetPolicy.getPlaceId().getValue());
			throw new CannotCopyDifferentPlaceException(
					"Cannot copy pricing policy between different places. "
							+ "Source and target rooms must belong to the same place.");
		}
		
		// 가격 정보 복사
		targetPolicy.updateDefaultPrice(sourcePolicy.getDefaultPrice());
		targetPolicy.resetPrices(sourcePolicy.getTimeRangePrices());
		
		final PricingPolicy updatedPolicy = pricingPolicyRepository.save(targetPolicy);
		
		logger.info("Successfully copied pricing policy from roomId={} to roomId={}",
				sourceRoomId.getValue(), targetRoomId.getValue());
		
		return updatedPolicy;
	}
}
