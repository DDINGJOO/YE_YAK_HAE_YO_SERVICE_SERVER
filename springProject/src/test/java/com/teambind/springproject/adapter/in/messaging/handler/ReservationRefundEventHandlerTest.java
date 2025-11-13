package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.kafka.event.ReservationRefundEvent;
import com.teambind.springproject.adapter.in.messaging.kafka.handler.ReservationRefundEventHandler;
import com.teambind.springproject.application.service.reservationpricing.ReservationPricingService;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationRefundEventHandler 단위 테스트")
class ReservationRefundEventHandlerTest {

	@Mock
	private ReservationPricingService reservationPricingService;

	@InjectMocks
	private ReservationRefundEventHandler handler;

	private ReservationRefundEvent event;

	@BeforeEach
	void setUp() {
		event = new ReservationRefundEvent(
				"reservation-refund",
				"ReservationRefund",
				1L,
				LocalDateTime.now()
		);
	}

	@Nested
	@DisplayName("handle() - 예약 환불 이벤트 처리")
	class HandleTests {

		@Test
		@DisplayName("환불 이벤트를 처리하여 refundReservation을 호출한다")
		void refundConfirmedReservation() {
			// when
			handler.handle(event);

			// then
			verify(reservationPricingService).refundReservation(1L);
		}

		@Test
		@DisplayName("PENDING 상태의 예약을 환불하려고 하면 InvalidReservationStatusException을 발생시킨다")
		void throwsExceptionWhenRefundingPending() {
			// given
			doThrow(new InvalidReservationStatusException("Cannot refund reservation in PENDING status"))
					.when(reservationPricingService).refundReservation(1L);

			// when & then
			assertThatThrownBy(() -> handler.handle(event))
					.isInstanceOf(InvalidReservationStatusException.class)
					.hasMessageContaining("refund");

			verify(reservationPricingService).refundReservation(1L);
		}

		@Test
		@DisplayName("이미 CANCELLED 상태의 예약을 환불하려고 하면 InvalidReservationStatusException을 발생시킨다")
		void throwsExceptionWhenAlreadyCancelled() {
			// given
			doThrow(new InvalidReservationStatusException("Cannot refund reservation in CANCELLED status"))
					.when(reservationPricingService).refundReservation(1L);

			// when & then
			assertThatThrownBy(() -> handler.handle(event))
					.isInstanceOf(InvalidReservationStatusException.class)
					.hasMessageContaining("refund");

			verify(reservationPricingService).refundReservation(1L);
		}

		@Test
		@DisplayName("존재하지 않는 예약 ID인 경우 ReservationPricingNotFoundException을 발생시킨다")
		void throwsExceptionWhenReservationNotFound() {
			// given
			doThrow(new ReservationPricingNotFoundException("Reservation pricing not found: reservationId=1"))
					.when(reservationPricingService).refundReservation(1L);

			// when & then
			assertThatThrownBy(() -> handler.handle(event))
					.isInstanceOf(ReservationPricingNotFoundException.class)
					.hasMessageContaining("reservationId=1");

			verify(reservationPricingService).refundReservation(1L);
		}
	}

	@Nested
	@DisplayName("getSupportedEventType() - 지원하는 이벤트 타입")
	class GetSupportedEventTypeTests {

		@Test
		@DisplayName("ReservationRefund 이벤트 타입을 반환한다")
		void returnsSupportedEventType() {
			// when
			final String eventType = handler.getSupportedEventType();

			// then
			assertThat(eventType).isEqualTo("ReservationRefund");
		}
	}
}
