package com.teambind.springproject.adapter.out.messaging;

import com.teambind.springproject.application.port.out.InventoryCompensationQueue;
import com.teambind.springproject.domain.reservationpricing.InventoryCompensationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 인메모리 재고 보상 트랜잭션 큐 구현체.
 * 프로덕션 환경에서는 Redis Queue 또는 Kafka로 교체 권장.
 *
 * 현재 제약사항:
 *
 *   애플리케이션 재시작 시 큐 데이터 소실
 *   다중 인스턴스 환경에서 태스크 분산 처리 불가
 *
 */
@Component
public class InMemoryInventoryCompensationQueue implements InventoryCompensationQueue {

	private static final Logger logger = LoggerFactory.getLogger(InMemoryInventoryCompensationQueue.class);
	private static final int MAX_QUEUE_SIZE = 1000;

	private final ConcurrentLinkedQueue<InventoryCompensationTask> queue = new ConcurrentLinkedQueue<>();

	@Override
	public void enqueue(final InventoryCompensationTask task) {
		// 큐 크기 제한 (OutOfMemoryError 방지)
		if (queue.size() >= MAX_QUEUE_SIZE) {
			logger.error("CRITICAL: Compensation queue full (size={}). Dropping task: taskId={}",
					MAX_QUEUE_SIZE, task.getTaskId());
			// TODO: 인프라팀 협의 후 DLQ (Dead Letter Queue)로 전환
			// TODO: 긴급 알람 발송 (Slack, PagerDuty 등)
			return;
		}

		queue.offer(task);
		logger.warn("Inventory compensation task added to queue: taskId={}, current size={}",
				task.getTaskId(), queue.size());

		// 큐 크기 경고
		if (queue.size() > 10) {
			logger.error("WARNING: Compensation queue size exceeded 10. Current size: {}", queue.size());
			// TODO: 프로덕션 환경에서는 알람 발송
		}
	}

	@Override
	public InventoryCompensationTask dequeue() {
		return queue.poll();
	}

	@Override
	public int size() {
		return queue.size();
	}
}
