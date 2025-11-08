package com.teambind.springproject.application.service.pricingpolicy;

import com.teambind.springproject.application.port.in.CreatePricingPolicyUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가격 정책 생성 서비스.
 * RoomCreatedEvent 처리를 위한 Application Service입니다.
 */
@Service
@Transactional
public class CreatePricingPolicyService implements CreatePricingPolicyUseCase {

  private static final Logger logger = LoggerFactory.getLogger(
      CreatePricingPolicyService.class);

  private final PricingPolicyRepository pricingPolicyRepository;

  public CreatePricingPolicyService(final PricingPolicyRepository pricingPolicyRepository) {
    this.pricingPolicyRepository = pricingPolicyRepository;
  }

  @Override
  public PricingPolicy createDefaultPolicy(
      final RoomId roomId,
      final PlaceId placeId,
      final TimeSlot timeSlot) {

    logger.info("Creating default pricing policy for roomId={}, placeId={}, timeSlot={}",
        roomId.getValue(), placeId.getValue(), timeSlot);

    // 이미 정책이 존재하는지 확인
    if (pricingPolicyRepository.existsById(roomId)) {
      logger.warn("Pricing policy already exists for roomId={}", roomId.getValue());
      return pricingPolicyRepository.findById(roomId)
          .orElseThrow(() -> new IllegalStateException(
              "Pricing policy should exist but not found"));
    }

    // 기본 가격 0원으로 정책 생성
    final PricingPolicy policy = PricingPolicy.create(
        roomId,
        placeId,
        timeSlot,
        Money.of(BigDecimal.ZERO)
    );

    final PricingPolicy savedPolicy = pricingPolicyRepository.save(policy);

    logger.info("Successfully created default pricing policy for roomId={}",
        roomId.getValue());

    return savedPolicy;
  }
}
