package com.teambind.springproject.adapter.in.messaging.kafka.event;

/**
 * 이벤트 추상 클래스.
 * 모든 도메인 이벤트의 기본 클래스입니다.
 *
 * 불변 객체로 설계되어 이벤트의 무결성을 보장합니다.
 */
public abstract class Event {
	
	private final String topic;
	private final String eventType;
	
	protected Event(final String topic, final String eventType) {
		this.topic = topic;
		this.eventType = eventType;
	}
	
	/**
	 * 이벤트 타입 이름을 반환합니다.
	 * 하위 클래스에서 구체적인 이벤트 타입을 정의해야 합니다.
	 *
	 * @return 이벤트 타입 이름
	 */
	public abstract String getEventTypeName();
	
	public String getTopic() {
		return topic;
	}
	
	public String getEventType() {
		return eventType;
	}
}
