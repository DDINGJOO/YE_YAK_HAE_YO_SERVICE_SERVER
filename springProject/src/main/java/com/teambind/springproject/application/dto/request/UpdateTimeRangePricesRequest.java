package com.teambind.springproject.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 시간대별 가격 업데이트 요청 DTO.
 */
public record UpdateTimeRangePricesRequest(
		@NotNull(message = "Time range prices are required")
		@Valid
		List<TimeRangePriceDto> timeRangePrices
) {
}
