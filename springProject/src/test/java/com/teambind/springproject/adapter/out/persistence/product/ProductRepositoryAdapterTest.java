package com.teambind.springproject.adapter.out.persistence.product;

import com.teambind.springproject.application.port.out.RoomAllowedProductRepository;
import com.teambind.springproject.domain.product.Product;
import com.teambind.springproject.domain.product.pricing.PricingStrategy;
import com.teambind.springproject.domain.product.vo.PricingType;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.PlaceId;
import com.teambind.springproject.domain.shared.ProductId;
import com.teambind.springproject.domain.shared.RoomId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
		ProductRepositoryAdapter.class,
		com.teambind.springproject.common.config.CustomConfig.class,
		com.teambind.springproject.common.util.generator.SnowflakeIdGenerator.class
})
@DisplayName("ProductRepository 통합 테스트")
class ProductRepositoryAdapterTest {
	
	@Autowired
	private ProductRepositoryAdapter repository;

	@Autowired
	private ProductJpaRepository jpaRepository;

	@MockBean
	private RoomAllowedProductRepository roomAllowedProductRepository;
	
	@BeforeEach
	void setUp() {
		// Mock returns empty list by default for all roomIds
		when(roomAllowedProductRepository.findAllowedProductIdsByRoomId(any(Long.class)))
				.thenReturn(Collections.emptyList());
	}
	
	@Nested
	@DisplayName("CRUD 동작 테스트")
	class CrudTests {
		
		@Test
		@DisplayName("PLACE 범위 상품 저장 및 조회")
		void savePlaceScopedProduct() {
			// given
			final Product product = Product.createPlaceScoped(
					ProductId.of(null), // Auto-generated ID
					PlaceId.of(100L),
					"공용 빔 프로젝터",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			assertThat(saved).isNotNull();
			assertThat(saved.getProductId()).isNotNull();
			
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getScope()).isEqualTo(ProductScope.PLACE);
			assertThat(found.get().getPlaceId()).isEqualTo(PlaceId.of(100L));
			assertThat(found.get().getRoomId()).isNull();
			assertThat(found.get().getName()).isEqualTo("공용 빔 프로젝터");
			assertThat(found.get().getTotalQuantity()).isEqualTo(5);
		}
		
		@Test
		@DisplayName("ROOM 범위 상품 저장 및 조회")
		void saveRoomScopedProduct() {
			// given
			final Product product = Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					RoomId.of(200L),
					"룸 전용 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					10
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getScope()).isEqualTo(ProductScope.ROOM);
			assertThat(found.get().getPlaceId()).isEqualTo(PlaceId.of(100L));
			assertThat(found.get().getRoomId()).isEqualTo(RoomId.of(200L));
			assertThat(found.get().getName()).isEqualTo("룸 전용 화이트보드");
		}
		
