package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.UpdatePricingPolicyUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.pricingpolicy.exception.PricingPolicyNotFoundException;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.RoomId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가격 정책 업데이트 서비스.
 */
@Service
@Transactional
public class UpdatePricingPolicyService implements UpdatePricingPolicyUseCase {

  private static final Logger logger = LoggerFactory.getLogger(
      UpdatePricingPolicyService.class);

  private final PricingPolicyRepository pricingPolicyRepository;

  public UpdatePricingPolicyService(final PricingPolicyRepository pricingPolicyRepository) {
    this.pricingPolicyRepository = pricingPolicyRepository;
  }

  @Override
  public PricingPolicy updateDefaultPrice(final RoomId roomId, final Money defaultPrice) {
    logger.info("Updating default price for roomId={} to {}", roomId.getValue(),
        defaultPrice.getAmount());

    final PricingPolicy policy = pricingPolicyRepository.findById(roomId)
        .orElseThrow(() -> new PricingPolicyNotFoundException(
            "Pricing policy not found for roomId: " + roomId.getValue()));

    policy.updateDefaultPrice(defaultPrice);

    final PricingPolicy updatedPolicy = pricingPolicyRepository.save(policy);

    logger.info("Successfully updated default price for roomId={}", roomId.getValue());

    return updatedPolicy;
  }

  @Override
  public PricingPolicy updateTimeRangePrices(final RoomId roomId,
      final List<TimeRangePrice> timeRangePrices) {
    logger.info("Updating time range prices for roomId={}", roomId.getValue());

    final PricingPolicy policy = pricingPolicyRepository.findById(roomId)
        .orElseThrow(() -> new PricingPolicyNotFoundException(
            "Pricing policy not found for roomId: " + roomId.getValue()));

    final TimeRangePrices newTimeRangePrices = TimeRangePrices.of(timeRangePrices);
    policy.resetPrices(newTimeRangePrices);

    final PricingPolicy updatedPolicy = pricingPolicyRepository.save(policy);

    logger.info("Successfully updated time range prices for roomId={}", roomId.getValue());

    return updatedPolicy;
  }
}
