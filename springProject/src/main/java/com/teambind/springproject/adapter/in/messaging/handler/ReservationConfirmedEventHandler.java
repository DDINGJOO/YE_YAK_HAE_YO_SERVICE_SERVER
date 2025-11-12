package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.ReservationConfirmedEvent;
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
 * ReservationConfirmedEvent 핸들러.
 * 결제 서비스에서 결제 완료 이벤트를 수신하여 예약 상태를 CONFIRMED로 변경합니다.
 */
@Component
@Transactional
public class ReservationConfirmedEventHandler implements EventHandler<ReservationConfirmedEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(
			ReservationConfirmedEventHandler.class);
	
	private final ReservationPricingRepository reservationPricingRepository;
	
	public ReservationConfirmedEventHandler(
			final ReservationPricingRepository reservationPricingRepository) {
		this.reservationPricingRepository = reservationPricingRepository;
	}
	
	@Override
	public void handle(final ReservationConfirmedEvent event) {
		logger.info("Handling ReservationConfirmedEvent: reservationId={}, occurredAt={}",
				event.getReservationId(), event.getOccurredAt());
		
		try {
			final ReservationId reservationId = ReservationId.of(event.getReservationId());
			
			// 1. 예약 조회
			final ReservationPricing reservation = reservationPricingRepository.findById(reservationId)
					.orElseThrow(() -> new ReservationPricingNotFoundException(event.getReservationId()));
			
			// 2. 상태 변경 (PENDING → CONFIRMED)
			reservation.confirm();
			
			// 3. 저장
			reservationPricingRepository.save(reservation);
			
			logger.info("Successfully confirmed reservation: reservationId={}, status={}",
					event.getReservationId(), reservation.getStatus());
			
		} catch (final ReservationPricingNotFoundException e) {
			logger.error("Reservation not found: reservationId={}", event.getReservationId(), e);
			throw e;
		} catch (final InvalidReservationStatusException e) {
			logger.error("Invalid state transition: reservationId={}, message={}",
					event.getReservationId(), e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			logger.error("Failed to handle ReservationConfirmedEvent: {}", event, e);
			throw new RuntimeException("Failed to handle ReservationConfirmedEvent", e);
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "ReservationConfirmed";
	}
}
