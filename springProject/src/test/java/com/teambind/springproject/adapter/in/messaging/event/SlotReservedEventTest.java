package com.teambind.springproject.adapter.in.messaging.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlotReservedEvent 테스트")
class SlotReservedEventTest {
	
	private ObjectMapper objectMapper;
	
	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
	}
	
	@Test
	@DisplayName("Event 객체 생성 성공")
	void createEventSuccess() {
		// Given
		Long roomId = 1L;
		LocalDate slotDate = LocalDate.of(2025, 11, 15);
		List<LocalTime> startTimes = List.of(
				LocalTime.of(10, 0),
				LocalTime.of(11, 0)
		);
		Long reservationId = 1000L;
		LocalDateTime occurredAt = LocalDateTime.now();
		
		// When
		SlotReservedEvent event = new SlotReservedEvent(
				"reservation-reserved",
				"SlotReserved",
				roomId,
				slotDate,
				startTimes,
				reservationId,
				occurredAt
		);
		
		// Then
		assertThat(event.getRoomId()).isEqualTo(roomId);
		assertThat(event.getSlotDate()).isEqualTo(slotDate);
		assertThat(event.getStartTimes()).hasSize(2);
		assertThat(event.getReservationId()).isEqualTo(reservationId);
		assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
		assertThat(event.getEventTypeName()).isEqualTo("SlotReserved");
		assertThat(event.getTopic()).isEqualTo("reservation-reserved");
	}
	
	@Test
	@DisplayName("Jackson 직렬화 성공")
	void serializeEventSuccess() throws Exception {
		// Given
		SlotReservedEvent event = new SlotReservedEvent(
				"reservation-reserved",
				"SlotReserved",
				1L,
				LocalDate.of(2025, 11, 15),
				List.of(LocalTime.of(10, 0)),
				1000L,
				LocalDateTime.of(2025, 11, 9, 10, 0)
		);
		
		// When
		String json = objectMapper.writeValueAsString(event);
		
		// Then
		assertThat(json).isNotNull();
		assertThat(json).contains("\"roomId\":1");
		assertThat(json).contains("\"reservationId\":1000");
		assertThat(json).contains("\"slotDate\":");
	}
	
	@Test
	@DisplayName("Jackson 역직렬화 성공")
	void deserializeEventSuccess() throws Exception {
		// Given
		String json = """
				{
				  "topic": "reservation-reserved",
				  "eventType": "SlotReserved",
				  "roomId": 1,
				  "slotDate": "2025-11-15",
				  "startTimes": ["10:00:00", "11:00:00"],
				  "reservationId": 1000,
				  "occurredAt": "2025-11-09T10:00:00"
				}
				""";
		
		// When
		SlotReservedEvent event = objectMapper.readValue(json, SlotReservedEvent.class);
		
		// Then
		assertThat(event.getRoomId()).isEqualTo(1L);
		assertThat(event.getSlotDate()).isEqualTo(LocalDate.of(2025, 11, 15));
		assertThat(event.getStartTimes()).hasSize(2);
		assertThat(event.getStartTimes().get(0)).isEqualTo(LocalTime.of(10, 0));
		assertThat(event.getReservationId()).isEqualTo(1000L);
		assertThat(event.getOccurredAt()).isEqualTo(LocalDateTime.of(2025, 11, 9, 10, 0));
	}
	
	@Test
	@DisplayName("빈 리스트로 생성 시 빈 불변 리스트 반환")
	void createEventWithEmptyLists() {
		// Given & When
		SlotReservedEvent event = new SlotReservedEvent(
				"reservation-reserved",
				"SlotReserved",
				1L,
				LocalDate.of(2025, 11, 15),
				null,
				1000L,
				LocalDateTime.now()
		);
		
		// Then
		assertThat(event.getStartTimes()).isEmpty();
	}
	
	@Test
	@DisplayName("toString 메서드 동작 확인")
	void toStringTest() {
		// Given
		SlotReservedEvent event = new SlotReservedEvent(
				"reservation-reserved",
				"SlotReserved",
				1L,
				LocalDate.of(2025, 11, 15),
				List.of(LocalTime.of(10, 0)),
				1000L,
				LocalDateTime.of(2025, 11, 9, 10, 0)
		);
		
		// When
		String result = event.toString();
		
		// Then
		assertThat(result).contains("SlotReservedEvent");
		assertThat(result).contains("roomId=1");
		assertThat(result).contains("reservationId=1000");
	}
}
