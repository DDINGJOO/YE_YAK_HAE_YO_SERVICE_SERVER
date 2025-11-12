package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.ReservationConfirmedEvent;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.TimeSlotPriceBreakdown;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationConfirmedEventHandler 단위 테스트")
class ReservationConfirmedEventHandlerTest {
	
	@Mock
	private ReservationPricingRepository reservationPricingRepository;
	
	@InjectMocks
	private ReservationConfirmedEventHandler handler;
	
	private ReservationConfirmedEvent event;
	private ReservationPricing reservationPricing;
	
	@BeforeEach
	void setUp() {
		event = new ReservationConfirmedEvent(
				"reservation-confirmed",
				"ReservationConfirmed",
				1L,
				LocalDateTime.now()
		);
		
		final TimeSlotPriceBreakdown timeSlotBreakdown = new TimeSlotPriceBreakdown(
				Map.of(LocalDateTime.of(2025, 1, 15, 10, 0), Money.of(BigDecimal.valueOf(10000))),
				TimeSlot.HOUR
		);
		
		reservationPricing = ReservationPricing.calculate(
				ReservationId.of(1L),
				RoomId.of(1L),
				timeSlotBreakdown,
				Collections.emptyList(),
				10L
		);
	}
	
	@Nested
	@DisplayName("handle() - 예약 확정 이벤트 처리")
	class HandleTests {
		
		@Test
		@DisplayName("PENDING 상태의 예약을 CONFIRMED로 변경한다")
		void confirmPendingReservation() {
			// given
			when(reservationPricingRepository.findById(any(ReservationId.class)))
					.thenReturn(Optional.of(reservationPricing));
			
			// when
			handler.handle(event);
			
			// then
			verify(reservationPricingRepository).findById(ReservationId.of(1L));
			verify(reservationPricingRepository).save(any(ReservationPricing.class));
			assert reservationPricing.getStatus() == ReservationStatus.CONFIRMED;
		}
		
		@Test
		@DisplayName("존재하지 않는 예약 ID인 경우 ReservationPricingNotFoundException을 발생시킨다")
		void throwsExceptionWhenReservationNotFound() {
			// given
			when(reservationPricingRepository.findById(any(ReservationId.class)))
					.thenReturn(Optional.empty());
			
			// when & then
			assertThatThrownBy(() -> handler.handle(event))
					.isInstanceOf(ReservationPricingNotFoundException.class)
					.hasMessageContaining("reservationId=1");
			
			verify(reservationPricingRepository).findById(ReservationId.of(1L));
			verify(reservationPricingRepository, never()).save(any(ReservationPricing.class));
		}
		
		@Test
		@DisplayName("CONFIRMED 상태의 예약을 다시 확정하려고 하면 InvalidReservationStatusException을 발생시킨다")
		void throwsExceptionWhenAlreadyConfirmed() {
			// given
			reservationPricing.confirm(); // 이미 CONFIRMED 상태로 변경
			when(reservationPricingRepository.findById(any(ReservationId.class)))
					.thenReturn(Optional.of(reservationPricing));

			// when & then
			assertThatThrownBy(() -> handler.handle(event))
					.isInstanceOf(InvalidReservationStatusException.class);

			verify(reservationPricingRepository).findById(ReservationId.of(1L));
			verify(reservationPricingRepository, never()).save(any(ReservationPricing.class));
		}

		@Test
		@DisplayName("CANCELLED 상태의 예약을 확정하려고 하면 InvalidReservationStatusException을 발생시킨다")
		void throwsExceptionWhenCancelled() {
			// given
			reservationPricing.cancel(); // CANCELLED 상태로 변경
			when(reservationPricingRepository.findById(any(ReservationId.class)))
					.thenReturn(Optional.of(reservationPricing));

			// when & then
			assertThatThrownBy(() -> handler.handle(event))
					.isInstanceOf(InvalidReservationStatusException.class);

			verify(reservationPricingRepository).findById(ReservationId.of(1L));
			verify(reservationPricingRepository, never()).save(any(ReservationPricing.class));
		}
	}
	
	@Nested
	@DisplayName("getSupportedEventType() - 지원하는 이벤트 타입")
	class GetSupportedEventTypeTests {
		
		@Test
		@DisplayName("ReservationConfirmed 이벤트 타입을 반환한다")
		void returnsSupportedEventType() {
			// when
			final String eventType = handler.getSupportedEventType();
			
			// then
			assert eventType.equals("ReservationConfirmed");
		}
	}
}
