package com.teambind.springproject.adapter.in.scheduling;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * pg_partman 자동 파티션 유지보수 스케줄러.
 * 매일 오전 3시에 실행되어 새로운 파티션을 생성하고 오래된 파티션을 정리합니다.
 *
 * <p>pg_partman은 다음 작업을 수행합니다:
 * <ul>
 *   <li>미래 파티션 생성 (premake 설정값만큼, 기본 3개월)</li>
 *   <li>오래된 파티션 삭제 (retention 설정값 기준, 기본 12개월)</li>
 * </ul>
 *
 * <p>동시성 제어:
 * <ul>
 *   <li>ShedLock을 사용하여 다중 인스턴스 환경에서 중복 실행 방지</li>
 *   <li>데이터베이스 기반 분산 락으로 클러스터 환경에서 안전하게 작동</li>
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
	 *   <li>ShedLock으로 동시 실행 방지 (다중 인스턴스 환경 대응)</li>
	 *   <li>product_time_slot_inventory 테이블의 파티션 유지보수 실행</li>
	 *   <li>필요한 경우 새로운 파티션 생성 (3개월 미리 생성)</li>
	 *   <li>12개월 이상 된 파티션 삭제</li>
	 * </ol>
	 *
	 * <p>트랜잭션:
	 * <ul>
	 *   <li>전체 작업이 하나의 트랜잭션으로 실행됨</li>
	 *   <li>실패 시 자동 롤백으로 일관성 보장</li>
	 * </ul>
	 *
	 * <p>ShedLock 설정:
	 * <ul>
	 *   <li>lockAtMostFor: 최대 10분간 락 유지 (작업이 10분 이상 걸리면 강제 해제)</li>
	 *   <li>lockAtLeastFor: 최소 1분간 락 유지 (빠르게 완료되어도 1분간 재실행 방지)</li>
	 * </ul>
	 */
	@Scheduled(cron = "0 0 3 * * *")  // 매일 오전 3시
	@SchedulerLock(name = "partitionMaintenance", lockAtMostFor = "10m", lockAtLeastFor = "1m")
	@Transactional
	public void runPartitionMaintenance() {
		logger.info("Starting pg_partman maintenance for product_time_slot_inventory");

		try {
			// Run pg_partman maintenance within transaction
			jdbcTemplate.execute("SELECT partman.run_maintenance('public.product_time_slot_inventory')");

			logger.info("Successfully completed pg_partman maintenance");

		} catch (final DataAccessException e) {
			logger.error("Database error during partition maintenance. Transaction will be rolled back.", e);
			throw e;  // Re-throw to trigger transaction rollback
		} catch (final Exception e) {
			logger.error("Unexpected error during partition maintenance. " +
					"Manual intervention may be required to create partitions.", e);
			throw new RuntimeException("Partition maintenance failed", e);
		}
	}

	/**
	 * 애플리케이션 시작 시 파티션 상태를 확인합니다.
	 * 누락된 파티션이 있다면 생성합니다.
	 *
	 * <p>동시성:
	 * <ul>
	 *   <li>ShedLock 사용으로 다중 인스턴스 환경에서 중복 실행 방지</li>
	 *   <li>시작 시 10초 후 1회만 실행</li>
	 * </ul>
	 *
	 * <p>ShedLock 설정:
	 * <ul>
	 *   <li>lockAtMostFor: 최대 5분간 락 유지</li>
	 *   <li>lockAtLeastFor: 최소 30초간 락 유지</li>
	 * </ul>
	 */
	@Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)  // 시작 10초 후 1회 실행
	@SchedulerLock(name = "partitionStartupCheck", lockAtMostFor = "5m", lockAtLeastFor = "30s")
	@Transactional
	public void checkPartitionsOnStartup() {
		logger.info("Checking partition status on application startup");

		try {
			jdbcTemplate.execute("SELECT partman.run_maintenance('public.product_time_slot_inventory')");

			logger.info("Partition check completed successfully");

		} catch (final DataAccessException e) {
			logger.warn("Failed to check partitions on startup. " +
					"This is not critical if pg_partman is not yet configured.", e);
			// Don't re-throw - startup should continue even if partition check fails
		} catch (final Exception e) {
			logger.warn("Unexpected error during partition check on startup.", e);
		}
	}
}