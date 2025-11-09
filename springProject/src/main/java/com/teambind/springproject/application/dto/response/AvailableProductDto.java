package com.teambind.springproject.application.dto.response;

import java.math.BigDecimal;

/**
 * 가용한 상품 정보 DTO.
 */
public record AvailableProductDto(
    Long productId,
    String productName,
    BigDecimal unitPrice,
    int availableQuantity,
    int totalStock
) {

}
