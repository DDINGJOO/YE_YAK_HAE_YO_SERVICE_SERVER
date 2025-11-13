package com.teambind.springproject.integration.concurrency;

import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PLACE Scope 상품의 시간대별 재고 관리 통합 테스트.
 * <p>
 * PostgreSQL 파티션 테이블(product_time_slot_inventory_*)을 사용하여
 * 시간대별 재고 예약/해제를 테스트합니다.
 * <p>
 * 주요 검증 사항:
 * - 시간대별 재고 예약 (reservePlaceTimeSlotQuantity)
 * - 시간대별 재고 해제/롤백 (releasePlaceTimeSlotQuantity)
 * - 동시성 제어 (Row-level lock)
 * - 트랜잭션 전파 (MANDATORY)
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("PLACE Scope 상품 시간대별 재고 관리 통합 테스트")
public class PlaceScopeProductInventoryTest extends BaseConcurrencyTest {

	@Autowired
	private ProductRepository productRepository;

	private ProductId testProductId;
	private RoomId testRoomId;
	private LocalDateTime testTimeSlot;

	@BeforeEach
	void setUp() {
		// Clean database before each test
		cleanDatabase();

		// PLACE Scope 상품 생성 - JdbcTemplate로 직접 삽입하여 트랜잭션 복잡도 제거
		final long productIdValue = System.currentTimeMillis();  // Simple ID generation
		testProductId = ProductId.of(productIdValue);

		final String sql = """
				INSERT INTO products
				(product_id, place_id, room_id, name, scope, pricing_type, initial_price, total_quantity, reserved_quantity)
				VALUES (?, ?, NULL, ?, 'PLACE', 'ONE_TIME', 10000, 10, 0)
				""";

		jdbcTemplate.update(sql,
			productIdValue,
			100L,  // placeId
			"공용 빔 프로젝터"
		);

		testRoomId = RoomId.of(200L);
		testTimeSlot = LocalDateTime.of(2025, 11, 15, 10, 0);
	}

	@Nested
	@DisplayName("시간대별 재고 예약 테스트")
	class ReserveTimeSlotQuantityTests {

		@Test
		@DisplayName("시간대별 재고를 성공적으로 예약한다")
		void reserveTimeSlotQuantity_success() {
			// given - 초기 재고 설정
			initializeInventory(testProductId, testRoomId, testTimeSlot, 10);

			// when
			final boolean result = productRepository.reservePlaceTimeSlotQuantity(
					testProductId,
					testRoomId,
					testTimeSlot,
					5  // 5개 예약
			);

			// then
			assertThat(result).isTrue();

			// 재고 확인
			final Integer reservedQuantity = getReservedQuantity(
					testProductId,
					testRoomId,
					testTimeSlot
			);
			assertThat(reservedQuantity).isEqualTo(5);
		}

		@Test
		@DisplayName("재고가 부족하면 예약이 실패한다")
		void reserveTimeSlotQuantity_insufficientInventory() {
			// given - 재고 3개만 있음
			initializeInventory(testProductId, testRoomId, testTimeSlot, 3);

			// when - 5개 예약 시도
			final boolean result = productRepository.reservePlaceTimeSlotQuantity(
					testProductId,
					testRoomId,
					testTimeSlot,
					5
			);

			// then - 실패
			assertThat(result).isFalse();

			// 재고 변경 없음
			final Integer reservedQuantity = getReservedQuantity(
					testProductId,
					testRoomId,
					testTimeSlot
			);
			assertThat(reservedQuantity).isEqualTo(0);
		}

