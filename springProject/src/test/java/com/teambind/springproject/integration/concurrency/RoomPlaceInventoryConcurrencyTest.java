package com.teambind.springproject.integration.concurrency;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ROOM/PLACE Scope 상품 시간대별 재고 동시성 테스트.
 * <p>
 * Task #157에서 구현한 시간대별 원자적 재고 예약 메커니즘을 검증합니다.
 * - product_time_slot_inventory 테이블 기반 재고 관리
 * - INSERT ON CONFLICT UPDATE + EXISTS 서브쿼리로 동시성 제어
 * - Row Lock을 활용한 Race Condition 방지
 * <p>
 * BaseConcurrencyTest를 상속하여 IntegrationTestContainers를 재사용합니다.
 */
@DisplayName("ROOM/PLACE Scope 시간대별 재고 동시성 테스트")
class RoomPlaceInventoryConcurrencyTest extends BaseConcurrencyTest {

	@Autowired
	private CreateReservationUseCase createReservationUseCase;

	@Autowired
	private PricingPolicyRepository pricingPolicyRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ReservationPricingRepository reservationPricingRepository;

	private Long testPlaceId;
	private Long testRoomId;
	private Long productId;
	private List<LocalDateTime> testTimeSlots;

	@BeforeEach
	void setUp() {
		// Clean database first
		cleanDatabase();

		testPlaceId = 1000L;
		testRoomId = 2000L;

		// 테스트 시간대 설정 (10:00 ~ 12:00, 2시간)
		testTimeSlots = List.of(
				LocalDateTime.of(2025, 1, 15, 10, 0),
				LocalDateTime.of(2025, 1, 15, 11, 0)
		);

		// PricingPolicy 설정
		final PricingPolicy pricingPolicy = PricingPolicy.create(
				RoomId.of(testRoomId),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("10000"))
		);
		pricingPolicyRepository.save(pricingPolicy);

