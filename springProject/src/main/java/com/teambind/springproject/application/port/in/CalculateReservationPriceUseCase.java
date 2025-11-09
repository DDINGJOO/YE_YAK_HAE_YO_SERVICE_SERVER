package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.response.PricePreviewResponse;

/**
 * 예약 가격 미리보기 Use Case.
 * 예약을 생성하지 않고 가격만 계산하여 반환합니다.
 */
public interface CalculateReservationPriceUseCase {

  /**
   * 예약 가격을 미리 계산합니다.
   *
   * @param request 예약 요청 정보
   * @return 가격 미리보기 결과 (시간대 가격 + 상품별 가격 + 총 합계)
   */
  PricePreviewResponse calculatePrice(CreateReservationRequest request);
}
