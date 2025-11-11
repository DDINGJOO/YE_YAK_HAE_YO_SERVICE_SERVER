package com.teambind.springproject.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 재고 가용성 조회 요청 DTO.
 */
public record ProductAvailabilityRequest(
		@NotNull(message = "Room ID is required")
		@Positive(message = "Room ID must be positive")
		Long roomId,
		
		@NotNull(message = "Place ID is required")
		@Positive(message = "Place ID must be positive")
		Long placeId,
		
		@NotEmpty(message = "Time slots must not be empty")
		List<LocalDateTime> timeSlots
) {

}
