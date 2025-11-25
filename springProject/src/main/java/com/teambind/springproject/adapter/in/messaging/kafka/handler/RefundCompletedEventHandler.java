package com.teambind.springproject.adapter.in.messaging.kafka.handler;

import com.teambind.springproject.adapter.in.messaging.kafka.event.RefundCompletedEvent;
import com.teambind.springproject.application.service.reservationpricing.ReservationPricingService;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RefundCompleted 이벤트 핸들러.
 * 결제 서비스에서 환불이 완료되었을 때 발행되는 이벤트를 수신하여
 * CONFIRMED 상태의 예약을 CANCELLED로 변경하고 재고를 해제합니다.
 */
@Component
@Transactional
public class RefundCompletedEventHandler implements EventHandler<RefundCompletedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(RefundCompletedEventHandler.class);

	private final ReservationPricingService reservationPricingService;

	public RefundCompletedEventHandler(final ReservationPricingService reservationPricingService) {
		this.reservationPricingService = reservationPricingService;
	}

	@Override
	public void handle(final RefundCompletedEvent event) {
		logger.info("Handling RefundCompletedEvent: refundId={}, reservationId={}, refundAmount={}, completedAt={}",
				event.getRefundId(), event.getReservationId(), event.getRefundAmount(), event.getCompletedAt());

		try {
			// 환불 처리: 재고 해제 + 상태 변경 (CONFIRMED → CANCELLED)
			reservationPricingService.refundReservation(event.getReservationId());

			logger.info("Successfully processed refund completed: refundId={}, reservationId={}",
					event.getRefundId(), event.getReservationId());

		} catch (final ReservationPricingNotFoundException e) {
			logger.error("Reservation not found: reservationId={}", event.getReservationId(), e);
			throw e;
		} catch (final InvalidReservationStatusException e) {
			logger.error("Invalid state transition for refund: reservationId={}, message={}",
					event.getReservationId(), e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			logger.error("Failed to handle RefundCompletedEvent: {}", event, e);
			throw new RuntimeException("Failed to handle RefundCompletedEvent", e);
		}
	}

	@Override
	public String getSupportedEventType() {
		return "RefundCompleted";
	}
}