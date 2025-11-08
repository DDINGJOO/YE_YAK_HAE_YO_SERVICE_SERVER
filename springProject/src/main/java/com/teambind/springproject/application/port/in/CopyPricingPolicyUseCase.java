package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.RoomId;

/**
 * 가격 정책 복사 Use Case.
 * 다른 룸의 가격 정책을 복사하여 현재 룸에 적용합니다.
 *
 * 제약 조건:
 * - 같은 PlaceId를 가진 룸 간에만 복사 가능
 */
public interface CopyPricingPolicyUseCase {

  /**
   * 다른 룸의 가격 정책을 복사합니다.
   * 같은 PlaceId를 가진 룸 간에만 복사 가능합니다.
   *
   * @param targetRoomId 복사할 대상 룸 ID
   * @param sourceRoomId 복사 원본 룸 ID
   * @return 업데이트된 대상 룸의 가격 정책
   * @throws IllegalArgumentException 다른 PlaceId를 가진 룸 간 복사 시도 시
   * @throws IllegalStateException    원본 또는 대상 정책이 존재하지 않을 시
   */
  PricingPolicy copyFromRoom(RoomId targetRoomId, RoomId sourceRoomId);
}
