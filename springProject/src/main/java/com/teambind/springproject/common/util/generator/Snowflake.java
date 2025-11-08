package com.teambind.springproject.common.util.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Snowflake ID Generator - Time-ordered 64-bit unique ID - Custom epoch - Supports multiple nodes
 *
 * <p>Author: MyungJoo Date: 2025-06-17
 */
@Component
@Primary
@RequiredArgsConstructor
public class Snowflake implements PrimaryKeyGenerator {
	
	// ===== Bit Allocation =====
	private static final int NODE_ID_BITS = 10;
	private static final int SEQUENCE_BITS = 12;
	
	private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;
	private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
	
	private static final int NODE_ID_SHIFT = SEQUENCE_BITS;
	private static final int TIMESTAMP_SHIFT = NODE_ID_BITS + SEQUENCE_BITS;
	
	// ===== Custom Epoch: 2024-01-01T00:00:00Z =====
	private static final long CUSTOM_EPOCH = 1704067200000L;
	private static final long maxNodeId = (1L << NODE_ID_BITS) - 1;
	// ===== Instance Variables =====
	private final long nodeId = ThreadLocalRandom.current().nextLong(maxNodeId + 1);
	private long lastTimestamp = -1L;
	private long sequence = 0L;
	
	/**
	 * Generate next unique ID
	 */
	public synchronized long nextId() {
		long currentTimestamp = currentTime();
		
		// Clock rollback handling
		if (currentTimestamp < lastTimestamp) {
			// Option 1: wait for time to catch up (soft fail)
			currentTimestamp = waitNextMillis(lastTimestamp);
		}
		
		if (currentTimestamp == lastTimestamp) {
			sequence = (sequence + 1) & MAX_SEQUENCE;
			if (sequence == 0) {
				// Sequence overflow: wait for next millisecond
				currentTimestamp = waitNextMillis(currentTimestamp);
			}
		} else {
			sequence = 0;
		}
		
		lastTimestamp = currentTimestamp;
		
		return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
				| (nodeId << NODE_ID_SHIFT)
				| sequence;
	}
	
	/**
	 * Busy-wait for next millisecond
	 */
	private long waitNextMillis(long lastTimestamp) {
		long timestamp = currentTime();
		while (timestamp <= lastTimestamp) {
			Thread.yield(); // Reduce CPU waste
			timestamp = currentTime();
		}
		return timestamp;
	}
	
	private long currentTime() {
		return System.currentTimeMillis();
	}
	
	@Override
	public Long generateLongKey() {
		return nextId();
	}
	
	@Override
	@Deprecated(since = "1.1", forRemoval = false)
	public String generateKey() {
		return String.valueOf(nextId());
	}
}