		@Test
		@DisplayName("여러 시간대에 독립적으로 재고를 예약한다")
		void reserveMultipleTimeSlots_independently() {
			// given
			final LocalDateTime timeSlot1 = LocalDateTime.of(2025, 11, 15, 10, 0);
			final LocalDateTime timeSlot2 = LocalDateTime.of(2025, 11, 15, 11, 0);
			final LocalDateTime timeSlot3 = LocalDateTime.of(2025, 11, 15, 12, 0);

			initializeInventory(testProductId, testRoomId, timeSlot1, 10);
			initializeInventory(testProductId, testRoomId, timeSlot2, 10);
			initializeInventory(testProductId, testRoomId, timeSlot3, 10);

			// when - 각 시간대별로 다른 수량 예약
			final boolean result1 = productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, timeSlot1, 3);
			final boolean result2 = productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, timeSlot2, 5);
			final boolean result3 = productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, timeSlot3, 7);

			// then - 모두 성공
			assertThat(result1).isTrue();
			assertThat(result2).isTrue();
			assertThat(result3).isTrue();

			// 각 시간대별 재고 확인
			assertThat(getReservedQuantity(testProductId, testRoomId, timeSlot1)).isEqualTo(3);
			assertThat(getReservedQuantity(testProductId, testRoomId, timeSlot2)).isEqualTo(5);
			assertThat(getReservedQuantity(testProductId, testRoomId, timeSlot3)).isEqualTo(7);
		}
	}

	@Nested
	@DisplayName("시간대별 재고 해제(롤백) 테스트")
	class ReleaseTimeSlotQuantityTests {

		@Test
		@DisplayName("예약한 재고를 성공적으로 해제한다")
		void releaseTimeSlotQuantity_success() {
			// given - 재고 예약
			initializeInventory(testProductId, testRoomId, testTimeSlot, 10);
			productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, testTimeSlot, 7);

			// when - 5개 해제
			final boolean result = productRepository.releaseTimeSlotQuantity(
					testProductId,
					testRoomId,
					testTimeSlot,
					5
			);

			// then
			assertThat(result).isTrue();

			// 재고 확인 (7 - 5 = 2개 남음)
			final Integer reservedQuantity = getReservedQuantity(
					testProductId,
					testRoomId,
					testTimeSlot
			);
			assertThat(reservedQuantity).isEqualTo(2);
		}

		@Test
		@DisplayName("예약 후 전체 취소하면 재고가 완전히 복구된다")
		void releaseAllReservedQuantity() {
			// given
			initializeInventory(testProductId, testRoomId, testTimeSlot, 10);
			productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, testTimeSlot, 10);

			// when - 전체 해제
			final boolean result = productRepository.releaseTimeSlotQuantity(
					testProductId, testRoomId, testTimeSlot, 10);

			// then
			assertThat(result).isTrue();
			assertThat(getReservedQuantity(testProductId, testRoomId, testTimeSlot)).isEqualTo(0);
		}

		@Test
		@DisplayName("해제 후 재예약이 가능하다")
		void reserveAfterRelease() {
			// given - 예약 후 해제
			initializeInventory(testProductId, testRoomId, testTimeSlot, 10);
			productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, testTimeSlot, 8);
			productRepository.releaseTimeSlotQuantity(
					testProductId, testRoomId, testTimeSlot, 5);

			// when - 재예약 (현재 3개 예약됨, 7개 가능)
			final boolean result = productRepository.reservePlaceTimeSlotQuantity(
					testProductId, testRoomId, testTimeSlot, 6);

			// then
			assertThat(result).isTrue();
			assertThat(getReservedQuantity(testProductId, testRoomId, testTimeSlot)).isEqualTo(9);  // 3 + 6
		}
	}

	@Nested
	@DisplayName("동시성 제어 테스트")
	class ConcurrencyTests {

		@Test
		@DisplayName("동시에 10명이 시간대별 재고 5개를 예약하면 5명만 성공한다")
		void concurrentReservation_limitedInventory() throws InterruptedException {
			// given
			initializeInventory(testProductId, testRoomId, testTimeSlot, 5);

			final int threadCount = 10;
			final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
			final CountDownLatch latch = new CountDownLatch(threadCount);
			final AtomicInteger successCount = new AtomicInteger(0);
			final AtomicInteger failCount = new AtomicInteger(0);

			// when - 10명이 동시에 1개씩 예약
			for (int i = 0; i < threadCount; i++) {
				executorService.submit(() -> {
					try {
						final boolean result = productRepository.reservePlaceTimeSlotQuantity(
								testProductId,
								testRoomId,
								testTimeSlot,
								1
						);
						if (result) {
							successCount.incrementAndGet();
						} else {
							failCount.incrementAndGet();
						}
					} catch (Exception e) {
						failCount.incrementAndGet();
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await(10, TimeUnit.SECONDS);
			executorService.shutdown();

			// then - 재고 5개이므로 5명만 성공
			assertThat(successCount.get()).isEqualTo(5);
			assertThat(failCount.get()).isEqualTo(5);

			// 최종 재고 확인
			final Integer reservedQuantity = getReservedQuantity(
					testProductId,
					testRoomId,
					testTimeSlot
			);
			assertThat(reservedQuantity).isEqualTo(5);
		}

		@Test
		@DisplayName("동시 예약 후 동시 해제하면 재고가 정확히 복구된다")
		void concurrentReservationAndRelease() throws InterruptedException {
			// given - 재고 20개
			initializeInventory(testProductId, testRoomId, testTimeSlot, 20);

			// 먼저 10개 예약
			final int reserveThreadCount = 10;
			final ExecutorService reserveExecutor = Executors.newFixedThreadPool(reserveThreadCount);
			final CountDownLatch reserveLatch = new CountDownLatch(reserveThreadCount);

			for (int i = 0; i < reserveThreadCount; i++) {
				reserveExecutor.submit(() -> {
					try {
						productRepository.reservePlaceTimeSlotQuantity(
								testProductId, testRoomId, testTimeSlot, 1);
					} finally {
						reserveLatch.countDown();
					}
				});
			}
			reserveLatch.await(10, TimeUnit.SECONDS);
			reserveExecutor.shutdown();

			// 예약 확인
			assertThat(getReservedQuantity(testProductId, testRoomId, testTimeSlot))
					.isEqualTo(10);

			// when - 5개 동시 해제
			final int releaseThreadCount = 5;
			final ExecutorService releaseExecutor = Executors.newFixedThreadPool(releaseThreadCount);
			final CountDownLatch releaseLatch = new CountDownLatch(releaseThreadCount);
			final AtomicInteger releaseSuccessCount = new AtomicInteger(0);

			for (int i = 0; i < releaseThreadCount; i++) {
				releaseExecutor.submit(() -> {
					try {
						final boolean result = productRepository.releaseTimeSlotQuantity(
								testProductId, testRoomId, testTimeSlot, 1);
						if (result) {
							releaseSuccessCount.incrementAndGet();
						}
					} finally {
						releaseLatch.countDown();
					}
				});
			}

			releaseLatch.await(10, TimeUnit.SECONDS);
			releaseExecutor.shutdown();

			// then - 5개 해제 성공, 5개 남음
			assertThat(releaseSuccessCount.get()).isEqualTo(5);
			assertThat(getReservedQuantity(testProductId, testRoomId, testTimeSlot))
					.isEqualTo(5);
		}

		@Test
		@DisplayName("동시에 여러 시간대를 예약해도 각 시간대별로 독립적으로 처리된다")
		void concurrentReservationAcrossMultipleTimeSlots() throws InterruptedException {
			// given
			final LocalDateTime slot1 = LocalDateTime.of(2025, 11, 15, 10, 0);
			final LocalDateTime slot2 = LocalDateTime.of(2025, 11, 15, 11, 0);
			final LocalDateTime slot3 = LocalDateTime.of(2025, 11, 15, 12, 0);

			initializeInventory(testProductId, testRoomId, slot1, 5);
			initializeInventory(testProductId, testRoomId, slot2, 5);
			initializeInventory(testProductId, testRoomId, slot3, 5);

			final int totalThreads = 30;  // 각 시간대별로 10개씩
			final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
			final CountDownLatch latch = new CountDownLatch(totalThreads);
			final AtomicInteger successCount = new AtomicInteger(0);

			// when - 각 시간대에 10명씩 동시 예약 (총 30명)
			for (int i = 0; i < 10; i++) {
				executorService.submit(() -> {
					try {
						if (productRepository.reservePlaceTimeSlotQuantity(
								testProductId, testRoomId, slot1, 1)) {
							successCount.incrementAndGet();
						}
					} finally {
						latch.countDown();
					}
				});
			}

			for (int i = 0; i < 10; i++) {
				executorService.submit(() -> {
					try {
						if (productRepository.reservePlaceTimeSlotQuantity(
								testProductId, testRoomId, slot2, 1)) {
							successCount.incrementAndGet();
						}
					} finally {
						latch.countDown();
					}
				});
			}

			for (int i = 0; i < 10; i++) {
				executorService.submit(() -> {
					try {
						if (productRepository.reservePlaceTimeSlotQuantity(
								testProductId, testRoomId, slot3, 1)) {
							successCount.incrementAndGet();
						}
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await(10, TimeUnit.SECONDS);
			executorService.shutdown();

			// then - 각 시간대에서 5명씩만 성공 (총 15명)
			assertThat(successCount.get()).isEqualTo(15);

			// 각 시간대별 재고 확인
			assertThat(getReservedQuantity(testProductId, testRoomId, slot1)).isEqualTo(5);
			assertThat(getReservedQuantity(testProductId, testRoomId, slot2)).isEqualTo(5);
			assertThat(getReservedQuantity(testProductId, testRoomId, slot3)).isEqualTo(5);
		}
	}

	/**
	 * 테스트용 inventory 데이터 초기화.
	 * PostgreSQL 파티션 테이블에 직접 데이터를 삽입합니다.
	 */
	private void initializeInventory(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot,
			final int totalQuantity
	) {
		final String tableName = getPartitionTableName(timeSlot);
		final String sql = String.format(
				"INSERT INTO %s (product_id, room_id, time_slot, total_quantity, reserved_quantity) " +
						"VALUES (?, ?, ?, ?, 0) " +
						"ON CONFLICT (product_id, room_id, time_slot) DO UPDATE SET total_quantity = EXCLUDED.total_quantity",
				tableName
		);

		jdbcTemplate.update(sql,
				productId.getValue(),
				roomId.getValue(),
				timeSlot,
				totalQuantity
		);
	}

	/**
	 * 현재 예약된 수량을 조회합니다.
	 */
	private Integer getReservedQuantity(
			final ProductId productId,
			final RoomId roomId,
			final LocalDateTime timeSlot
	) {
		final String tableName = getPartitionTableName(timeSlot);
		final String sql = String.format(
				"SELECT reserved_quantity FROM %s WHERE product_id = ? AND room_id = ? AND time_slot = ?",
				tableName
		);

		return jdbcTemplate.queryForObject(sql, Integer.class,
				productId.getValue(),
				roomId.getValue(),
				timeSlot
		);
	}

	/**
	 * 시간대에 해당하는 파티션 테이블 이름을 반환합니다.
	 */
	private String getPartitionTableName(final LocalDateTime timeSlot) {
		return String.format("product_time_slot_inventory_%d_%02d",
				timeSlot.getYear(),
				timeSlot.getMonthValue()
		);
	}
}