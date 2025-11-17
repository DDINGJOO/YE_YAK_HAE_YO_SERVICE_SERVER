package com.teambind.springproject.adapter.in.scheduling;

import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.reservationpricing.ReservationPricing;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 만료된 PENDING 예약을 자동으로 취소하는 스케줄러.
 * ShedLock을 사용하여 분산 환경에서 중복 실행을 방지합니다.
 */
@Component
public class ExpiredReservationScheduler {

	private static final Logger logger = LoggerFactory.getLogger(
			ExpiredReservationScheduler.class);

	private final ReservationPricingRepository reservationPricingRepository;
	private final CreateReservationUseCase createReservationUseCase;

	public ExpiredReservationScheduler(
			final ReservationPricingRepository reservationPricingRepository,
			final CreateReservationUseCase createReservationUseCase) {
		this.reservationPricingRepository = reservationPricingRepository;
		this.createReservationUseCase = createReservationUseCase;
	}
	
	/**
	 * 만료된 PENDING 예약을 찾아서 취소 처리합니다.
	 * 재고 복구, 상태 변경, 이벤트 발행을 모두 수행합니다.
	 * 매 1분마다 실행되며, 최대 10분간 락을 유지합니다.
	 */
	@Scheduled(cron = "0 * * * * *")
	@SchedulerLock(
			name = "cancelExpiredReservations",
			lockAtMostFor = "10m",
			lockAtLeastFor = "30s"
	)
	public void cancelExpiredReservations() {
		logger.info("Starting expired reservation cancellation job");

		try {
			final List<ReservationPricing> expiredReservations =
					reservationPricingRepository.findExpiredPendingReservations();

			if (expiredReservations.isEmpty()) {
				logger.info("No expired reservations found");
				return;
			}

			logger.info("Found {} expired reservations to cancel", expiredReservations.size());

			int cancelledCount = 0;
			int failedCount = 0;

			for (final ReservationPricing reservation : expiredReservations) {
				try {
					// cancelReservation()을 호출하여 재고 복구 + 이벤트 발행까지 처리
					createReservationUseCase.cancelReservation(
							reservation.getReservationId().getValue());
					cancelledCount++;
					logger.debug("Cancelled expired reservation: reservationId={}",
							reservation.getReservationId().getValue());
				} catch (final Exception e) {
					failedCount++;
					logger.error("Failed to cancel expired reservation: reservationId={}",
							reservation.getReservationId().getValue(), e);
				}
			}

			logger.info("Expired reservation cancellation job completed: "
							+ "cancelled={}, failed={}, total={}",
					cancelledCount, failedCount, expiredReservations.size());

		} catch (final Exception e) {
			logger.error("Failed to execute expired reservation cancellation job", e);
		}
	}
}
