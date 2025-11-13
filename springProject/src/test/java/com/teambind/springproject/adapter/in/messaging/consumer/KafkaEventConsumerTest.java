package com.teambind.springproject.adapter.in.messaging.consumer;

import com.teambind.springproject.adapter.in.messaging.kafka.consumer.KafkaEventConsumer;
import com.teambind.springproject.adapter.in.messaging.kafka.event.RoomCreatedEvent;
import com.teambind.springproject.adapter.in.messaging.kafka.event.SlotReservedEvent;
import com.teambind.springproject.adapter.in.messaging.kafka.handler.RoomCreatedEventHandler;
import com.teambind.springproject.adapter.in.messaging.kafka.handler.SlotReservedEventHandler;
import com.teambind.springproject.common.util.json.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventConsumer 단위 테스트")
class KafkaEventConsumerTest {
	
	private KafkaEventConsumer kafkaEventConsumer;
	
	@Mock
	private RoomCreatedEventHandler roomCreatedEventHandler;
	
	@Mock
	private SlotReservedEventHandler slotReservedEventHandler;
	
	@Mock
	private JsonUtil jsonUtil;
	
	@Mock
	private Acknowledgment acknowledgment;
	
	@Nested
	@DisplayName("consume 테스트")
	class ConsumeTests {
		
		@Test
		@DisplayName("RoomCreatedEvent 메시지를 정상적으로 처리한다")
		void consumeRoomCreatedEvent() throws Exception {
			// given
			when(roomCreatedEventHandler.getSupportedEventType()).thenReturn("RoomCreated");
			kafkaEventConsumer = new KafkaEventConsumer(jsonUtil, List.of(roomCreatedEventHandler));

			final String message = """
					{
					  "topic": "room-events",
					  "eventType": "RoomCreated",
					  "placeId": 100,
					  "roomId": 1,
					  "timeSlot": "HOUR"
					}
					""";

			final RoomCreatedEvent mockEvent = new RoomCreatedEvent(
					"room-events", "RoomCreated", 100L, 1L, "HOUR");
			// Mock the Map.class call for extracting eventType
			when(jsonUtil.fromJson(eq(message), eq(Map.class))).thenReturn(Map.of("eventType", "RoomCreated"));
			// Mock the event class call for deserializing the event
			when(jsonUtil.fromJson(eq(message), eq(RoomCreatedEvent.class))).thenReturn(mockEvent);
			
			// when
			kafkaEventConsumer.consume(message, acknowledgment);
			
			// then
			verify(roomCreatedEventHandler).handle(any(RoomCreatedEvent.class));
			verify(acknowledgment).acknowledge();
		}
		
		@Test
		@DisplayName("알 수 없는 eventType이면 핸들러를 호출하지 않는다")
		void ignoreUnknownEventType() {
			// given
			lenient().when(roomCreatedEventHandler.getSupportedEventType()).thenReturn("RoomCreated");
			kafkaEventConsumer = new KafkaEventConsumer(jsonUtil, List.of(roomCreatedEventHandler));
			
			final String message = """
					{
					  "topic": "room-events",
					  "eventType": "UnknownEvent",
					  "placeId": 100,
					  "roomId": 1
					}
					""";
			
			// when
			kafkaEventConsumer.consume(message, acknowledgment);
			
			// then
			verify(roomCreatedEventHandler, never()).handle(any(RoomCreatedEvent.class));
			verify(acknowledgment).acknowledge();
		}
		
		@Test
		@DisplayName("잘못된 JSON 형식이면 예외를 처리하고 acknowledge한다")
		void handleInvalidJson() {
			// given
			lenient().when(roomCreatedEventHandler.getSupportedEventType()).thenReturn("RoomCreated");
			kafkaEventConsumer = new KafkaEventConsumer(jsonUtil, List.of(roomCreatedEventHandler));
			
			final String invalidMessage = "{ invalid json }";
			
			// when
			kafkaEventConsumer.consume(invalidMessage, acknowledgment);
			
			// then
			verify(roomCreatedEventHandler, never()).handle(any(RoomCreatedEvent.class));
			verify(acknowledgment).acknowledge();
		}
		
		@Test
		@DisplayName("핸들러 처리 중 예외가 발생해도 acknowledge한다")
		void acknowledgeEvenWhenHandlerFails() throws Exception {
			// given
			when(roomCreatedEventHandler.getSupportedEventType()).thenReturn("RoomCreated");
			kafkaEventConsumer = new KafkaEventConsumer(jsonUtil, List.of(roomCreatedEventHandler));

			final String message = """
					{
					  "topic": "room-events",
					  "eventType": "RoomCreated",
					  "placeId": 100,
					  "roomId": 1,
					  "timeSlot": "HOUR"
					}
					""";

			final RoomCreatedEvent mockEvent = new RoomCreatedEvent(
					"room-events", "RoomCreated", 100L, 1L, "HOUR");
			// Mock the Map.class call for extracting eventType
			when(jsonUtil.fromJson(eq(message), eq(Map.class))).thenReturn(Map.of("eventType", "RoomCreated"));
			// Mock the event class call for deserializing the event
			when(jsonUtil.fromJson(eq(message), eq(RoomCreatedEvent.class))).thenReturn(mockEvent);
			
			// Mock handler to throw exception
			org.mockito.Mockito.doThrow(new RuntimeException("Handler error"))
					.when(roomCreatedEventHandler).handle(any(RoomCreatedEvent.class));
			
			// when
			kafkaEventConsumer.consume(message, acknowledgment);
			
			// then
			verify(acknowledgment).acknowledge();
		}
	}
	
