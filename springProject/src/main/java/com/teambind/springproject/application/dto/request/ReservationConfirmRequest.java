package com.teambind.springproject.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teambind.springproject.adapter.out.messaging.kafka.event.dto.ProductPriceDetailDto;
import com.teambind.springproject.application.dto.response.ReservationTimePriceDetail;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 확정 요청 DTO.
 * 외부 예약 관리 서비스로 HTTP 요청 시 사용되는 요청 본문입니다.
 *
 * ID 필드들은 외부 시스템 호환성을 위해 String으로 직렬화됩니다.
 */
@Getter
public class ReservationConfirmRequest {

	@JsonProperty("reservationId")
	private final String reservationId;

	@JsonProperty("placeId")
	private final String placeId;

	@JsonProperty("roomId")
	private final String roomId;

	@JsonProperty("reservationDate")
	private final String reservationDate;

	@JsonProperty("productPriceDetails")
	private final List<ProductPriceDetailDto> productPriceDetails;

	@JsonProperty("reservationTimePriceDetail")
	private final ReservationTimePriceDetail reservationTimePriceDetail;

	@JsonProperty("totalPrice")
	private final BigDecimal totalPrice;

	@JsonProperty("occurredAt")
	private final String occurredAt;

	private ReservationConfirmRequest(
			final String reservationId,
			final String placeId,
			final String roomId,
			final String reservationDate,
			final List<ProductPriceDetailDto> productPriceDetails,
			final ReservationTimePriceDetail reservationTimePriceDetail,
			final BigDecimal totalPrice,
			final String occurredAt) {
		this.reservationId = reservationId;
		this.placeId = placeId;
		this.roomId = roomId;
		this.reservationDate = reservationDate;
		this.productPriceDetails = productPriceDetails;
		this.reservationTimePriceDetail = reservationTimePriceDetail;
		this.totalPrice = totalPrice;
		this.occurredAt = occurredAt;
	}

	/**
	 * 예약 정보와 가격 정책으로부터 확정 요청 DTO를 생성합니다.
	 * Factory Method Pattern을 사용하여 객체 생성 책임을 캡슐화합니다.
	 *
	 * @param reservation   예약 정보
	 * @param pricingPolicy 가격 정책
	 * @return 예약 확정 요청 DTO
	 */
	public static ReservationConfirmRequest from(
			final ReservationPricing reservation,
			final PricingPolicy pricingPolicy) {

		final TimeSlotPriceBreakdown timeSlotBreakdown = reservation.getTimeSlotBreakdown();
		final List<LocalDateTime> timeSlots = new ArrayList<>(timeSlotBreakdown.slotPrices().keySet());
		final LocalDate reservationDate = timeSlots.get(0).toLocalDate();

		final ReservationTimePriceDetail timePriceDetail = ReservationTimePriceDetail.from(reservation);

		final List<ProductPriceDetailDto> productPriceDetailDtos = reservation.getProductBreakdowns()
				.stream()
				.map(breakdown -> ProductPriceDetailDto.from(
						new com.teambind.springproject.application.dto.response.ProductPriceDetail(
								breakdown.productId().getValue(),
								breakdown.productName(),
								breakdown.quantity(),
								breakdown.unitPrice().getAmount(),
								breakdown.totalPrice().getAmount()
						)
				))
				.collect(Collectors.toList());

		final String formattedDate = reservationDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
		final String formattedOccurredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

		return new ReservationConfirmRequest(
				String.valueOf(reservation.getReservationId().getValue()),
				String.valueOf(pricingPolicy.getPlaceId().getValue()),
				String.valueOf(reservation.getRoomId().getValue()),
				formattedDate,
				productPriceDetailDtos,
				timePriceDetail,
				reservation.getTotalPrice().getAmount(),
				formattedOccurredAt
		);
	}
}