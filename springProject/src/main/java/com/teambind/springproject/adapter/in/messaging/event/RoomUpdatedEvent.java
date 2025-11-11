package com.teambind.springproject.adapter.in.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 룸 업데이트 이벤트.
 * Place 서비스에서 룸이 업데이트되었을 때 발행되는 이벤트입니다.
 * <p>
 * 이 이벤트를 수신하면 해당 룸에 대한 가격 정책을 업데이트합니다.
 */
public final class RoomUpdatedEvent extends Event {
	
	private static final String EVENT_TYPE_NAME = "RoomUpdated";
	
	private final Long placeId;
	private final Long roomId;
	private final String timeSlot;
	
	@JsonCreator
	public RoomUpdatedEvent(
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
		return "RoomUpdatedEvent{"
				+ "placeId=" + placeId
				+ ", roomId=" + roomId
				+ ", timeSlot='" + timeSlot + '\''
				+ ", topic='" + getTopic() + '\''
				+ ", eventType='" + getEventType() + '\''
				+ '}';
	}
}
