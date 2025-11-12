package com.teambind.springproject.domain.reservationpricing;

import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.shared.RoomId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 재고 보상 트랜잭션 태스크.
 * 롤백 실패 시 나중에 재시도하기 위한 정보를 담습니다.
 */
public class InventoryCompensationTask {

	private final String taskId;
	private final Product product;
	private final int quantity;
	private final RoomId roomId;
	private final List<LocalDateTime> timeSlots;
	private final LocalDateTime createdAt;
	private final String originalError;
	private int retryCount;

	public InventoryCompensationTask(
			final Product product,
			final int quantity,
			final RoomId roomId,
			final List<LocalDateTime> timeSlots,
			final String originalError) {
		this.taskId = UUID.randomUUID().toString();
		this.product = product;
		this.quantity = quantity;
		this.roomId = roomId;
		this.timeSlots = timeSlots;
		this.createdAt = LocalDateTime.now();
		this.originalError = originalError;
		this.retryCount = 0;
	}

	public String getTaskId() {
		return taskId;
	}

	public Product getProduct() {
		return product;
	}

	public int getQuantity() {
		return quantity;
	}

	public RoomId getRoomId() {
		return roomId;
	}

	public List<LocalDateTime> getTimeSlots() {
		return timeSlots;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getOriginalError() {
		return originalError;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void incrementRetryCount() {
		this.retryCount++;
	}

	@Override
	public String toString() {
		return String.format(
				"InventoryCompensationTask{taskId=%s, productId=%d, scope=%s, quantity=%d, retryCount=%d, createdAt=%s, error=%s}",
				taskId,
				product.getProductId().getValue(),
				product.getScope(),
				quantity,
				retryCount,
				createdAt,
				originalError
		);
	}
}