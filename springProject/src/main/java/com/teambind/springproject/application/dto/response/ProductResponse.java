package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.application.dto.request.PricingStrategyDto;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.vo.ProductScope;

/**
 * 상품 응답 DTO.
 */
public record ProductResponse(
    Long productId,
    ProductScope scope,
    Long placeId,
    Long roomId,
    String name,
    PricingStrategyDto pricingStrategy,
    Integer totalQuantity
) {

  /**
   * Product 도메인 객체로부터 Response DTO를 생성합니다.
   *
   * @param product 상품 도메인 객체
   * @return ProductResponse
   */
  public static ProductResponse from(final Product product) {
    final Long placeId = product.getPlaceId() != null
        ? product.getPlaceId().getValue()
        : null;

    final Long roomId = product.getRoomId() != null
        ? product.getRoomId().getValue()
        : null;

    return new ProductResponse(
        product.getProductId().getValue(),
        product.getScope(),
        placeId,
        roomId,
        product.getName(),
        PricingStrategyDto.from(product.getPricingStrategy()),
        product.getTotalQuantity()
    );
  }
}
