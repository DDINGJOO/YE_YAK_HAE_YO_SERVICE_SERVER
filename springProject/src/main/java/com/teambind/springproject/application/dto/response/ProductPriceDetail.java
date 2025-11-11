package com.teambind.springproject.application.dto.response;

import java.math.BigDecimal;

/**
 * 상품별 가격 상세 정보 DTO.
 */
public record ProductPriceDetail(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal subtotal
) {

}
