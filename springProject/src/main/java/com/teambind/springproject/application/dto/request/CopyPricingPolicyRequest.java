package com.teambind.springproject.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 가격 정책 복사 요청 DTO.
 */
public record CopyPricingPolicyRequest(
		@NotNull(message = "Source room ID is required")
		@Positive(message = "Source room ID must be positive")
		Long sourceRoomId
) {
}
