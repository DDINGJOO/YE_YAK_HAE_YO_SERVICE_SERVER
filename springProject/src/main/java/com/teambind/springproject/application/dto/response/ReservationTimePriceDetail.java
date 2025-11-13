package com.teambind.springproject.application.dto.response;

import com.teambind.springproject.domain.reservationpricing.ReservationPricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public record ReservationTimePriceDetail(
		List<String> startTimes, // "11:00", "12:00", "13:00" ..
		BigDecimal totalReservationTimePrice
) {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	public ReservationTimePriceDetail {
		if (startTimes == null) {
			throw new IllegalArgumentException("Start times cannot be null");
		}
	}

	public static ReservationTimePriceDetail from(final ReservationPricing reservationPricing) {
		final List<String> formattedTimes = reservationPricing.getTimeSlotBreakdown().getSlotTimes()
				.stream()
				.map(localDateTime -> localDateTime.format(TIME_FORMATTER))
				.collect(Collectors.toList());

		return new ReservationTimePriceDetail(
				formattedTimes,
				reservationPricing.getTotalPrice().getAmount()
		);
	}
}
