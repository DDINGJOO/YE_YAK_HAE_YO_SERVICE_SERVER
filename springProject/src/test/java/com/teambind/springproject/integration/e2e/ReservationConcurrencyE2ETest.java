package com.teambind.springproject.integration.e2e;

import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

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
 * 예약 동시성 제어 E2E 테스트.
 *
 * 실제 HTTP API를 통해 동시성 시나리오를 검증합니다:
 * - 동시 예약 시도 시 오버부킹 방지
 * - 여러 상품 예약 실패 시 롤백
 * - 예약 취소 후 재예약 가능성
 *
 * Issue #146 - 재고 동시성 제어 E2E 테스트 작성
 */
@DisplayName("예약 동시성 제어 E2E 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReservationConcurrencyE2ETest extends BaseE2ETest {

	@Autowired
	private PricingPolicyRepository pricingPolicyRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ReservationPricingRepository reservationPricingRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long testPlaceId;
	private Long testRoomId;
	private Long testProductId;
	private List<LocalDateTime> testTimeSlots;

	@BeforeEach
	void setUpTestData() {
		testPlaceId = 999L;
		testRoomId = 9999L;

		// Test time slots
		testTimeSlots = List.of(
				LocalDateTime.of(2025, 1, 20, 10, 0),
				LocalDateTime.of(2025, 1, 20, 11, 0)
		);

		// Create pricing policy
		final PricingPolicy pricingPolicy = PricingPolicy.create(
				RoomId.of(testRoomId),
				PlaceId.of(testPlaceId),
				TimeSlot.HOUR,
				Money.of(new BigDecimal("10000"))
		);
		pricingPolicyRepository.save(pricingPolicy);
	}

	@Test
	@DisplayName("Scenario 1: 동시 예약 시도 - 오버부킹 방지 검증")
	void scenario1_ConcurrentReservations_PreventOverbooking() throws InterruptedException {
		// Given: 재고가 5개인 상품 생성
		final Product product = Product.createReservationScoped(
				ProductId.of(null),
				"동시성 테스트 상품",
				PricingStrategy.simpleStock(Money.of(5000)),
				5  // 총 재고 5개
		);
		final Product savedProduct = productRepository.save(product);
		testProductId = savedProduct.getProductId().getValue();

		final int totalThreads = 10;  // 10명의 사용자
		final int requestedQuantity = 1;  // 각각 1개씩 예약 시도
		final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch completeLatch = new CountDownLatch(totalThreads);

		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failureCount = new AtomicInteger(0);

		// When: 10명이 동시에 1개씩 예약 시도 (총 10개 요청, 재고는 5개)
		for (int i = 0; i < totalThreads; i++) {
			executorService.submit(() -> {
				try {
					startLatch.await();  // 동시 시작 보장

					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(testProductId, requestedQuantity))
					);

					final ResponseEntity<ReservationPricingResponse> response = restTemplate.postForEntity(
							getBaseUrl() + "/api/v1/reservations",
							request,
							ReservationPricingResponse.class
					);

					if (response.getStatusCode() == HttpStatus.CREATED) {
						successCount.incrementAndGet();
					} else {
						failureCount.incrementAndGet();
					}

				} catch (final Exception e) {
					// 재고 부족 등의 예외 발생 시
					failureCount.incrementAndGet();
				} finally {
					completeLatch.countDown();
				}
			});
		}

		startLatch.countDown();  // 모든 스레드 동시 시작
		final boolean finished = completeLatch.await(30, TimeUnit.SECONDS);
		executorService.shutdown();

		// Then: 정확히 5개만 성공, 5개는 실패해야 함 (오버부킹 방지)
		assertThat(finished).isTrue();
		assertThat(successCount.get()).isEqualTo(5);
		assertThat(failureCount.get()).isEqualTo(5);

		// Verify: DB에 저장된 reserved_quantity 확인
		final Integer reservedQuantity = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				testProductId
		);
		assertThat(reservedQuantity).isEqualTo(5);
	}

	@Test
	@DisplayName("Scenario 2: 여러 상품 동시 예약 - 롤백 로직 검증")
	void scenario2_MultipleProducts_RollbackOnFailure() {
		// Given: 재고가 충분한 상품 A와 재고가 부족한 상품 B 생성
		final Product productA = Product.createReservationScoped(
				ProductId.of(null),
				"상품 A (재고 충분)",
				PricingStrategy.simpleStock(Money.of(3000)),
				10  // 재고 10개
		);
		final Product savedProductA = productRepository.save(productA);
		final Long productAId = savedProductA.getProductId().getValue();

		final Product productB = Product.createReservationScoped(
				ProductId.of(null),
				"상품 B (재고 부족)",
				PricingStrategy.simpleStock(Money.of(5000)),
				2  // 재고 2개만
		);
		final Product savedProductB = productRepository.save(productB);
		final Long productBId = savedProductB.getProductId().getValue();

		// When: 상품 A (5개) + 상품 B (5개) 동시 예약 시도 → B 재고 부족으로 실패 예상
		final CreateReservationRequest request = new CreateReservationRequest(
				testRoomId,
				testTimeSlots,
				List.of(
						new ProductRequest(productAId, 5),
						new ProductRequest(productBId, 5)  // 재고 부족!
				)
		);

		boolean requestFailed = false;
		try {
			final ResponseEntity<ReservationPricingResponse> response = restTemplate.postForEntity(
					getBaseUrl() + "/api/v1/reservations",
					request,
					ReservationPricingResponse.class
			);

			// 예약 실패 예상 (400 Bad Request 또는 422 Unprocessable Entity)
			if (response.getStatusCode() == HttpStatus.BAD_REQUEST
					|| response.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
				requestFailed = true;
			}
		} catch (final Exception e) {
			// RestClientException 또는 HttpClientErrorException 발생 시도 실패로 간주
			requestFailed = true;
		}

		// Then: 예약 실패해야 함
		assertThat(requestFailed).isTrue();

		// Verify: 상품 A의 재고도 롤백되어 원래대로 (0개)여야 함
		final Integer reservedQuantityA = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				productAId
		);
		assertThat(reservedQuantityA).isEqualTo(0);  // 롤백 확인

		final Integer reservedQuantityB = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				productBId
		);
		assertThat(reservedQuantityB).isEqualTo(0);  // 애초에 예약 실패
	}

	@Test
	@DisplayName("Scenario 3: 예약 취소 후 재예약 가능성 검증")
	void scenario3_CancelAndReReserve_InventoryRestored() {
		// Given: 재고가 3개인 상품 생성
		final Product product = Product.createReservationScoped(
				ProductId.of(null),
				"재예약 테스트 상품",
				PricingStrategy.simpleStock(Money.of(4000)),
				3  // 총 재고 3개
		);
		final Product savedProduct = productRepository.save(product);
		testProductId = savedProduct.getProductId().getValue();

		// Step 1: 3개 모두 예약
		final CreateReservationRequest firstRequest = new CreateReservationRequest(
				testRoomId,
				testTimeSlots,
				List.of(new ProductRequest(testProductId, 3))
		);

		final ResponseEntity<ReservationPricingResponse> firstResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				firstRequest,
				ReservationPricingResponse.class
		);

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(firstResponse.getBody()).isNotNull();
		final Long firstReservationId = firstResponse.getBody().reservationId();

		// Verify: 재고 모두 소진
		Integer reservedQuantity = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				testProductId
		);
		assertThat(reservedQuantity).isEqualTo(3);

		// Step 2: 예약 취소 (재고 복구)
		final ResponseEntity<ReservationPricingResponse> cancelResponse = restTemplate.exchange(
				getBaseUrl() + "/api/v1/reservations/" + firstReservationId + "/cancel",
				org.springframework.http.HttpMethod.PUT,
				null,
				ReservationPricingResponse.class
		);

		assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(cancelResponse.getBody()).isNotNull();
		assertThat(cancelResponse.getBody().status()).isEqualTo(ReservationStatus.CANCELLED);

		// Verify: 재고 복구 확인
		reservedQuantity = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				testProductId
		);
		assertThat(reservedQuantity).isEqualTo(0);  // 재고 복구됨

		// Step 3: 다시 2개 예약 시도 (재예약 가능해야 함)
		final CreateReservationRequest secondRequest = new CreateReservationRequest(
				testRoomId,
				testTimeSlots,
				List.of(new ProductRequest(testProductId, 2))
		);

		final ResponseEntity<ReservationPricingResponse> secondResponse = restTemplate.postForEntity(
				getBaseUrl() + "/api/v1/reservations",
				secondRequest,
				ReservationPricingResponse.class
		);

		// Then: 재예약 성공
		assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(secondResponse.getBody()).isNotNull();
		assertThat(secondResponse.getBody().status()).isEqualTo(ReservationStatus.PENDING);

		// Verify: 재고 2개 차감
		reservedQuantity = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				testProductId
		);
		assertThat(reservedQuantity).isEqualTo(2);
	}

	@Test
	@DisplayName("Scenario 4: 동시 예약 및 취소 - 재고 정합성 검증")
	void scenario4_ConcurrentReserveAndCancel_InventoryConsistency() throws InterruptedException {
		// Given: 재고가 10개인 상품 생성
		final Product product = Product.createReservationScoped(
				ProductId.of(null),
				"동시 예약/취소 테스트 상품",
				PricingStrategy.simpleStock(Money.of(6000)),
				10  // 총 재고 10개
		);
		final Product savedProduct = productRepository.save(product);
		testProductId = savedProduct.getProductId().getValue();

		final int reserveThreads = 5;  // 5명이 예약 시도
		final ExecutorService executorService = Executors.newFixedThreadPool(reserveThreads);
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch completeLatch = new CountDownLatch(reserveThreads);

		final Long[] reservationIds = new Long[reserveThreads];

		// When: 5명이 동시에 2개씩 예약 (총 10개 = 재고와 동일)
		for (int i = 0; i < reserveThreads; i++) {
			final int index = i;
			executorService.submit(() -> {
				try {
					startLatch.await();

					final CreateReservationRequest request = new CreateReservationRequest(
							testRoomId,
							testTimeSlots,
							List.of(new ProductRequest(testProductId, 2))
					);

					final ResponseEntity<ReservationPricingResponse> response = restTemplate.postForEntity(
							getBaseUrl() + "/api/v1/reservations",
							request,
							ReservationPricingResponse.class
					);

					if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
						reservationIds[index] = response.getBody().reservationId();
					}

				} catch (final Exception e) {
					// Ignore exceptions
				} finally {
					completeLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		final boolean finished = completeLatch.await(30, TimeUnit.SECONDS);
		executorService.shutdown();

		assertThat(finished).isTrue();

		// Then: 정확히 5개 예약 성공 (각 2개씩 = 총 10개)
		final long successfulReservations = java.util.Arrays.stream(reservationIds)
				.filter(java.util.Objects::nonNull)
				.count();
		assertThat(successfulReservations).isEqualTo(5);

		// Verify: 재고 전부 소진
		Integer reservedQuantity = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				testProductId
		);
		assertThat(reservedQuantity).isEqualTo(10);

		// When: 2개의 예약을 취소
		int cancelCount = 0;
		for (final Long reservationId : reservationIds) {
			if (reservationId != null && cancelCount < 2) {
				restTemplate.exchange(
						getBaseUrl() + "/api/v1/reservations/" + reservationId + "/cancel",
						org.springframework.http.HttpMethod.PUT,
						null,
						ReservationPricingResponse.class
				);
				cancelCount++;
			}
		}

		// Then: 재고 4개 복구 (2개 예약 취소 × 각 2개 = 4개)
		reservedQuantity = jdbcTemplate.queryForObject(
				"SELECT reserved_quantity FROM products WHERE product_id = ?",
				Integer.class,
				testProductId
		);
		assertThat(reservedQuantity).isEqualTo(6);  // 10 - 4 = 6
	}
}