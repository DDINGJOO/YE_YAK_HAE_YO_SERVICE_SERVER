package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.Event;

/**
 * 이벤트 핸들러 인터페이스.
 * 특정 타입의 이벤트를 처리하는 핸들러를 정의합니다.
 *
 * @param <T> 처리할 이벤트 타입
 */
public interface EventHandler<T extends Event> {
	
	/**
	 * 이벤트를 처리합니다.
	 *
	 * @param event 처리할 이벤트
	 */
	void handle(T event);
	
	/**
	 * 이 핸들러가 지원하는 이벤트 타입 이름을 반환합니다.
	 *
	 * @return 이벤트 타입 이름
	 */
	String getSupportedEventType();
}