		// 재고가 5개인 ROOM Scope 상품 생성
		final Product product = Product.createRoomScoped(
				ProductId.of(null),  // 자동 생성
				PlaceId.of(testPlaceId),
				RoomId.of(testRoomId),
				"ROOM 동시성 테스트 상품",
				PricingStrategy.simpleStock(Money.of(1000)),
				5  // 총 재고 5개
		);
		final Product savedProduct = productRepository.save(product);
		productId = savedProduct.getProductId().getValue();
	}

	@Test
	@DisplayName("ROOM Scope: 동시에 10명이 5개 재고 상품을 예약하면 5명만 성공한다")
	void roomScope_concurrentReservationsWithLimitedInventory() throws InterruptedException {
		// given
		final int threadCount = 10;
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failCount = new AtomicInteger(0);

		// when - 10명이 동시에 1개씩 예약 시도
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(productId, 1))
					);

					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();

		// then - 재고가 5개이므로 5명만 성공해야 함
		System.out.println("[ROOM Scope] SUCCESS: " + successCount.get() + ", FAIL: " + failCount.get());
		assertThat(successCount.get()).isEqualTo(5);
		assertThat(failCount.get()).isEqualTo(5);

		// 성공한 예약들 확인
		final long confirmedReservations = reservationPricingRepository
				.findByStatusIn(List.of(ReservationStatus.PENDING))
				.size();
		assertThat(confirmedReservations).isEqualTo(5);
	}

	@Test
	@DisplayName("ROOM Scope: 다른 시간대 예약은 독립적으로 처리된다")
	void roomScope_differentTimeSlotsShouldBeIndependent() throws InterruptedException {
		// given - 시간대 A (10:00-11:00)와 시간대 B (14:00-15:00)
		final List<LocalDateTime> timeSlotsA = List.of(LocalDateTime.of(2025, 1, 15, 10, 0));
		final List<LocalDateTime> timeSlotsB = List.of(LocalDateTime.of(2025, 1, 15, 14, 0));

		final int threadCount = 10;  // 각 시간대별로 10명씩
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);
		final CountDownLatch latch = new CountDownLatch(threadCount * 2);
		final AtomicInteger successCountA = new AtomicInteger(0);
		final AtomicInteger successCountB = new AtomicInteger(0);
		final AtomicInteger failCountA = new AtomicInteger(0);
		final AtomicInteger failCountB = new AtomicInteger(0);

		// when - 시간대 A에서 10명 예약
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							timeSlotsA,
							List.of(new ProductRequest(productId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCountA.incrementAndGet();
				} catch (Exception e) {
					failCountA.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		// when - 시간대 B에서 10명 예약
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							timeSlotsB,
							List.of(new ProductRequest(productId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCountB.incrementAndGet();
				} catch (Exception e) {
					failCountB.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();

		// then - 각 시간대마다 독립적으로 5명씩 성공
		System.out.println("[Time Slot A] SUCCESS: " + successCountA.get() + ", FAIL: " + failCountA.get());
		System.out.println("[Time Slot B] SUCCESS: " + successCountB.get() + ", FAIL: " + failCountB.get());
		assertThat(successCountA.get()).isEqualTo(5);
		assertThat(failCountA.get()).isEqualTo(5);
		assertThat(successCountB.get()).isEqualTo(5);
		assertThat(failCountB.get()).isEqualTo(5);
	}

	@Test
	@DisplayName("ROOM Scope: 예약 취소 후 재고가 복구되어 다시 예약 가능하다")
	void roomScope_inventoryRestoredAfterCancellation() throws InterruptedException {
		// given - 먼저 5개 모두 예약
		for (int i = 0; i < 5; i++) {
			final CreateReservationRequest request = new CreateReservationRequest(
					testRoomId,
					testTimeSlots,
					List.of(new ProductRequest(productId, 1))
			);
			createReservationUseCase.createReservation(request);
		}

		// when - 2개 취소
		final List<ReservationPricingResponse> allReservations = reservationPricingRepository
				.findByStatusIn(List.of(ReservationStatus.PENDING))
				.stream()
				.map(ReservationPricingResponse::from)
				.toList();

		allReservations.subList(0, 2).forEach(reservation ->
				createReservationUseCase.cancelReservation(reservation.reservationId())
		);

		// then - 취소 후 2개 다시 예약 가능
		final int threadCount = 3;  // 3명이 시도하지만 2명만 성공
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(productId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();

		assertThat(successCount.get()).isEqualTo(2);  // 취소한 2개만 예약 가능
		assertThat(failCount.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("PLACE Scope: 여러 룸에서 동시 예약 시 전체 Place 재고 제한이 적용된다")
	void placeScope_multipleRoomsConcurrentReservations() throws InterruptedException {
		// given - PLACE Scope 상품 생성 (재고 5개)
		final Product placeProduct = Product.createPlaceScoped(
				ProductId.of(null),
				PlaceId.of(testPlaceId),
				"PLACE 동시성 테스트 상품",
				PricingStrategy.simpleStock(Money.of(1000)),
				5
		);
		final Product savedPlaceProduct = productRepository.save(placeProduct);
		final Long placeProductId = savedPlaceProduct.getProductId().getValue();

		// 추가 룸 생성 (같은 Place에 속함)
		final Long room2Id = 2001L;
		final Long room3Id = 2002L;

		final PricingPolicy pricingPolicy2 = PricingPolicy.create(
				RoomId.of(room2Id),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("10000"))
		);
		pricingPolicyRepository.save(pricingPolicy2);

		final PricingPolicy pricingPolicy3 = PricingPolicy.create(
				RoomId.of(room3Id),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("10000"))
		);
		pricingPolicyRepository.save(pricingPolicy3);

		// PLACE 상품을 각 룸에서 접근 가능하도록 화이트리스트 등록
		jdbcTemplate.update(
				"INSERT INTO room_allowed_products (room_id, product_id, created_at) VALUES (?, ?, NOW())",
				testRoomId, placeProductId
		);
		jdbcTemplate.update(
				"INSERT INTO room_allowed_products (room_id, product_id, created_at) VALUES (?, ?, NOW())",
				room2Id, placeProductId
		);
		jdbcTemplate.update(
				"INSERT INTO room_allowed_products (room_id, product_id, created_at) VALUES (?, ?, NOW())",
				room3Id, placeProductId
		);

		// when - 3개 룸에서 각각 동시에 예약 시도 (총 15명, 재고 5개)
		final int threadsPerRoom = 5;
		final int totalThreads = threadsPerRoom * 3;
		final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
		final CountDownLatch latch = new CountDownLatch(totalThreads);
		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failCount = new AtomicInteger(0);

		// Room 1에서 5명
		for (int i = 0; i < threadsPerRoom; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(placeProductId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					System.out.println("[PLACE Test] Room1 예약 실패: " + e.getMessage());
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		// Room 2에서 5명
		for (int i = 0; i < threadsPerRoom; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							room2Id,
							testTimeSlots,
							List.of(new ProductRequest(placeProductId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					System.out.println("[PLACE Test] Room2 예약 실패: " + e.getMessage());
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		// Room 3에서 5명
		for (int i = 0; i < threadsPerRoom; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							room3Id,
							testTimeSlots,
							List.of(new ProductRequest(placeProductId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					System.out.println("[PLACE Test] Room3 예약 실패: " + e.getMessage());
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(15, TimeUnit.SECONDS);
		executorService.shutdown();

		// then - 전체 Place 재고가 5개이므로 총 5명만 성공 (룸 무관)
		System.out.println("[PLACE Scope] SUCCESS: " + successCount.get() + ", FAIL: " + failCount.get());
		assertThat(successCount.get()).isEqualTo(5);
		assertThat(failCount.get()).isEqualTo(10);
	}

	@Test
	@DisplayName("ROOM Scope: 겹치는 시간대 예약 시 각 시간대별 재고가 차감된다")
	void roomScope_overlappingTimeSlotInventoryDecrement() throws InterruptedException {
		// given - 2시간 예약 (10:00-11:00, 11:00-12:00)
		final int threadCount = 5;
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);

		// when - 5명이 동시에 2시간 예약
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,  // 10:00-11:00, 11:00-12:00
							List.of(new ProductRequest(productId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 재고 부족으로 실패
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();

		// then - 각 시간대마다 재고 5개이므로 모두 성공
		assertThat(successCount.get()).isEqualTo(5);

		// 추가 검증: 이제 10:00-11:00 시간대만 예약 시도하면 실패해야 함
		try {
			final CreateReservationRequest request = new CreateReservationRequest(
					testRoomId,
					List.of(LocalDateTime.of(2025, 1, 15, 10, 0)),
					List.of(new ProductRequest(productId, 1))
			);
			createReservationUseCase.createReservation(request);
			throw new AssertionError("Should fail due to insufficient inventory");
		} catch (Exception e) {
			// 예상된 실패
			assertThat(e).hasMessageContaining("Product is not available");
		}
	}
}