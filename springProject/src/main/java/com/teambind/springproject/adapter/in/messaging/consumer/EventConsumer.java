package com.teambind.springproject.adapter.in.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.adapter.in.messaging.event.Event;
import com.teambind.springproject.adapter.in.messaging.event.ReservationCancelledEvent;
import com.teambind.springproject.adapter.in.messaging.event.ReservationConfirmedEvent;
import com.teambind.springproject.adapter.in.messaging.event.RoomCreatedEvent;
import com.teambind.springproject.adapter.in.messaging.event.RoomUpdatedEvent;
import com.teambind.springproject.adapter.in.messaging.event.SlotReservedEvent;
import com.teambind.springproject.adapter.in.messaging.handler.EventHandler;
import com.teambind.springproject.common.util.json.JsonUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 Consumer.
 * 이벤트를 수신하고 적절한 핸들러로 라우팅합니다.
 */
@Component
public class EventConsumer {

  private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
  private static final Map<String, Class<? extends Event>> EVENT_TYPE_MAP = new HashMap<>();

  static {
    EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
    EVENT_TYPE_MAP.put("RoomUpdated", RoomUpdatedEvent.class);
    EVENT_TYPE_MAP.put("SlotReserved", SlotReservedEvent.class);
    EVENT_TYPE_MAP.put("ReservationConfirmed", ReservationConfirmedEvent.class);
    EVENT_TYPE_MAP.put("ReservationCancelled", ReservationCancelledEvent.class);
  }

  private final JsonUtil jsonUtil;
  private final ObjectMapper objectMapper;
  private final List<EventHandler<?>> handlers;

  public EventConsumer(
      final JsonUtil jsonUtil,
      final ObjectMapper objectMapper,
      final List<EventHandler<?>> handlers) {
    this.jsonUtil = jsonUtil;
    this.objectMapper = objectMapper;
    this.handlers = handlers;
  }

  /**
   * room-created, room-updated 토픽에서 이벤트를 수신합니다.
   *
   * @param message        Kafka 메시지 (JSON)
   * @param acknowledgment Kafka acknowledgment
   */
  @KafkaListener(topics = {"room-created", "room-updated"}, groupId = "${spring.kafka.consumer.group-id}")
  public void consume(final String message, final Acknowledgment acknowledgment) {
    logger.info("Received message from room topics: {}", message);

    try {
      // 1. eventType 추출
      final JsonNode rootNode = objectMapper.readTree(message);
      final String eventType = rootNode.get("eventType").asText();

      // 2. eventType에 해당하는 클래스 찾기
      final Class<? extends Event> eventClass = EVENT_TYPE_MAP.get(eventType);
      if (eventClass == null) {
        logger.warn("Unknown eventType: {}", eventType);
        acknowledgment.acknowledge();
        return;
      }

      // 3. JSON을 이벤트 객체로 역직렬화
      final Event event = jsonUtil.fromJson(message, eventClass);

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
   * reservation-reserved 토픽에서 이벤트를 수신합니다.
   *
   * @param message        Kafka 메시지 (JSON)
   * @param acknowledgment Kafka acknowledgment
   */
  @KafkaListener(topics = "reservation-reserved", groupId = "${spring.kafka.consumer.group-id}")
  public void consumeReservationEvents(final String message, final Acknowledgment acknowledgment) {
    logger.info("Received message from reservation-reserved topic: {}", message);

    try {
      // 1. eventType 추출
      final JsonNode rootNode = objectMapper.readTree(message);
      final String eventType = rootNode.get("eventType").asText();

      // 2. eventType에 해당하는 클래스 찾기
      final Class<? extends Event> eventClass = EVENT_TYPE_MAP.get(eventType);
      if (eventClass == null) {
        logger.warn("Unknown eventType: {}", eventType);
        acknowledgment.acknowledge();
        return;
      }

      // 3. JSON을 이벤트 객체로 역직렬화
      final Event event = jsonUtil.fromJson(message, eventClass);

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
   * reservation-confirmed, reservation-cancelled 토픽에서 결제 이벤트를 수신합니다.
   *
   * @param message        Kafka 메시지 (JSON)
   * @param acknowledgment Kafka acknowledgment
   */
  @KafkaListener(
      topics = {"reservation-confirmed", "reservation-cancelled"},
      groupId = "${spring.kafka.consumer.group-id}")
  public void consumePaymentEvents(final String message, final Acknowledgment acknowledgment) {
    logger.info("Received message from payment topics: {}", message);

    try {
      // 1. eventType 추출
      final JsonNode rootNode = objectMapper.readTree(message);
      final String eventType = rootNode.get("eventType").asText();

      // 2. eventType에 해당하는 클래스 찾기
      final Class<? extends Event> eventClass = EVENT_TYPE_MAP.get(eventType);
      if (eventClass == null) {
        logger.warn("Unknown eventType: {}", eventType);
        acknowledgment.acknowledge();
        return;
      }

      // 3. JSON을 이벤트 객체로 역직렬화
      final Event event = jsonUtil.fromJson(message, eventClass);

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

  @SuppressWarnings("unchecked")
  private EventHandler<Event> findHandler(final String eventType) {
    return (EventHandler<Event>) handlers.stream()
        .filter(handler -> handler.getSupportedEventType().equals(eventType))
        .findFirst()
        .orElse(null);
  }
}
