package com.teambind.springproject.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * pg_partman 자동 파티션 유지보수 스케줄러.
 * 매일 자정에 실행되어 새로운 파티션을 생성하고 오래된 파티션을 정리합니다.
 *
 * <p>pg_partman은 다음 작업을 수행합니다:
 * <ul>
 *   <li>미래 파티션 생성 (premake 설정값만큼, 기본 3개월)</li>
 *   <li>오래된 파티션 삭제 (retention 설정값 기준, 기본 12개월)</li>
 * </ul>
 */
@Component
public class PartitionMaintenanceScheduler {

	private static final Logger logger = LoggerFactory.getLogger(PartitionMaintenanceScheduler.class);

	private final JdbcTemplate jdbcTemplate;

	public PartitionMaintenanceScheduler(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * pg_partman 유지보수 실행.
	 * 매일 오전 3시에 실행됩니다.
	 *
	 * <p>이 메서드는 다음을 수행합니다:
	 * <ol>
	 *   <li>Advisory Lock으로 동시 실행 방지 (다중 인스턴스 환경 대응)</li>
	 *   <li>product_time_slot_inventory 테이블의 파티션 유지보수 실행</li>
	 *   <li>필요한 경우 새로운 파티션 생성 (3개월 미리 생성)</li>
	 *   <li>12개월 이상 된 파티션 삭제</li>
	 * </ol>
	 */
	@Scheduled(cron = "0 0 3 * * *")  // 매일 오전 3시
	public void runPartitionMaintenance() {
		logger.info("Starting pg_partman maintenance for product_time_slot_inventory");

		// Advisory Lock ID: 임의의 고유한 숫자 (해시 기반)
		final int lockId = "partition_maintenance".hashCode();

		try {
			// 1. Try to acquire advisory lock (non-blocking)
			final Boolean lockAcquired = jdbcTemplate.queryForObject(
					"SELECT pg_try_advisory_lock(?)",
					Boolean.class,
					lockId
			);

			if (Boolean.FALSE.equals(lockAcquired)) {
				logger.info("Another instance is already running partition maintenance. Skipping.");
				return;
			}

			logger.info("Advisory lock acquired. Proceeding with partition maintenance.");

			try {
				// 2. pg_partman 유지보수 프로시저 실행
				jdbcTemplate.execute("SELECT partman.run_maintenance('public.product_time_slot_inventory')");

				logger.info("Successfully completed pg_partman maintenance");

			} finally {
				// 3. Release advisory lock
				jdbcTemplate.execute("SELECT pg_advisory_unlock(" + lockId + ")");
				logger.debug("Advisory lock released");
			}

		} catch (final Exception e) {
			logger.error("Failed to run pg_partman maintenance. " +
					"Manual intervention may be required to create partitions.", e);
			// 예외를 다시 던지지 않음 - 유지보수 실패가 서비스를 중단시키지 않도록
		}
	}

	/**
	 * 애플리케이션 시작 시 즉시 파티션 상태를 확인합니다.
	 * 누락된 파티션이 있다면 생성합니다.
	 */
	@Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)  // 시작 10초 후 1회 실행
	public void checkPartitionsOnStartup() {
		logger.info("Checking partition status on application startup");

		try {
			jdbcTemplate.execute("SELECT partman.run_maintenance('public.product_time_slot_inventory')");
			logger.info("Partition check completed successfully");

		} catch (final Exception e) {
			logger.warn("Failed to check partitions on startup. " +
					"This is not critical if pg_partman is not yet configured.", e);
		}
	}
}