		@Test
		@DisplayName("RESERVATION 범위 상품 저장 및 조회")
		void saveReservationScopedProduct() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"음료수",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getScope()).isEqualTo(ProductScope.RESERVATION);
			assertThat(found.get().getPlaceId()).isNull();
			assertThat(found.get().getRoomId()).isNull();
			assertThat(found.get().getName()).isEqualTo("음료수");
		}
		
		@Test
		@DisplayName("상품 수정 후 저장")
		void updateProduct() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"원래 이름",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when
			saved.updateName("새로운 이름");
			saved.updateTotalQuantity(20);
			repository.save(saved);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			assertThat(found.get().getName()).isEqualTo("새로운 이름");
			assertThat(found.get().getTotalQuantity()).isEqualTo(20);
		}
		
		@Test
		@DisplayName("상품 삭제")
		void deleteProduct() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"삭제할 상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when
			repository.deleteById(saved.getProductId());
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isEmpty();
		}
		
		@Test
		@DisplayName("상품 존재 여부 확인")
		void existsById() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when & then
			assertThat(repository.existsById(saved.getProductId())).isTrue();
			assertThat(repository.existsById(ProductId.of(99999L))).isFalse();
		}
	}
	
	@Nested
	@DisplayName("Scope별 조회 테스트")
	class ScopeQueryTests {

		@BeforeEach
		void setUpScope() {
			// 각 테스트 전 데이터 정리
			jpaRepository.deleteAll();
		}

		@Test
		@DisplayName("PlaceId로 해당 플레이스의 모든 상품 조회")
		void findByPlaceId() {
			// given
			final PlaceId placeId = PlaceId.of(100L);
			
			// PLACE 범위 상품
			repository.save(Product.createPlaceScoped(
					ProductId.of(null),
					placeId,
					"공용 빔 프로젝터",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			));
			
			// ROOM 범위 상품 (같은 PLACE)
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					placeId,
					RoomId.of(201L),
					"룸 A 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					placeId,
					RoomId.of(202L),
					"룸 B 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			// 다른 PLACE의 상품
			repository.save(Product.createPlaceScoped(
					ProductId.of(null),
					PlaceId.of(200L),
					"다른 플레이스 상품",
					PricingStrategy.oneTime(Money.of(5000)),
					3
			));
			
			// when
			final List<Product> products = repository.findByPlaceId(placeId);
			
			// then - PLACE 범위 1개 + ROOM 범위 2개 = 총 3개
			assertThat(products).hasSize(3);
			assertThat(products).allMatch(p -> p.getPlaceId().equals(placeId));
		}
		
		@Test
		@DisplayName("RoomId로 해당 룸의 상품 조회")
		void findByRoomId() {
			// given
			final RoomId roomId = RoomId.of(201L);
			
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					roomId,
					"룸 A 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			repository.save(Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					RoomId.of(202L),
					"룸 B 화이트보드",
					PricingStrategy.simpleStock(Money.of(3000)),
					2
			));
			
			// when
			final List<Product> products = repository.findByRoomId(roomId);
			
			// then
			assertThat(products).hasSize(1);
			assertThat(products.get(0).getRoomId()).isEqualTo(roomId);
		}
		
		@Test
		@DisplayName("ProductScope로 상품 조회")
		void findByScope() {
			// given
			repository.save(Product.createPlaceScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					"PLACE 상품",
					PricingStrategy.oneTime(Money.of(10000)),
					5
			));
			
			repository.save(Product.createReservationScoped(
					ProductId.of(null),
					"RESERVATION 상품 1",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			));
			
			repository.save(Product.createReservationScoped(
					ProductId.of(null),
					"RESERVATION 상품 2",
					PricingStrategy.simpleStock(Money.of(3000)),
					50
			));
			
			// when
			final List<Product> reservationProducts = repository.findByScope(ProductScope.RESERVATION);
			final List<Product> placeProducts = repository.findByScope(ProductScope.PLACE);
			
			// then
			assertThat(reservationProducts).hasSize(2);
			assertThat(placeProducts).hasSize(1);
			assertThat(reservationProducts).allMatch(p -> p.getScope() == ProductScope.RESERVATION);
		}
	}
	
	@Nested
	@DisplayName("PricingStrategy 저장/조회 테스트")
	class PricingStrategyTests {
		
		@Test
		@DisplayName("INITIAL_PLUS_ADDITIONAL 타입 저장 및 조회")
		void saveInitialPlusAdditionalType() {
			// given
			final Product product = Product.createPlaceScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					"빔 프로젝터",
					PricingStrategy.initialPlusAdditional(Money.of(10000), Money.of(5000)),
					5
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.INITIAL_PLUS_ADDITIONAL);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(10000));
			assertThat(strategy.getAdditionalPrice()).isEqualTo(Money.of(5000));
		}
		
		@Test
		@DisplayName("ONE_TIME 타입 저장 및 조회")
		void saveOneTimeType() {
			// given
			final Product product = Product.createRoomScoped(
					ProductId.of(null),
					PlaceId.of(100L),
					RoomId.of(200L),
					"1회 대여 장비",
					PricingStrategy.oneTime(Money.of(15000)),
					10
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.ONE_TIME);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(15000));
			assertThat(strategy.getAdditionalPrice()).isNull();
		}
		
		@Test
		@DisplayName("SIMPLE_STOCK 타입 저장 및 조회")
		void saveSimpleStockType() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"음료수",
					PricingStrategy.simpleStock(Money.of(2000)),
					100
			);
			
			// when
			final Product saved = repository.save(product);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.SIMPLE_STOCK);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(2000));
			assertThat(strategy.getAdditionalPrice()).isNull();
		}
		
		@Test
		@DisplayName("PricingStrategy 변경 후 저장")
		void updatePricingStrategy() {
			// given
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"상품",
					PricingStrategy.simpleStock(Money.of(1000)),
					10
			);
			final Product saved = repository.save(product);
			
			// when
			saved.updatePricingStrategy(PricingStrategy.oneTime(Money.of(5000)));
			repository.save(saved);
			
			// then
			final Optional<Product> found = repository.findById(saved.getProductId());
			assertThat(found).isPresent();
			
			final PricingStrategy strategy = found.get().getPricingStrategy();
			assertThat(strategy.getPricingType()).isEqualTo(PricingType.ONE_TIME);
			assertThat(strategy.getInitialPrice()).isEqualTo(Money.of(5000));
		}
	}

	@Nested
	@DisplayName("대규모 동시성 테스트")
	class LargeScaleConcurrencyTests {

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("100명이 재고 50개 상품을 동시 예약 시 정확히 50명만 성공")
		void hundredUsersTryToReserve50Items() throws InterruptedException {
			// given
			final int totalQuantity = 50;
			final int concurrentUsers = 100;
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"인기 상품",
					PricingStrategy.simpleStock(Money.of(10000)),
					totalQuantity
			);
			final Product saved = repository.save(product);

			final ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
			final CountDownLatch startLatch = new CountDownLatch(1);
			final CountDownLatch endLatch = new CountDownLatch(concurrentUsers);
			final AtomicInteger successCount = new AtomicInteger(0);
			final AtomicInteger failureCount = new AtomicInteger(0);
			final List<Long> responseTimes = new CopyOnWriteArrayList<>();

			// when
			for (int i = 0; i < concurrentUsers; i++) {
				executorService.submit(() -> {
					try {
						startLatch.await();
						final long startTime = System.currentTimeMillis();

						final boolean reserved = repository.reserveQuantity(saved.getProductId(), 1);

						final long endTime = System.currentTimeMillis();
						responseTimes.add(endTime - startTime);

						if (reserved) {
							successCount.incrementAndGet();
						} else {
							failureCount.incrementAndGet();
						}
					} catch (final Exception e) {
						failureCount.incrementAndGet();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await();
			executorService.shutdown();

			// then
			assertThat(successCount.get()).isEqualTo(totalQuantity);
			assertThat(failureCount.get()).isEqualTo(concurrentUsers - totalQuantity);

			final Product result = repository.findById(saved.getProductId()).orElseThrow();
			assertThat(result.getReservedQuantity()).isEqualTo(totalQuantity);
			assertThat(result.getAvailableQuantity()).isZero();

			System.out.println("=== 성능 측정 결과 ===");
			System.out.println("총 사용자: " + concurrentUsers);
			System.out.println("성공: " + successCount.get());
			System.out.println("실패: " + failureCount.get());
			printPerformanceMetrics(responseTimes);
		}

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("200명이 재고 10개 상품을 각 5개씩 동시 예약 시 정확히 2명만 성공")
		void twoHundredUsersTryToReserve10ItemsWith5Quantity() throws InterruptedException {
			// given
			final int totalQuantity = 10;
			final int concurrentUsers = 200;
			final int quantityPerUser = 5;
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"희귀 상품",
					PricingStrategy.simpleStock(Money.of(50000)),
					totalQuantity
			);
			final Product saved = repository.save(product);

			final ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
			final CountDownLatch startLatch = new CountDownLatch(1);
			final CountDownLatch endLatch = new CountDownLatch(concurrentUsers);
			final AtomicInteger successCount = new AtomicInteger(0);
			final AtomicInteger failureCount = new AtomicInteger(0);
			final List<Long> responseTimes = new CopyOnWriteArrayList<>();

			// when
			for (int i = 0; i < concurrentUsers; i++) {
				executorService.submit(() -> {
					try {
						startLatch.await();
						final long startTime = System.currentTimeMillis();

						final boolean reserved = repository.reserveQuantity(saved.getProductId(), quantityPerUser);

						final long endTime = System.currentTimeMillis();
						responseTimes.add(endTime - startTime);

						if (reserved) {
							successCount.incrementAndGet();
						} else {
							failureCount.incrementAndGet();
						}
					} catch (final Exception e) {
						failureCount.incrementAndGet();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await();
			executorService.shutdown();

			// then
			final int expectedSuccess = totalQuantity / quantityPerUser;
			assertThat(successCount.get()).isEqualTo(expectedSuccess);
			assertThat(failureCount.get()).isEqualTo(concurrentUsers - expectedSuccess);

			final Product result = repository.findById(saved.getProductId()).orElseThrow();
			assertThat(result.getReservedQuantity()).isEqualTo(totalQuantity);
			assertThat(result.getAvailableQuantity()).isZero();

			System.out.println("=== 성능 측정 결과 ===");
			System.out.println("총 사용자: " + concurrentUsers);
			System.out.println("성공: " + successCount.get());
			System.out.println("실패: " + failureCount.get());
			printPerformanceMetrics(responseTimes);
		}

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("500명이 충분한 재고 상품을 동시 예약 시 모두 성공")
		void fiveHundredUsersTryToReserveSufficientStock() throws InterruptedException {
			// given
			final int totalQuantity = 1000;
			final int concurrentUsers = 500;
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"대량 재고 상품",
					PricingStrategy.simpleStock(Money.of(5000)),
					totalQuantity
			);
			final Product saved = repository.save(product);

			final ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
			final CountDownLatch startLatch = new CountDownLatch(1);
			final CountDownLatch endLatch = new CountDownLatch(concurrentUsers);
			final AtomicInteger successCount = new AtomicInteger(0);
			final AtomicInteger failureCount = new AtomicInteger(0);
			final List<Long> responseTimes = new CopyOnWriteArrayList<>();

			// when
			for (int i = 0; i < concurrentUsers; i++) {
				executorService.submit(() -> {
					try {
						startLatch.await();
						final long startTime = System.currentTimeMillis();

						final boolean reserved = repository.reserveQuantity(saved.getProductId(), 1);

						final long endTime = System.currentTimeMillis();
						responseTimes.add(endTime - startTime);

						if (reserved) {
							successCount.incrementAndGet();
						} else {
							failureCount.incrementAndGet();
						}
					} catch (final Exception e) {
						failureCount.incrementAndGet();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await();
			executorService.shutdown();

			// then
			assertThat(successCount.get()).isEqualTo(concurrentUsers);
			assertThat(failureCount.get()).isZero();

			final Product result = repository.findById(saved.getProductId()).orElseThrow();
			assertThat(result.getReservedQuantity()).isEqualTo(concurrentUsers);
			assertThat(result.getAvailableQuantity()).isEqualTo(totalQuantity - concurrentUsers);

			System.out.println("=== 성능 측정 결과 ===");
			System.out.println("총 사용자: " + concurrentUsers);
			System.out.println("성공: " + successCount.get());
			System.out.println("실패: " + failureCount.get());
			printPerformanceMetrics(responseTimes);
		}

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("100명이 예약 후 50명이 동시 취소 시 재고 정확히 복구")
		void hundredUsersReserveThenFiftyRelease() throws InterruptedException {
			// given
			final int totalQuantity = 100;
			final int reserveUsers = 100;
			final int releaseUsers = 50;
			final Product product = Product.createReservationScoped(
					ProductId.of(null),
					"예약 취소 테스트 상품",
					PricingStrategy.simpleStock(Money.of(10000)),
					totalQuantity
			);
			final Product saved = repository.save(product);

			// 먼저 100명이 예약
			final ExecutorService reserveExecutor = Executors.newFixedThreadPool(reserveUsers);
			final CountDownLatch reserveStartLatch = new CountDownLatch(1);
			final CountDownLatch reserveEndLatch = new CountDownLatch(reserveUsers);
			final AtomicInteger reserveSuccessCount = new AtomicInteger(0);

			for (int i = 0; i < reserveUsers; i++) {
				reserveExecutor.submit(() -> {
					try {
						reserveStartLatch.await();
						final boolean reserved = repository.reserveQuantity(saved.getProductId(), 1);
						if (reserved) {
							reserveSuccessCount.incrementAndGet();
						}
					} catch (final Exception e) {
						// ignore
					} finally {
						reserveEndLatch.countDown();
					}
				});
			}

			reserveStartLatch.countDown();
			reserveEndLatch.await();
			reserveExecutor.shutdown();

			assertThat(reserveSuccessCount.get()).isEqualTo(totalQuantity);

			// 50명이 동시 취소
			final ExecutorService releaseExecutor = Executors.newFixedThreadPool(releaseUsers);
			final CountDownLatch releaseStartLatch = new CountDownLatch(1);
			final CountDownLatch releaseEndLatch = new CountDownLatch(releaseUsers);
			final AtomicInteger releaseSuccessCount = new AtomicInteger(0);
			final List<Long> responseTimes = new CopyOnWriteArrayList<>();

			for (int i = 0; i < releaseUsers; i++) {
				releaseExecutor.submit(() -> {
					try {
						releaseStartLatch.await();
						final long startTime = System.currentTimeMillis();

						final boolean released = repository.releaseQuantity(saved.getProductId(), 1);

						final long endTime = System.currentTimeMillis();
						responseTimes.add(endTime - startTime);

						if (released) {
							releaseSuccessCount.incrementAndGet();
						}
					} catch (final Exception e) {
						// ignore
					} finally {
						releaseEndLatch.countDown();
					}
				});
			}

			releaseStartLatch.countDown();
			releaseEndLatch.await();
			releaseExecutor.shutdown();

			// then
			assertThat(releaseSuccessCount.get()).isEqualTo(releaseUsers);

			final Product result = repository.findById(saved.getProductId()).orElseThrow();
			assertThat(result.getReservedQuantity()).isEqualTo(totalQuantity - releaseUsers);
			assertThat(result.getAvailableQuantity()).isEqualTo(releaseUsers);

			System.out.println("=== 예약 취소 성능 측정 결과 ===");
			System.out.println("예약 성공: " + reserveSuccessCount.get());
			System.out.println("취소 성공: " + releaseSuccessCount.get());
			printPerformanceMetrics(responseTimes);
		}

		private void printPerformanceMetrics(final List<Long> responseTimes) {
			if (responseTimes.isEmpty()) {
				System.out.println("응답 시간 데이터 없음");
				return;
			}

			final List<Long> sorted = responseTimes.stream()
					.sorted()
					.collect(Collectors.toList());

			final long p50 = sorted.get((int) (sorted.size() * 0.50));
			final long p95 = sorted.get((int) (sorted.size() * 0.95));
			final long p99 = sorted.get((int) (sorted.size() * 0.99));
			final long max = sorted.get(sorted.size() - 1);
			final double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);

			System.out.println("P50: " + p50 + "ms");
			System.out.println("P95: " + p95 + "ms");
			System.out.println("P99: " + p99 + "ms");
			System.out.println("MAX: " + max + "ms");
			System.out.println("AVG: " + String.format("%.2f", avg) + "ms");
		}
	}
}
