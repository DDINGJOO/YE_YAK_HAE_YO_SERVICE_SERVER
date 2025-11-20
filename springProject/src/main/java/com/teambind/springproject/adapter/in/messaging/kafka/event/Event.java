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

	/**
	 * Object를 Long으로 변환합니다.
	 * JSON 역직렬화 시 문자열 또는 숫자 타입의 ID를 Long으로 변환하기 위해 사용됩니다.
	 *
	 * @param value 변환할 값 (Long, Number, String 타입 지원)
	 * @return Long 값
	 * @throws IllegalArgumentException 변환할 수 없는 타입인 경우
	 */
	protected static Long parseLong(final Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		if (value instanceof String) {
			return Long.parseLong((String) value);
		}
		throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Long: " + value);
	}
}
