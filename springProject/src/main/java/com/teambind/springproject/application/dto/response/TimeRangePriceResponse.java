package com.teambind.springproject.application.dto.response;

import java.math.BigDecimal;

/**
 * 시간대별 가격 응답 DTO.
 */
public record TimeRangePriceResponse(
    String dayOfWeek,
    String startTime,
    String endTime,
    BigDecimal price
) {
}
