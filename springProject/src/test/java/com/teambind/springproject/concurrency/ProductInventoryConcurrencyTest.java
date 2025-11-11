package com.teambind.springproject.concurrency;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.port.out.PricingPolicyRepository;
import com.teambind.springproject.application.port.out.ProductRepository;
import com.teambind.springproject.application.port.out.ReservationPricingRepository;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrice;
import com.teambind.springproject.domain.pricingpolicy.TimeRangePrices;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.shared.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상품 재고 동시성 테스트.
 * <p>
 * 여러 스레드가 동시에 같은 상품을 예약하려고 할 때
 * 재고가 올바르게 관리되는지 검증합니다.
 * <p>
 * 주의: 현재 시스템에 동시성 보호 메커니즘(Pessimistic Lock, Optimistic Lock 등)이
 * 구현되어 있지 않아 일부 테스트가 실패할 수 있습니다.
 * 이는 동시성 제어가 필요함을 보여주는 테스트입니다.
 * <p>
 * TODO: 동시성 보호를 위해 다음 중 하나를 구현해야 합니다:
 * - Pessimistic Lock (@Lock(LockModeType.PESSIMISTIC_WRITE))
 * - Optimistic Lock (@Version 필드 추가)
 * - Database 레벨 SELECT FOR UPDATE
 * - 분산 락 (Redis, Zookeeper 등)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("상품 재고 동시성 테스트")
class ProductInventoryConcurrencyTest {
	
	@Autowired
	private CreateReservationUseCase createReservationUseCase;
	
	@Autowired
	private PricingPolicyRepository pricingPolicyRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private ReservationPricingRepository reservationPricingRepository;
	
	private Long testRoomId;
	private Long testPlaceId;
	private Long productId;
	private List<LocalDateTime> testTimeSlots;
	
	@BeforeEach
	void setUp() {
		testPlaceId = 1000L;
		testRoomId = 2000L;
		productId = 3000L;
		
		// 테스트 시간대 설정 (2025-01-15 10:00-12:00)
		testTimeSlots = List.of(
				LocalDateTime.of(2025, 1, 15, 10, 0),
				LocalDateTime.of(2025, 1, 15, 11, 0)
		);
		
		// PricingPolicy 설정
		final TimeRangePrices timeRangePrices = TimeRangePrices.of(List.of(
				new TimeRangePrice(
						DayOfWeek.WEDNESDAY,
						TimeRange.of(LocalTime.of(0, 0), LocalTime.of(23, 59)),
						Money.of(new BigDecimal("10000"))
				)
		));
		
		final PricingPolicy pricingPolicy = PricingPolicy.createWithTimeRangePrices(
				RoomId.of(testRoomId),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("10000")),
				timeRangePrices
		);
		pricingPolicyRepository.save(pricingPolicy);
		
		// 재고가 5개인 ROOM 범위 상품 생성
		final Product product = Product.createRoomScoped(
				ProductId.of(productId),
				PlaceId.of(testPlaceId),
				RoomId.of(testRoomId),
				"동시성 테스트 상품",
				PricingStrategy.simpleStock(Money.of(1000)),
				5  // 총 재고 5개
		);
		productRepository.save(product);
	}
	
	@Test
	@DisplayName("동시에 10명이 5개 재고 상품을 예약하면 5명만 성공한다")
	void concurrentReservationsWithLimitedInventory() throws InterruptedException {
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
							List.of(new ProductRequest(productId, 1))  // 1개씩 요청
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
		assertThat(successCount.get()).isEqualTo(5);
		assertThat(failCount.get()).isEqualTo(5);
		
		// 성공한 예약들 확인
		final long confirmedReservations = reservationPricingRepository
				.findByStatusIn(List.of(ReservationStatus.PENDING))
				.size();
		assertThat(confirmedReservations).isEqualTo(5);
	}
	
	@Test
	@DisplayName("동시에 3명이 각각 2개씩 예약하면 2명만 성공한다 (재고 5개)")
	void concurrentReservationsWithMultipleQuantity() throws InterruptedException {
		// given
		final int threadCount = 3;
		final int quantityPerRequest = 2;
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failCount = new AtomicInteger(0);
		
		// when - 3명이 동시에 2개씩 예약 시도 (총 6개 요청 > 재고 5개)
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(productId, quantityPerRequest))
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
		
		// then - 2개씩 요청이므로 2명만 성공 (2*2=4개 사용, 1개 남음)
		assertThat(successCount.get()).isEqualTo(2);
		assertThat(failCount.get()).isEqualTo(1);
	}
	
	@Test
	@DisplayName("동시에 100명이 재고 충분한 상품을 예약하면 모두 성공한다")
	void concurrentReservationsWithSufficientInventory() throws InterruptedException {
		// given - 재고를 200개로 늘림
		final Product largeInventoryProduct = Product.createRoomScoped(
				ProductId.of(4000L),
				PlaceId.of(testPlaceId),
				RoomId.of(testRoomId),
				"대량 재고 상품",
				PricingStrategy.simpleStock(Money.of(1000)),
				200
		);
		productRepository.save(largeInventoryProduct);
		
		final int threadCount = 100;
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failCount = new AtomicInteger(0);
		
		// when - 100명이 동시에 1개씩 예약
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(4000L, 1))
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
		
		latch.await(30, TimeUnit.SECONDS);
		executorService.shutdown();
		
		// then - 재고가 충분하므로 모두 성공
		assertThat(successCount.get()).isEqualTo(100);
		assertThat(failCount.get()).isEqualTo(0);
	}
	
	@Test
	@DisplayName("동시 예약 후 취소하면 재고가 복구되어 다시 예약 가능하다")
	void inventoryRestoredAfterCancellation() throws InterruptedException {
		// given - 먼저 5개 모두 예약
		final List<ReservationPricingResponse> reservations = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			final CreateReservationRequest request = new CreateReservationRequest(
					testRoomId,
					testTimeSlots,
					List.of(new ProductRequest(productId, 1))
			);
			reservations.add(createReservationUseCase.createReservation(request));
		}
		
		// when - 2개 취소
		reservations.subList(0, 2).forEach(reservation ->
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
	@DisplayName("동일 시간대에 대한 동시 예약만 재고 충돌이 발생한다")
	void concurrentReservationsOnlyConflictInSameTimeSlot() throws InterruptedException {
		// given - 다른 시간대 설정
		final List<LocalDateTime> differentTimeSlots = List.of(
				LocalDateTime.of(2025, 1, 15, 14, 0),  // 다른 시간
				LocalDateTime.of(2025, 1, 15, 15, 0)
		);
		
		final int threadCount = 10;
		final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		final CountDownLatch latch = new CountDownLatch(threadCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		
		// when - 5명은 기존 시간대, 5명은 다른 시간대에 예약
		for (int i = 0; i < threadCount; i++) {
			final List<LocalDateTime> timeSlots = (i < 5) ? testTimeSlots : differentTimeSlots;
			executorService.submit(() -> {
				try {
					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							timeSlots,
							List.of(new ProductRequest(productId, 1))
					);
					createReservationUseCase.createReservation(request);
					successCount.incrementAndGet();
				} catch (Exception e) {
					// 실패 무시
				} finally {
					latch.countDown();
				}
			});
		}
		
		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();
		
		// then - 각 시간대마다 재고 5개이므로 총 10개 예약 가능
		assertThat(successCount.get()).isEqualTo(10);
	}
}
