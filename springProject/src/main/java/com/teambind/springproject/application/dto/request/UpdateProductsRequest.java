package com.teambind.springproject.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 예약 상품 업데이트 요청 DTO.
 */
public record UpdateProductsRequest(
    @NotNull(message = "Products list is required")
    @Valid
    List<ProductRequest> products
) {
}
