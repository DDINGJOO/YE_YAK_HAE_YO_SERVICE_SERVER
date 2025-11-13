package com.teambind.springproject.adapter.in.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

/**
 * 슬롯 예약 이벤트.
 * 예약 서비스에서 슬롯이 예약되었을 때 발행되는 이벤트입니다.
 * <p>
 * 이 이벤트를 수신하면 해당 예약에 대한 가격 정보를 Pending 상태로 생성합니다.
 * placeId는 roomId를 통해 PricingPolicy에서 조회하여 사용합니다.
 * 추가상품 정보는 예약 확정 시 업데이트됩니다.
 */
public final class SlotReservedEvent extends Event {
	
	private static final String EVENT_TYPE_NAME = "SlotReserved";
	private static final String DEFAULT_TOPIC = "reservation-reserved";
	
	private final Long roomId;
	private final LocalDate slotDate;
	private final List<LocalTime> startTimes;
	private final Long reservationId;
	private final LocalDateTime occurredAt;
	
	@JsonCreator
	public SlotReservedEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("roomId") final Long roomId,
			@JsonProperty("slotDate") final LocalDate slotDate,
			@JsonProperty("startTimes") final List<LocalTime> startTimes,
			@JsonProperty("reservationId") final Long reservationId,
			@JsonProperty("occurredAt") final LocalDateTime occurredAt) {
		super(topic != null ? topic : DEFAULT_TOPIC, eventType != null ? eventType : EVENT_TYPE_NAME);
		this.roomId = roomId;
		this.slotDate = slotDate;
		this.startTimes = startTimes != null ? List.copyOf(startTimes) : Collections.emptyList();
		this.reservationId = reservationId;
		this.occurredAt = occurredAt;
	}
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE_NAME;
	}
	
	public Long getRoomId() {
		return roomId;
	}
	
	public LocalDate getSlotDate() {
		return slotDate;
	}
	
	public List<LocalTime> getStartTimes() {
		return startTimes;
	}
	
	public Long getReservationId() {
		return reservationId;
	}
	
	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}
	
	@Override
	public String toString() {
		return "SlotReservedEvent{"
				+ "roomId=" + roomId
				+ ", slotDate=" + slotDate
				+ ", startTimes=" + startTimes
				+ ", reservationId=" + reservationId
				+ ", occurredAt=" + occurredAt
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}
