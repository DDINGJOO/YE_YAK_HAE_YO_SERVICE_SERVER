package com.teambind.springproject.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 기본 가격 업데이트 요청 DTO.
 */
public record UpdateDefaultPriceRequest(
		@NotNull(message = "Default price is required")
		@DecimalMin(value = "0.0", inclusive = true, message = "Default price must be greater than or equal to 0")
		BigDecimal defaultPrice
) {
}
