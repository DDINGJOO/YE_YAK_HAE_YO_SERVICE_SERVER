package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.RegisterProductRequest;
import com.teambind.springproject.application.dto.response.ProductResponse;

/**
 * 상품 등록 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface RegisterProductUseCase {

  /**
   * 상품을 등록합니다.
   *
   * @param request 상품 등록 요청
   * @return 등록된 상품 정보
   * @throws IllegalArgumentException Scope에 따른 ID 검증 실패 시
   */
  ProductResponse register(RegisterProductRequest request);
}
