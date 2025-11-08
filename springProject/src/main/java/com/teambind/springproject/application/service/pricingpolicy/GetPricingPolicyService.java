package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.GetPricingPolicyUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.RoomId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가격 정책 조회 서비스.
 */
@Service
@Transactional(readOnly = true)
public class GetPricingPolicyService implements GetPricingPolicyUseCase {

  private static final Logger logger = LoggerFactory.getLogger(GetPricingPolicyService.class);

  private final PricingPolicyRepository pricingPolicyRepository;

  public GetPricingPolicyService(final PricingPolicyRepository pricingPolicyRepository) {
    this.pricingPolicyRepository = pricingPolicyRepository;
  }

  @Override
  public PricingPolicy getPolicy(final RoomId roomId) {
    logger.info("Fetching pricing policy for roomId={}", roomId.getValue());

    return pricingPolicyRepository.findById(roomId)
        .orElseThrow(() -> new PricingPolicyNotFoundException(
            "Pricing policy not found for roomId: " + roomId.getValue()));
  }
}
