package com.teambind.springproject.adapter.out.messaging.kafka.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teambind.springproject.application.dto.response.ProductPriceDetail;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 상품별 가격 상세 정보 DTO.
 * Kafka 메시지로 발행될 때 사용되는 외부 계약 표현입니다.
 *
 * productId는 외부 시스템 호환성을 위해 String으로 직렬화됩니다.
 */
@Getter
public class ProductPriceDetailDto {

	@JsonProperty("productId")
	private final String productId;

	@JsonProperty("productName")
	private final String productName;

	@JsonProperty("quantity")
	private final int quantity;

	@JsonProperty("unitPrice")
	private final BigDecimal unitPrice;

	@JsonProperty("subtotal")
	private final BigDecimal subtotal;

	private ProductPriceDetailDto(
			final String productId,
			final String productName,
			final int quantity,
			final BigDecimal unitPrice,
			final BigDecimal subtotal) {
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.subtotal = subtotal;
	}

	/**
	 * ProductPriceDetail로부터 DTO를 생성합니다.
	 *
	 * @param detail 상품 가격 상세
	 * @return Kafka 발행용 DTO
	 */
	public static ProductPriceDetailDto from(final ProductPriceDetail detail) {
		return new ProductPriceDetailDto(
				String.valueOf(detail.productId()),
				detail.productName(),
				detail.quantity(),
				detail.unitPrice(),
				detail.subtotal()
		);
	}
}