	@Nested
	@DisplayName("consumeReservationEvents 테스트")
	class ConsumeReservationEventsTests {
		
		@Test
		@DisplayName("SlotReservedEvent 메시지를 정상적으로 처리한다")
		void consumeSlotReservedEvent() throws Exception {
			// given
			when(slotReservedEventHandler.getSupportedEventType()).thenReturn("SlotReserved");
			kafkaEventConsumer = new KafkaEventConsumer(
					jsonUtil, List.of(slotReservedEventHandler));

			final String message = """
					{
					  "topic": "reservation-reserved",
					  "eventType": "SlotReserved",
					  "roomId": 1,
					  "slotDate": "2025-11-15",
					  "startTimes": ["10:00:00", "10:30:00"],
					  "reservationId": 1000,
					  "occurredAt": "2025-11-09T10:00:00"
					}
					""";

			final SlotReservedEvent mockEvent = new SlotReservedEvent(
					"reservation-reserved",
					"SlotReserved",
					1L,
					LocalDate.of(2025, 11, 15),
					List.of(LocalTime.of(10, 0), LocalTime.of(10, 30)),
					1000L,
					LocalDateTime.of(2025, 11, 9, 10, 0)
			);
			// Mock the Map.class call for extracting eventType
			when(jsonUtil.fromJson(eq(message), eq(Map.class))).thenReturn(Map.of("eventType", "SlotReserved"));
			// Mock the event class call for deserializing the event
			when(jsonUtil.fromJson(eq(message), eq(SlotReservedEvent.class))).thenReturn(mockEvent);
			
			// when
			kafkaEventConsumer.consumeReservationEvents(message, acknowledgment);
			
			// then
			verify(slotReservedEventHandler).handle(any(SlotReservedEvent.class));
			verify(acknowledgment).acknowledge();
		}
		
		@Test
		@DisplayName("알 수 없는 eventType이면 핸들러를 호출하지 않는다")
		void ignoreUnknownEventType() {
			// given
			lenient().when(slotReservedEventHandler.getSupportedEventType()).thenReturn("SlotReserved");
			kafkaEventConsumer = new KafkaEventConsumer(
					jsonUtil, List.of(slotReservedEventHandler));
			
			final String message = """
					{
					  "topic": "reservation-reserved",
					  "eventType": "UnknownReservationEvent",
					  "reservationId": 1000
					}
					""";
			
			// when
			kafkaEventConsumer.consumeReservationEvents(message, acknowledgment);
			
			// then
			verify(slotReservedEventHandler, never()).handle(any(SlotReservedEvent.class));
			verify(acknowledgment).acknowledge();
		}
		
		@Test
		@DisplayName("핸들러 처리 중 예외가 발생해도 acknowledge한다")
		void acknowledgeEvenWhenHandlerFails() throws Exception {
			// given
			when(slotReservedEventHandler.getSupportedEventType()).thenReturn("SlotReserved");
			kafkaEventConsumer = new KafkaEventConsumer(
					jsonUtil, List.of(slotReservedEventHandler));

			final String message = """
					{
					  "topic": "reservation-reserved",
					  "eventType": "SlotReserved",
					  "roomId": 1,
					  "slotDate": "2025-11-15",
					  "startTimes": ["10:00:00"],
					  "reservationId": 1000,
					  "occurredAt": "2025-11-09T10:00:00"
					}
					""";

			final SlotReservedEvent mockEvent = new SlotReservedEvent(
					"reservation-reserved",
					"SlotReserved",
					1L,
					LocalDate.of(2025, 11, 15),
					List.of(LocalTime.of(10, 0)),
					1000L,
					LocalDateTime.of(2025, 11, 9, 10, 0)
			);
			// Mock the Map.class call for extracting eventType
			when(jsonUtil.fromJson(eq(message), eq(Map.class))).thenReturn(Map.of("eventType", "SlotReserved"));
			// Mock the event class call for deserializing the event
			when(jsonUtil.fromJson(eq(message), eq(SlotReservedEvent.class))).thenReturn(mockEvent);
			
			// Mock handler to throw exception
			org.mockito.Mockito.doThrow(new RuntimeException("Handler error"))
					.when(slotReservedEventHandler).handle(any(SlotReservedEvent.class));
			
			// when
			kafkaEventConsumer.consumeReservationEvents(message, acknowledgment);
			
			// then
			verify(acknowledgment).acknowledge();
		}
	}
}
