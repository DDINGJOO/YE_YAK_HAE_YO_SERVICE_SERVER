package com.teambind.springproject.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가격 미리보기 응답 DTO.
 */
public record PricePreviewResponse(
		BigDecimal timeSlotPrice,
		List<ProductPriceDetail> productBreakdowns,
		BigDecimal totalPrice
) {

}
