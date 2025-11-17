package com.teambind.springproject.adapter.out.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 예약 취소 이벤트.
 * 운영자가 예약을 거절했을 때 발행되는 발신(outbound) 이벤트입니다.
 *
 * 이 이벤트를 다른 서비스(예: 시간관리 서비스)에서 구독하여:
 * - 예약으로 선점한 시간 슬롯 락 해제
 * - 관련 리소스 정리
 */
@Getter
public class ReservationCancelledEvent extends Event {

	private static final String EVENT_TYPE_NAME = "ReservationCancelled";
	private static final String DEFAULT_TOPIC = "reservation-cancelled";

	private final Long reservationId;
	private final Long roomId;
	private final String cancelReason;
	private final LocalDateTime occurredAt;

	@JsonCreator
	public ReservationCancelledEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("reservationId") final Long reservationId,
			@JsonProperty("roomId") final Long roomId,
			@JsonProperty("cancelReason") final String cancelReason,
			@JsonProperty("occurredAt") final LocalDateTime occurredAt) {
		super(topic != null ? topic : DEFAULT_TOPIC, eventType != null ? eventType : EVENT_TYPE_NAME);
		this.reservationId = reservationId;
		this.roomId = roomId;
		this.cancelReason = cancelReason;
		this.occurredAt = occurredAt;
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE_NAME;
	}

	@Override
	public String toString() {
		return "ReservationCancelledEvent{"
				+ "reservationId=" + reservationId
				+ ", roomId=" + roomId
				+ ", cancelReason='" + cancelReason + '\''
				+ ", occurredAt=" + occurredAt
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}