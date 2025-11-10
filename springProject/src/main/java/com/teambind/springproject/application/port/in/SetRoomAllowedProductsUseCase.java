package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.SetRoomAllowedProductsRequest;
import com.teambind.springproject.application.dto.response.RoomAllowedProductsResponse;

/**
 * 룸 허용 상품 설정 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface SetRoomAllowedProductsUseCase {

  /**
   * 특정 룸의 허용 상품 목록을 설정합니다.
   * 기존 매핑은 모두 삭제되고 새로운 매핑이 저장됩니다.
   *
   * @param roomId 룸 ID
   * @param request 허용 상품 설정 요청
   * @return 설정된 허용 상품 정보
   */
  RoomAllowedProductsResponse setAllowedProducts(Long roomId, SetRoomAllowedProductsRequest request);
}
