package com.teambind.springproject.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 예약 생성 시 상품 정보를 담는 VO.
 * CreateReservationRequest의 nested record로 사용됩니다.
 */
public record ProductRequest(
		@NotNull(message = "Product ID is required")
		Long productId,
		
		@NotNull(message = "Quantity is required")
		@Min(value = 1, message = "Quantity must be at least 1")
		Integer quantity
) {
}
