package com.teambind.springproject.application.port.in;

import com.teambind.springproject.domain.shared.ProductId;

/**
 * 상품 삭제 Use Case.
 * Hexagonal Architecture의 입력 포트(Input Port)입니다.
 */
public interface DeleteProductUseCase {

  /**
   * 상품을 삭제합니다.
   *
   * @param productId 상품 ID
   * @throws java.util.NoSuchElementException 상품이 존재하지 않는 경우
   */
  void delete(ProductId productId);
}
