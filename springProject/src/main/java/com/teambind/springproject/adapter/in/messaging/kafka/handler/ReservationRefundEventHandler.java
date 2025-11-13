package com.teambind.springproject.adapter.in.messaging.kafka.handler;

import com.teambind.springproject.adapter.in.messaging.kafka.event.ReservationRefundEvent;
import com.teambind.springproject.application.service.reservationpricing.ReservationPricingService;
import com.teambind.springproject.domain.reservationpricing.exception.InvalidReservationStatusException;
import com.teambind.springproject.domain.reservationpricing.exception.ReservationPricingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReservationRefundEvent 핸들러.
 * 결제 서비스에서 환불 완료 또는 관리자 승인 취소/거절 이벤트를 수신하여
 * CONFIRMED 상태의 예약을 CANCELLED로 변경하고 재고를 해제합니다.
 * <p>
 * 주의: CONFIRMED 상태가 아닌 예약에 대한 환불 이벤트는 예외를 발생시킵니다.
 */
@Component
@Transactional
public class ReservationRefundEventHandler implements EventHandler<ReservationRefundEvent> {

	private static final Logger logger = LoggerFactory.getLogger(ReservationRefundEventHandler.class);

	private final ReservationPricingService reservationPricingService;

	public ReservationRefundEventHandler(final ReservationPricingService reservationPricingService) {
		this.reservationPricingService = reservationPricingService;
	}

	@Override
	public void handle(final ReservationRefundEvent event) {
		logger.info("Handling ReservationRefundEvent: reservationId={}, occurredAt={}",
				event.getReservationId(), event.getOccurredAt());

		try {
			// 환불 처리: 재고 해제 + 상태 변경 (CONFIRMED → CANCELLED)
			reservationPricingService.refundReservation(event.getReservationId());

			logger.info("Successfully refunded reservation: reservationId={}",
					event.getReservationId());

		} catch (final ReservationPricingNotFoundException e) {
			logger.error("Reservation not found: reservationId={}", event.getReservationId(), e);
			throw e;
		} catch (final InvalidReservationStatusException e) {
			logger.error("Invalid state transition for refund: reservationId={}, message={}",
					event.getReservationId(), e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			logger.error("Failed to handle ReservationRefundEvent: {}", event, e);
			throw new RuntimeException("Failed to handle ReservationRefundEvent", e);
		}
	}

	@Override
	public String getSupportedEventType() {
		return "ReservationRefund";
	}
}
