package com.teambind.springproject.adapter.in.scheduling;

import com.teambind.springproject.application.port.out.InventoryCompensationQueue;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.reservationpricing.InventoryCompensationTask;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 재고 보상 트랜잭션 스케줄러.
 * 롤백 실패한 재고를 주기적으로 재시도하여 복구합니다.
 *
 * <p>동작 방식:
 * <ul>
 *   <li>1분마다 큐에서 실패한 롤백 태스크를 꺼내서 재시도</li>
 *   <li>최대 5회 재시도 후에도 실패하면 CRITICAL 로그 출력</li>
 *   <li>ShedLock으로 다중 인스턴스 환경에서 중복 실행 방지</li>
 * </ul>
 */
@Component
public class InventoryCompensationScheduler {

	private static final Logger logger = LoggerFactory.getLogger(InventoryCompensationScheduler.class);
	private static final int MAX_RETRY_COUNT = 5;

	private final InventoryCompensationQueue compensationQueue;
	private final ProductRepository productRepository;
	private final Queue<InventoryCompensationTask> retryQueue = new ConcurrentLinkedQueue<>();

	public InventoryCompensationScheduler(
			final InventoryCompensationQueue compensationQueue,
			final ProductRepository productRepository) {
		this.compensationQueue = compensationQueue;
		this.productRepository = productRepository;
	}

	/**
	 * 보상 트랜잭션 처리.
	 * 1분마다 실행되어 큐에 있는 실패한 롤백을 재시도합니다.
	 */
	@Scheduled(fixedDelay = 60000)  // 1분마다
	@SchedulerLock(name = "inventoryCompensation", lockAtMostFor = "5m", lockAtLeastFor = "30s")
	public void processCompensationTasks() {
		final int initialQueueSize = compensationQueue.size();
		if (initialQueueSize == 0 && retryQueue.isEmpty()) {
			logger.debug("No compensation tasks to process");
			return;
		}

		logger.warn("Processing compensation tasks: main queue={}, retry queue={}",
				initialQueueSize, retryQueue.size());

		int processedCount = 0;
		int successCount = 0;
		int failedCount = 0;

		// 메인 큐 처리 - 큐가 빌 때까지 계속 처리
		while (true) {
			final InventoryCompensationTask task = compensationQueue.dequeue();
			if (task == null) {
				break;  // 큐가 비면 종료
			}

			processedCount++;

			try {
				// 재고 해제 시도
				releaseInventory(task);

				successCount++;
				logger.debug("Compensation task succeeded: taskId={}, retryCount={}",
						task.getTaskId(), task.getRetryCount());

			} catch (final Exception e) {
				task.incrementRetryCount();
				failedCount++;

				if (task.getRetryCount() >= MAX_RETRY_COUNT) {
					// 최대 재시도 횟수 초과 - CRITICAL 로그 출력
					logger.error("CRITICAL: Compensation task failed after {} retries. Manual intervention required. TaskId: {}",
							MAX_RETRY_COUNT, task.getTaskId(), e);
					// TODO: 알람 발송 (Slack, PagerDuty, Email 등)
					// TODO: DLQ (Dead Letter Queue) 저장
				} else {
					// 재시도 가능 - 별도 큐에 보관 (다음 스케줄에서 처리)
					logger.warn("Compensation task failed (retry {}/{}): taskId={}, error={}",
							task.getRetryCount(), MAX_RETRY_COUNT, task.getTaskId(), e.getMessage());
					retryQueue.offer(task);
				}
			}
		}

		// 재시도 큐를 메인 큐로 이동 (다음 스케줄에서 처리)
		while (!retryQueue.isEmpty()) {
			compensationQueue.enqueue(retryQueue.poll());
		}

		logger.warn("Compensation processing completed: processed={}, succeeded={}, failed={}, remaining={}",
				processedCount, successCount, failedCount, compensationQueue.size());
	}

	/**
	 * Scope에 따라 적절한 재고 해제 메서드 호출.
	 */
	private void releaseInventory(final InventoryCompensationTask task) {
		final ProductScope scope = task.getProduct().getScope();

		switch (scope) {
			case RESERVATION -> productRepository.releaseQuantity(
					task.getProduct().getProductId(),
					task.getQuantity()
			);
			case ROOM, PLACE -> {
				// 각 시간대별로 재고 해제
				for (final java.time.LocalDateTime timeSlot : task.getTimeSlots()) {
					productRepository.releaseTimeSlotQuantity(
							task.getProduct().getProductId(),
							task.getRoomId(),
							timeSlot,
							task.getQuantity()
					);
				}
			}
		}
	}
}