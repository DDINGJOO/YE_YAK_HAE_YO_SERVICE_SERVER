package com.teambind.springproject.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 생성 요청 DTO.
 */
public record CreateReservationRequest(
		@NotNull(message = "Room ID is required")
		Long roomId,
		
		@NotEmpty(message = "Time slots cannot be empty")
		List<LocalDateTime> timeSlots,
		

		List<ProductRequest> products
) {
}
