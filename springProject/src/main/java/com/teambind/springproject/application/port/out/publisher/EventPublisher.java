package com.teambind.springproject.application.port.out.publisher;


import com.teambind.springproject.adapter.out.messaging.kafka.event.Event;

/**
 * 이벤트 발행 Port
 *
 *  헥사고날 아키텍처의 출력 포트로, 도메인 이벤트를 외부 메시징 시스템으로 발행
 */
public interface EventPublisher {
	
	void publish(Event event);
}
