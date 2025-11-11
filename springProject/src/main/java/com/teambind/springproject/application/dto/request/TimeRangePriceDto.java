package com.teambind.springproject.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 시간대별 가격 DTO.
 */
public record TimeRangePriceDto(
		@NotNull(message = "Day of week is required")
		String dayOfWeek,
		
		@NotNull(message = "Start time is required")
		String startTime,
		
		@NotNull(message = "End time is required")
		String endTime,
		
		@NotNull(message = "Price is required")
		@DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
		BigDecimal price
) {
}
