package com.teambind.springproject.adapter.out.messaging.kafka.event.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationCancelledEvent;
import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationPendingPaymentEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventDtoFactory 테스트")
class EventDtoFactoryTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("ReservationPendingPaymentEvent를 DTO로 변환하고 ID가 String으로 직렬화된다")
	void convertReservationPendingPaymentEventToDto() throws Exception {
		// Given
		final ReservationPendingPaymentEvent event = new ReservationPendingPaymentEvent(
				"test-topic",
				"TestEvent",
				123L,  // Long 타입
				456L,
				789L,
				"2025-11-19",
				Collections.emptyList(),
				null,
				BigDecimal.valueOf(10000),
				"2025-11-19T10:00:00"
		);

		// When
		final Object dto = EventDtoFactory.createDto(event);
		final String json = objectMapper.writeValueAsString(dto);

		// Then
		assertThat(dto).isInstanceOf(ReservationPendingPaymentEventDto.class);
		assertThat(json).contains("\"reservationId\":\"123\"");  // String으로 직렬화
		assertThat(json).contains("\"placeId\":\"456\"");
		assertThat(json).contains("\"roomId\":\"789\"");
	}

	@Test
	@DisplayName("ReservationCancelledEvent를 DTO로 변환하고 ID만 String으로 직렬화된다")
	void convertReservationCancelledEventToDto() throws Exception {
		// Given
		final LocalDateTime testTime = LocalDateTime.of(2025, 11, 19, 10, 0, 0);
		final ReservationCancelledEvent event = new ReservationCancelledEvent(
				"cancel-topic",
				"CancelEvent",
				999L,  // Long 타입
				888L,
				"Test reason",
				testTime
		);

		// When
		final Object dto = EventDtoFactory.createDto(event);
		final ReservationCancelledEventDto typedDto = (ReservationCancelledEventDto) dto;

		// Then
		assertThat(dto).isInstanceOf(ReservationCancelledEventDto.class);
		assertThat(typedDto.getReservationId()).isEqualTo("999");  // ID는 String
		assertThat(typedDto.getRoomId()).isEqualTo("888");
		assertThat(typedDto.getOccurredAt()).isEqualTo(testTime);  // occurredAt은 LocalDateTime 그대로
		assertThat(typedDto.getCancelReason()).isEqualTo("Test reason");
	}

	@Test
	@DisplayName("등록되지 않은 이벤트 타입은 IllegalArgumentException을 던진다")
	void throwsExceptionForUnregisteredEventType() {
		// Given
		final class UnknownEvent extends com.teambind.springproject.adapter.out.messaging.kafka.event.Event {
			UnknownEvent() {
				super("unknown", "unknown");
			}

			@Override
			public String getEventTypeName() {
				return "unknown";
			}
		}

		final UnknownEvent event = new UnknownEvent();

		// When & Then
		assertThatThrownBy(() -> EventDtoFactory.createDto(event))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("No DTO converter registered");
	}

	@Test
	@DisplayName("hasConverter는 등록된 이벤트 타입에 대해 true를 반환한다")
	void hasConverterReturnsTrueForRegisteredEventType() {
		// When & Then
		assertThat(EventDtoFactory.hasConverter(ReservationPendingPaymentEvent.class)).isTrue();
		assertThat(EventDtoFactory.hasConverter(ReservationCancelledEvent.class)).isTrue();
	}
}
