package com.teambind.springproject.adapter.in.messaging.handler;

import com.teambind.springproject.adapter.in.messaging.event.ReservationCancelledEvent;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import com.teambind.springproject.domain.shared.ReservationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ReservationCancelledEvent 핸들러.
 * 결제 서비스에서 결제 실패 또는 취소 이벤트를 수신하여 예약 상태를 CANCELLED로 변경합니다.
 * 재고는 자동으로 복구됩니다 (CANCELLED 상태는 재고 계산에서 제외됨).
 */
@Component
public class ReservationCancelledEventHandler implements
		EventHandler<ReservationCancelledEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(
			ReservationCancelledEventHandler.class);
	
	private final ReservationPricingRepository reservationPricingRepository;
	
	public ReservationCancelledEventHandler(
			final ReservationPricingRepository reservationPricingRepository) {
		this.reservationPricingRepository = reservationPricingRepository;
	}
	
	@Override
	public void handle(final ReservationCancelledEvent event) {
		logger.info("Handling ReservationCancelledEvent: reservationId={}, occurredAt={}",
				event.getReservationId(), event.getOccurredAt());
		
		try {
			final ReservationId reservationId = ReservationId.of(event.getReservationId());
			
			// 1. 예약 조회
			final ReservationPricing reservation = reservationPricingRepository.findById(reservationId)
					.orElseThrow(() -> new ReservationPricingNotFoundException(event.getReservationId()));
			
			// 2. 상태 변경 (PENDING/CONFIRMED → CANCELLED)
			reservation.cancel();
			
			// 3. 저장
			reservationPricingRepository.save(reservation);
			
			logger.info("Successfully cancelled reservation: reservationId={}, status={}",
					event.getReservationId(), reservation.getStatus());
			
		} catch (final ReservationPricingNotFoundException e) {
			logger.error("Reservation not found: reservationId={}", event.getReservationId(), e);
			throw e;
		} catch (final IllegalStateException e) {
			logger.error("Invalid state transition: reservationId={}, message={}",
					event.getReservationId(), e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			logger.error("Failed to handle ReservationCancelledEvent: {}", event, e);
			throw new RuntimeException("Failed to handle ReservationCancelledEvent", e);
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "ReservationCancelled";
	}
}
