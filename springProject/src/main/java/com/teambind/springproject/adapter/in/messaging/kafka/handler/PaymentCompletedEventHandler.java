package com.teambind.springproject.adapter.in.messaging.kafka.handler;

import com.teambind.springproject.adapter.in.messaging.kafka.event.PaymentCompletedEvent;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.ReservationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentCompletedEvent 핸들러.
 * 결제 서비스에서 결제 완료 이벤트를 수신하여 예약 상태를 CONFIRMED로 변경합니다.
 */
@Component
@Transactional
public class PaymentCompletedEventHandler implements EventHandler<PaymentCompletedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(PaymentCompletedEventHandler.class);

	private final ReservationPricingRepository reservationPricingRepository;

	public PaymentCompletedEventHandler(
			final ReservationPricingRepository reservationPricingRepository) {
		this.reservationPricingRepository = reservationPricingRepository;
	}

	@Override
	public void handle(final PaymentCompletedEvent event) {
		logger.info("Handling PaymentCompletedEvent: paymentId={}, reservationId={}, amount={}, paidAt={}",
				event.getPaymentId(), event.getReservationId(), event.getAmount(), event.getPaidAt());

		try {
			final ReservationId reservationId = ReservationId.of(event.getReservationId());

			// 1. 예약 조회
			final ReservationPricing reservation = reservationPricingRepository.findById(reservationId)
					.orElseThrow(() -> new ReservationPricingNotFoundException(event.getReservationId()));

			// 2. 상태 변경 (PENDING → CONFIRMED)
			reservation.confirm();

			// 3. 저장
			reservationPricingRepository.save(reservation);

			logger.info("Successfully confirmed reservation: paymentId={}, reservationId={}, status={}",
					event.getPaymentId(), event.getReservationId(), reservation.getStatus());

		} catch (final ReservationPricingNotFoundException e) {
			logger.error("Reservation not found: reservationId={}", event.getReservationId(), e);
			throw e;
		} catch (final InvalidReservationStatusException e) {
			logger.error("Invalid state transition: reservationId={}, message={}",
					event.getReservationId(), e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			logger.error("Failed to handle PaymentCompletedEvent: {}", event, e);
			throw new RuntimeException("Failed to handle PaymentCompletedEvent", e);
		}
	}

	@Override
	public String getSupportedEventType() {
		return "PaymentCompleted";
	}
}