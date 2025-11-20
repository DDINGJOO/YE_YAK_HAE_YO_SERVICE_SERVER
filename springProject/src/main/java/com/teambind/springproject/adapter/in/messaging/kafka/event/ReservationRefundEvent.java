package com.teambind.springproject.adapter.in.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 예약 환불 이벤트.
 * 결제 서비스에서 환불이 완료되거나 관리자가 승인을 취소/거절했을 때 발행되는 이벤트입니다.
 *
 * 이 이벤트를 수신하면 해당 예약의 상태를 CONFIRMED → CANCELLED로 변경하고,
 * 예약으로 선점한 재고를 롤백 처리합니다.
 *
 * 주의: CONFIRMED 상태가 아닌 예약에 대한 환불 이벤트는 예외를 발생시킵니다.
 */
public final class ReservationRefundEvent extends Event {

	private static final String EVENT_TYPE_NAME = "ReservationRefund";
	private static final String DEFAULT_TOPIC = "reservation-refund";

	private final Long reservationId;
	private final LocalDateTime occurredAt;

	@JsonCreator
	public ReservationRefundEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("reservationId") final Object reservationId,
			@JsonProperty("occurredAt") final LocalDateTime occurredAt) {
		super(topic != null ? topic : DEFAULT_TOPIC, eventType != null ? eventType : EVENT_TYPE_NAME);
		this.reservationId = parseLong(reservationId);
		this.occurredAt = occurredAt;
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE_NAME;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	@Override
	public String toString() {
		return "ReservationRefundEvent{"
				+ "reservationId=" + reservationId
				+ ", occurredAt=" + occurredAt
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}
