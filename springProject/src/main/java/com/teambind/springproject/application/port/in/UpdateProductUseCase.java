package com.teambind.springproject.application.port.in;

import com.teambind.springproject.application.dto.request.UpdateProductRequest;
import com.teambind.springproject.application.dto.response.ProductResponse;
import com.teambind.springproject.domain.shared.ProductId;

/**
 * 상품 수정 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface UpdateProductUseCase {

  /**
   * 상품 정보를 수정합니다.
   *
   * @param productId 상품 ID
   * @param request 수정 요청
   * @return 수정된 상품 정보
   * @throws java.util.NoSuchElementException 상품이 존재하지 않는 경우
   */
  ProductResponse update(ProductId productId, UpdateProductRequest request);
}
