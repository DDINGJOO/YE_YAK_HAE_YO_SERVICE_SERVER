package com.teambind.springproject.adapter.in.messaging.kafka.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 룸 생성 이벤트.
 * Place 서비스에서 룸이 생성되었을 때 발행되는 이벤트입니다.
 * <p>
 * 이 이벤트를 수신하면 해당 룸에 대한 가격 정책을 자동으로 생성합니다.
 */
public final class RoomCreatedEvent extends Event {
	
	private static final String EVENT_TYPE_NAME = "RoomCreated";
	
	private final Long placeId;
	private final Long roomId;
	private final String timeSlot;
	
	@JsonCreator
	public RoomCreatedEvent(
			@JsonProperty("topic") final String topic,
			@JsonProperty("eventType") final String eventType,
			@JsonProperty("placeId") final Long placeId,
			@JsonProperty("roomId") final Long roomId,
			@JsonProperty("timeSlot") final String timeSlot) {
		super(topic, eventType);
		this.placeId = placeId;
		this.roomId = roomId;
		this.timeSlot = timeSlot;
	}
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE_NAME;
	}
	
	public Long getPlaceId() {
		return placeId;
	}
	
	public Long getRoomId() {
		return roomId;
	}
	
	public String getTimeSlot() {
		return timeSlot;
	}
	
	@Override
	public String toString() {
		return "RoomCreatedEvent{"
				+ "placeId=" + placeId
				+ ", roomId=" + roomId
				+ ", timeSlot='" + timeSlot + '\''
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}
