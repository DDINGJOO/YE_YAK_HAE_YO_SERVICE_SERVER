package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.RoomCreatedEvent;
import com.teambind.springproject.application.port.in.CreatePricingPolicyUseCase;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.RoomId;
import com.teambind.springproject.domain.shared.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomCreatedEventHandler 단위 테스트")
class RoomCreatedEventHandlerTest {
	
	@Mock
	private CreatePricingPolicyUseCase createPricingPolicyUseCase;
	
	@InjectMocks
	private RoomCreatedEventHandler roomCreatedEventHandler;
	
	@Nested
	@DisplayName("handle 테스트")
	class HandleTests {
		
		@Test
		@DisplayName("RoomCreatedEvent를 정상적으로 처리한다")
		void handleRoomCreatedEvent() {
			// given
			final RoomCreatedEvent event = new RoomCreatedEvent(
					"room-events",
					"RoomCreated",
					100L,
					1L,
					"HOUR"
			);
			
			final PricingPolicy expectedPolicy = PricingPolicy.create(
					RoomId.of(1L),
					PlaceId.of(100L),
					TimeSlot.HOUR,
					Money.of(BigDecimal.ZERO)
			);
			
			when(createPricingPolicyUseCase.createDefaultPolicy(
					any(RoomId.class),
					any(PlaceId.class),
					any(TimeSlot.class)
			)).thenReturn(expectedPolicy);
			
			// when
			roomCreatedEventHandler.handle(event);
			
			// then
			verify(createPricingPolicyUseCase).createDefaultPolicy(
					RoomId.of(1L),
					PlaceId.of(100L),
					TimeSlot.HOUR
			);
		}
		
		@Test
		@DisplayName("UseCase 실행 중 예외 발생 시 RuntimeException을 던진다")
		void throwsRuntimeExceptionWhenUseCaseFails() {
			// given
			final RoomCreatedEvent event = new RoomCreatedEvent(
					"room-events",
					"RoomCreated",
					100L,
					1L,
					"HOUR"
			);
			
			when(createPricingPolicyUseCase.createDefaultPolicy(
					any(RoomId.class),
					any(PlaceId.class),
					any(TimeSlot.class)
			)).thenThrow(new RuntimeException("Database error"));
			
			// when & then
			assertThatThrownBy(() -> roomCreatedEventHandler.handle(event))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Failed to handle RoomCreatedEvent");
		}
		
		@Test
		@DisplayName("잘못된 TimeSlot 값이 오면 예외를 던진다")
		void throwsExceptionWhenInvalidTimeSlot() {
			// given
			final RoomCreatedEvent event = new RoomCreatedEvent(
					"room-events",
					"RoomCreated",
					100L,
					1L,
					"INVALID_SLOT"
			);
			
			// when & then
			assertThatThrownBy(() -> roomCreatedEventHandler.handle(event))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Failed to handle RoomCreatedEvent");
		}
	}
	
	@Nested
	@DisplayName("getSupportedEventType 테스트")
	class GetSupportedEventTypeTests {
		
		@Test
		@DisplayName("지원하는 이벤트 타입은 'RoomCreated'이다")
		void supportedEventTypeIsRoomCreated() {
			// when
			final String eventType = roomCreatedEventHandler.getSupportedEventType();
			
			// then
			assertThat(eventType).isEqualTo("RoomCreated");
		}
	}
}
