package com.teambind.springproject.adapter.in.messaging.kafka.consumer;

import com.teambind.springproject.adapter.in.messaging.kafka.event.*;
import com.teambind.springproject.adapter.in.messaging.kafka.handler.EventHandler;
import com.teambind.springproject.common.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kafka 이벤트 Consumer.
 * 이벤트를 수신하고 적절한 핸들러로 라우팅합니다.
 */
@Component
@Transactional
public class KafkaEventConsumer {

	private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);
	private static final Map<String, Class<? extends Event>> EVENT_TYPE_MAP = new HashMap<>();

	static {
		EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
		EVENT_TYPE_MAP.put("RoomUpdated", RoomUpdatedEvent.class);
		EVENT_TYPE_MAP.put("SlotReserved", SlotReservedEvent.class);
		EVENT_TYPE_MAP.put("PaymentCompleted", PaymentCompletedEvent.class);
		EVENT_TYPE_MAP.put("RefundCompleted", RefundCompletedEvent.class);
		EVENT_TYPE_MAP.put("ReservationRefund", ReservationRefundEvent.class);
	}

	private final JsonUtil jsonUtil;
	private final List<EventHandler<?>> handlers;

	public KafkaEventConsumer(
			final JsonUtil jsonUtil,
			final List<EventHandler<?>> handlers) {
		this.jsonUtil = jsonUtil;
		this.handlers = handlers;
	}
	
	/**
	 * room-created, room-updated 토픽에서 이벤트를 수신합니다.
	 *
	 * @param message        Kafka 메시지 (JSON)
	 * @param acknowledgment Kafka acknowledgment
	 */
	@KafkaListener(
			topics = {"${kafka.topics.room-created}", "${kafka.topics.room-updated}"},
			groupId = "${spring.kafka.consumer.group-id}")
	public void consume(final String message, final Acknowledgment acknowledgment) {
		logger.info("Received message from room topics: {}", message);
		processEvent(message, acknowledgment);
	}
	
	/**
	 * reservation-reserved 토픽에서 이벤트를 수신합니다.
	 *
	 * @param message        Kafka 메시지 (JSON)
	 * @param acknowledgment Kafka acknowledgment
	 */
	@KafkaListener(
			topics = "${kafka.topics.reservation-reserved}",
			groupId = "${spring.kafka.consumer.group-id}")
	public void consumeReservationEvents(final String message, final Acknowledgment acknowledgment) {
		logger.info("Received message from reservation-reserved topic: {}", message);
		processEvent(message, acknowledgment);
	}

	/**
	 * payment-completed, reservation-refund, refund-completed 토픽에서 결제 이벤트를 수신합니다.
	 *
	 * @param message        Kafka 메시지 (JSON)
	 * @param acknowledgment Kafka acknowledgment
	 */
	@KafkaListener(
			topics = {
					"${kafka.topics.payment-completed}",
					"${kafka.topics.reservation-refund}",
					"${kafka.topics.refund-completed}"
			},
			groupId = "${spring.kafka.consumer.group-id}")
	public void consumePaymentEvents(final String message, final Acknowledgment acknowledgment) {
		logger.info("Received message from payment topics: {}", message);
		processEvent(message, acknowledgment);
	}

	/**
	 * 공통 이벤트 처리 로직.
	 * eventType 또는 topic을 추출하고 적절한 핸들러로 라우팅합니다.
	 *
	 * @param message        Kafka 메시지 (JSON)
	 * @param acknowledgment Kafka acknowledgment
	 */
	private void processEvent(final String message, final Acknowledgment acknowledgment) {
		try {
			// 1. eventType 또는 topic 추출 (Map으로 파싱)
			@SuppressWarnings("unchecked")
			final Map<String, Object> messageMap = jsonUtil.fromJson(message, Map.class);
			String eventType = (String) messageMap.get("eventType");

			// eventType이 없으면 topic에서 변환
			if (eventType == null) {
				final String topic = (String) messageMap.get("topic");
				if (topic == null) {
					logger.warn("Missing both eventType and topic in message: {}", message);
					acknowledgment.acknowledge();
					return;
				}
				// topic을 eventType으로 변환
				eventType = convertTopicToEventType(topic);
				logger.debug("Converted topic '{}' to eventType '{}'", topic, eventType);
			}

			// 2. eventType에 해당하는 클래스 찾기
			final Class<? extends Event> eventClass = EVENT_TYPE_MAP.get(eventType);
			if (eventClass == null) {
				logger.warn("Unknown eventType: {}", eventType);
				acknowledgment.acknowledge();
				return;
			}

			// 3. JSON을 이벤트 객체로 역직렬화 (eventType 필드 추가)
			messageMap.put("eventType", eventType);
			final String enhancedMessage = jsonUtil.toJson(messageMap);
			final Event event = jsonUtil.fromJson(enhancedMessage, eventClass);

			// 4. 적절한 핸들러 찾기
			final EventHandler<Event> handler = findHandler(eventType);
			if (handler == null) {
				logger.warn("No handler found for eventType: {}", eventType);
				acknowledgment.acknowledge();
				return;
			}

			// 5. 핸들러로 처리
			handler.handle(event);

			// 6. 수동 커밋
			acknowledgment.acknowledge();
			logger.info("Successfully processed event: {}", eventType);

		} catch (final Exception e) {
			logger.error("Failed to process message: {}", message, e);
			// TODO: DLQ(Dead Letter Queue)로 전송 또는 재처리 로직 구현
			acknowledgment.acknowledge(); // 실패해도 일단 acknowledge (무한 재시도 방지)
		}
	}

	/**
	 * Kafka topic 이름을 eventType 형식으로 변환합니다.
	 * 예: "room-created" -> "RoomCreated"
	 *     "reservation-reserved" -> "ReservationReserved"
	 *
	 * @param topic Kafka topic 이름
	 * @return eventType 문자열
	 */
	private String convertTopicToEventType(final String topic) {
		// 특별 케이스: reservation-reserved -> SlotReserved (이벤트명이 다름)
		if ("reservation-reserved".equals(topic)) {
			return "SlotReserved";
		}

		// 일반 변환: room-created -> RoomCreated
		return Arrays.stream(topic.split("-"))
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
				.collect(Collectors.joining(""));
	}
	
	@SuppressWarnings("unchecked")
	private EventHandler<Event> findHandler(final String eventType) {
		return (EventHandler<Event>) handlers.stream()
				.filter(handler -> handler.getSupportedEventType().equals(eventType))
				.findFirst()
				.orElse(null);
	}
}
