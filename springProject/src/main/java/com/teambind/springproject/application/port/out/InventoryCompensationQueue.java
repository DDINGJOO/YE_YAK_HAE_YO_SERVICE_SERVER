package com.teambind.springproject.application.port.out;

import com.teambind.springproject.domain.reservationpricing.InventoryCompensationTask;

/**
 * 재고 보상 트랜잭션 큐 인터페이스.
 * 롤백 실패 시 재시도를 위해 태스크를 큐에 추가합니다.
 */
public interface InventoryCompensationQueue {

	/**
	 * 보상 트랜잭션 태스크를 큐에 추가합니다.
	 *
	 * @param task 보상 태스크
	 */
	void enqueue(InventoryCompensationTask task);

	/**
	 * 큐에서 처리할 태스크를 가져옵니다.
	 *
	 * @return 처리할 태스크, 없으면 null
	 */
	InventoryCompensationTask dequeue();

	/**
	 * 현재 큐에 있는 태스크 개수를 반환합니다.
	 *
	 * @return 큐 크기
	 */
	int size();
}