package com.teambind.springproject.adapter.out.messaging.kafka.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationCancelledEvent;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 예약 취소 이벤트 DTO.
 * Kafka 메시지로 발행될 때 사용되는 외부 계약 표현입니다.
 *
 * ID 필드들은 외부 시스템 호환성을 위해 String으로 직렬화됩니다.
 * 내부 도메인 이벤트({@link ReservationCancelledEvent})와 분리하여
 * 외부 API 변경이 내부 모델에 영향을 주지 않도록 합니다.
 */
@Getter
public class ReservationCancelledEventDto {

	@JsonProperty("topic")
	private final String topic;

	@JsonProperty("eventType")
	private final String eventType;

	@JsonProperty("reservationId")
	private final String reservationId;

	@JsonProperty("roomId")
	private final String roomId;

	@JsonProperty("cancelReason")
	private final String cancelReason;

	@JsonProperty("occurredAt")
	private final LocalDateTime occurredAt;

	private ReservationCancelledEventDto(
			final String topic,
			final String eventType,
			final String reservationId,
			final String roomId,
			final String cancelReason,
			final LocalDateTime occurredAt) {
		this.topic = topic;
		this.eventType = eventType;
		this.reservationId = reservationId;
		this.roomId = roomId;
		this.cancelReason = cancelReason;
		this.occurredAt = occurredAt;
	}

	/**
	 * 도메인 이벤트로부터 DTO를 생성합니다.
	 * Factory Method Pattern을 사용하여 객체 생성 책임을 캡슐화합니다.
	 *
	 * @param event 도메인 이벤트
	 * @return Kafka 발행용 DTO
	 */
	public static ReservationCancelledEventDto from(final ReservationCancelledEvent event) {
		return new ReservationCancelledEventDto(
				event.getTopic(),
				event.getEventType(),
				String.valueOf(event.getReservationId()),
				String.valueOf(event.getRoomId()),
				event.getCancelReason(),
				event.getOccurredAt()
		);
	}
}