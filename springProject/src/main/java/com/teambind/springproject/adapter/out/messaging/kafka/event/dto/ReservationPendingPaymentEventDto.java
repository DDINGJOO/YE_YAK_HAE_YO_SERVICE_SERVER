package com.teambind.springproject.adapter.out.messaging.kafka.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationPendingPaymentEvent;
import com.teambind.springproject.application.dto.response.ReservationTimePriceDetail;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 결제 대기 이벤트 DTO.
 * Kafka 메시지로 발행될 때 사용되는 외부 계약 표현입니다.
 *
 * ID 필드들은 외부 시스템 호환성을 위해 String으로 직렬화됩니다.
 * 내부 도메인 이벤트({@link ReservationPendingPaymentEvent})와 분리하여
 * 외부 API 변경이 내부 모델에 영향을 주지 않도록 합니다.
 */
@Getter
public class ReservationPendingPaymentEventDto {

	@JsonProperty("topic")
	private final String topic;

	@JsonProperty("eventType")
	private final String eventType;

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

	private ReservationPendingPaymentEventDto(
			final String topic,
			final String eventType,
			final String reservationId,
			final String placeId,
			final String roomId,
			final String reservationDate,
			final List<ProductPriceDetailDto> productPriceDetails,
			final ReservationTimePriceDetail reservationTimePriceDetail,
			final BigDecimal totalPrice,
			final String occurredAt) {
		this.topic = topic;
		this.eventType = eventType;
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
	 * 도메인 이벤트로부터 DTO를 생성합니다.
	 * Factory Method Pattern을 사용하여 객체 생성 책임을 캡슐화합니다.
	 *
	 * @param event 도메인 이벤트
	 * @return Kafka 발행용 DTO
	 */
	public static ReservationPendingPaymentEventDto from(final ReservationPendingPaymentEvent event) {
		final List<ProductPriceDetailDto> productPriceDetailDtos = event.getProductPriceDetails()
				.stream()
				.map(ProductPriceDetailDto::from)
				.collect(Collectors.toList());

		return new ReservationPendingPaymentEventDto(
				event.getTopic(),
				event.getEventType(),
				String.valueOf(event.getReservationId()),
				String.valueOf(event.getPlaceId()),
				String.valueOf(event.getRoomId()),
				event.getReservationDate(),
				productPriceDetailDtos,
				event.getReservationTimePriceDetail(),
				event.getTotalPrice(),
				event.getOccurredAt()
		);
	}
}