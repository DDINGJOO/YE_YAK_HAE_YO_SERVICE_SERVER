package com.teambind.springproject.concurrency;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상품 재고 동시성 테스트 (RESERVATION Scope 전용).
 * <p>
 * 여러 스레드가 동시에 같은 상품을 예약하려고 할 때
 * 재고가 올바르게 관리되는지 검증합니다.
 * <p>
 * Task #142에서 구현한 원자적 재고 예약 메커니즘(Database Constraint)을 검증합니다.
 * - UPDATE 쿼리의 WHERE 조건에서 재고 검증 + 차감 동시 수행
 * - Row Lock을 활용한 Race Condition 방지
 * <p>
 * 주의: RESERVATION Scope 상품만 원자적 재고 예약을 지원합니다.
 * ROOM/PLACE Scope는 시간대별 재고 관리가 필요하여 별도 처리가 필요합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("상품 재고 동시성 테스트 (RESERVATION Scope)")
class ProductInventoryConcurrencyTest {

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
		testPlaceId = 1000L;
		testRoomId = 2000L;

		// 테스트 시간대 설정 (RESERVATION Scope는 시간 무관이지만 API 요구사항)
		testTimeSlots = List.of(
				LocalDateTime.of(2025, 1, 15, 10, 0),
				LocalDateTime.of(2025, 1, 15, 11, 0)
		);

		// PricingPolicy 설정 (예약 가격 계산용)
		final PricingPolicy pricingPolicy = PricingPolicy.create(
				RoomId.of(testRoomId),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("10000"))
		);
		pricingPolicyRepository.save(pricingPolicy);

		// 재고가 5개인 RESERVATION Scope 상품 생성
		final Product product = Product.createReservationScoped(
				ProductId.of(null),  // 자동 생성
				"동시성 테스트 상품",
				PricingStrategy.simpleStock(Money.of(1000)),
				5  // 총 재고 5개
		);
		final Product savedProduct = productRepository.save(product);
		productId = savedProduct.getProductId().getValue();  // 실제 할당된 ID 사용
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
		System.out.println("SUCCESS COUNT: " + successCount.get());
		System.out.println("FAIL COUNT: " + failCount.get());
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
		final Product largeInventoryProduct = Product.createReservationScoped(
				ProductId.of(null),  // 자동 생성
				"대량 재고 상품",
				PricingStrategy.simpleStock(Money.of(1000)),
				200
		);
		final Product savedLargeProduct = productRepository.save(largeInventoryProduct);
		final Long largeProductId = savedLargeProduct.getProductId().getValue();

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
							List.of(new ProductRequest(largeProductId, 1))
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
}
