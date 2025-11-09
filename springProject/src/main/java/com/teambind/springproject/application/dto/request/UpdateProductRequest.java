package com.teambind.springproject.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 상품 수정 요청 DTO.
 */
public record UpdateProductRequest(
    @NotBlank(message = "Product name is required")
    String name,

    @NotNull(message = "Pricing strategy is required")
    @Valid
    PricingStrategyDto pricingStrategy,

    @NotNull(message = "Total quantity is required")
    @Min(value = 0, message = "Total quantity must be greater than or equal to 0")
    Integer totalQuantity
) {
}
