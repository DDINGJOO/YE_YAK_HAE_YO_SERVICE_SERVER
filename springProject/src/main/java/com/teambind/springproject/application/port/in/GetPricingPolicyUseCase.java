package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.RoomId;

/**
 * 가격 정책 조회 Use Case.
 */
public interface GetPricingPolicyUseCase {

  /**
   * 룸 ID로 가격 정책을 조회합니다.
   *
   * @param roomId 룸 ID
   * @return 가격 정책
   * @throws IllegalStateException 가격 정책이 존재하지 않을 시
   */
  PricingPolicy getPolicy(RoomId roomId);
}
