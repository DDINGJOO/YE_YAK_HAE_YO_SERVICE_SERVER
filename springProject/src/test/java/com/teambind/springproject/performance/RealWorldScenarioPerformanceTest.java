package com.teambind.springproject.performance;

import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PlaceIdEmbeddable;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyEntity;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyJpaRepository;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.RoomIdEmbeddable;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.TimeRangePriceEmbeddable;
import com.teambind.springproject.adapter.out.persistence.product.ProductEntity;
import com.teambind.springproject.adapter.out.persistence.product.ProductJpaRepository;
import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.ReservationPricingResponse;
import com.teambind.springproject.application.port.in.CalculateReservationPriceUseCase;
import com.teambind.springproject.application.port.in.CreateReservationUseCase;
import com.teambind.springproject.application.service.reservationpricing.ReservationPricingService;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.TimeSlot;
import com.teambind.springproject.performance.support.AggregatedPerformanceReport;
import com.teambind.springproject.performance.support.LargeScaleTestBase;
import com.teambind.springproject.performance.support.PerformanceReportAggregator;
import com.teambind.springproject.performance.support.RandomDataSelector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 실제 서비스 시나리오 기반 대규모 성능 테스트.
 *
 * 테스트 규모:
 *
 *   Places: 2,000개
 *   Rooms: 10,000개 (Place당 5개)
 *   Products: 10,000개
 *   Reservations: 생성 및 취소 시뮬레이션
 *
 *
 * 측정 목표:
 *
 *   N+1 쿼리 최적화 효과 확인
 *   대량 상품 포함 시나리오 성능
 *   배치 처리 성능
 *   복잡한 조회 시나리오 성능
 *
 */
@Transactional
@Tag("integration")
@Tag("performance")
@DisplayName("실제 서비스 시나리오 대규모 성능 테스트")
public class RealWorldScenarioPerformanceTest extends LargeScaleTestBase {

	private static final Logger logger = LoggerFactory.getLogger(
			RealWorldScenarioPerformanceTest.class);

	private static final int RANDOM_QUERY_COUNT = 50;

	// 대규모 데이터 규모 (기존 10배)
	private static final int PLACE_COUNT = 2000;
	private static final int ROOMS_PER_PLACE = 5;
	private static final int TOTAL_ROOMS = PLACE_COUNT * ROOMS_PER_PLACE; // 10,000
	private static final int PRODUCTS_PER_PLACE = 5;
	private static final int TOTAL_PRODUCTS = PLACE_COUNT * PRODUCTS_PER_PLACE; // 10,000

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private CreateReservationUseCase createReservationUseCase;

	@Autowired
	private CalculateReservationPriceUseCase calculateReservationPriceUseCase;

	@Autowired
	private ReservationPricingService reservationPricingService;

	@Autowired
	private PricingPolicyJpaRepository pricingPolicyRepository;

	@Autowired
	private ProductJpaRepository productRepository;

	private Statistics statistics;
	private RandomDataSelector randomSelector;

	@BeforeEach
	void setUp() {
		final SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
				.unwrap(SessionFactory.class);
		statistics = sessionFactory.getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();

		randomSelector = new RandomDataSelector(42L);

		logger.info("=".repeat(80));
		logger.info("Real World Scenario Performance Test Setup Complete");
		logger.info("Data Scale: {} Places, {} Rooms, {} Products",
				PLACE_COUNT, TOTAL_ROOMS, TOTAL_PRODUCTS);
		logger.info("Random Query Count: {}", RANDOM_QUERY_COUNT);
		logger.info("=".repeat(80));
	}

	@Test
	@DisplayName("Scenario 1: 대량 상품 포함 가격 계산 (100개 상품)")
	void testMassiveProductPriceCalculation() {
		// Given: 1개 Place, 1개 Room, 100개 Product
		logger.info("Creating test data: 1 Place, 1 Room, 100 Products...");
		final Long placeId = 1L;
		final Long roomId = 101L;

		createPricingPolicy(placeId, roomId);

		final List<Long> productIds = new ArrayList<>();
		IntStream.rangeClosed(1, 100).forEach(idx -> {
			final Long productId = createProduct(
					(long) idx,
					placeId,
					roomId,
					"Product " + idx,
					ProductScope.PLACE,
					new BigDecimal("5000"),
					100
			);
			productIds.add(productId);
		});

		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");

		// When: 50번 가격 미리보기 요청 (100개 상품 포함)
		final AggregatedPerformanceReport report = measureRandomAccess(
				() -> {
					final List<ProductRequest> productRequests = productIds.stream()
							.map(id -> new ProductRequest(id, 1))
							.toList();

					final List<LocalDateTime> timeSlots = generateTimeSlots(
							LocalDateTime.now().plusDays(1), 3
					);

					final CreateReservationRequest request = new CreateReservationRequest(
							roomId,
							timeSlots,
							productRequests
					);

					calculateReservationPriceUseCase.calculatePrice(request);
				}
		);

		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 1: 대량 상품 포함 가격 계산 (100 products)", report);

		// Expected:
		// - N+1 최적화 전: 1 (PricingPolicy) + 100 (각 Product) = 101 queries
		// - N+1 최적화 후: 1 (PricingPolicy) + 1 (Batch Product) = 2 queries
	}

	@Test
	@DisplayName("Scenario 2: 배치 예약 취소 - releaseProducts() N+1 최적화 검증")
	void testBatchReservationCancellation() {
		// Given: 10개 예약 생성 (각 예약당 10개 상품)
		logger.info("Creating test data: 10 Reservations with 10 products each...");
		final Long placeId = 1L;
		final Long roomId = 101L;

		createPricingPolicy(placeId, roomId);

		final List<Long> productIds = new ArrayList<>();
		IntStream.rangeClosed(1, 10).forEach(idx -> {
			final Long productId = createProduct(
					(long) idx,
					placeId,
					roomId,
					"Product " + idx,
					ProductScope.ROOM,
					new BigDecimal("5000"),
					1000 // 충분한 재고
			);
			productIds.add(productId);
		});

		// 10개 예약 생성
		final List<Long> reservationIds = new ArrayList<>();
		IntStream.rangeClosed(1, 10).forEach(idx -> {
			final List<ProductRequest> productRequests = productIds.stream()
					.map(id -> new ProductRequest(id, 1))
					.toList();

			final List<LocalDateTime> timeSlots = generateTimeSlots(
					LocalDateTime.now().plusDays(idx), 2
			);

			final CreateReservationRequest request = new CreateReservationRequest(
					roomId,
					timeSlots,
					productRequests
			);

			final ReservationPricingResponse response = createReservationUseCase.createReservation(request);
			reservationIds.add(response.reservationId());
		});

		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed: {} reservations", reservationIds.size());

		// When: 10개 예약을 순차적으로 취소 (각 취소마다 releaseProducts() 호출)
		final AggregatedPerformanceReport report = measureBatchCancellation(reservationIds);

		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 2: 배치 예약 취소 (10 reservations, 10 products each)", report);

		// Expected:
		// - N+1 최적화 전: 10 * (1 + 10) = 110 queries (각 예약마다 1 + 상품 10개)
		// - N+1 최적화 후: 10 * 2 = 20 queries (각 예약마다 1 + 배치 조회 1)
	}

	@Test
	@DisplayName("Scenario 3: 극대 규모 상품 가격 계산 (500개 상품)")
	void testExtremeScaleProductPriceCalculation() {
		// Given: 1개 Place, 1개 Room, 500개 Product
		logger.info("Creating test data: 1 Place, 1 Room, 500 Products...");
		final Long placeId = 1L;
		final Long roomId = 101L;

		createPricingPolicy(placeId, roomId);

		final List<Long> productIds = new ArrayList<>();
		IntStream.rangeClosed(1, 500).forEach(idx -> {
			final Long productId = createProduct(
					null, // snowflake ID
					placeId,
					roomId,
					"Product " + idx,
					ProductScope.PLACE,
					new BigDecimal("5000"),
					100
			);
			productIds.add(productId);
		});

		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");

		// When: 20번 가격 미리보기 요청 (500개 상품 포함)
		final PerformanceReportAggregator aggregator = new PerformanceReportAggregator();

		logger.info("Starting {} queries with 500 products each...", 20);

		for (int i = 0; i < 20; i++) {
			clearStatistics();

			final List<ProductRequest> productRequests = productIds.stream()
					.map(id -> new ProductRequest(id, 1))
					.toList();

			final List<LocalDateTime> timeSlots = generateTimeSlots(
					LocalDateTime.now().plusDays(1), 3
			);

			final CreateReservationRequest request = new CreateReservationRequest(
					roomId,
					timeSlots,
					productRequests
			);

			final long startTime = System.currentTimeMillis();
			calculateReservationPriceUseCase.calculatePrice(request);
			final long duration = System.currentTimeMillis() - startTime;

			aggregator.addMeasurement(statistics.getQueryExecutionCount(), duration);

			if ((i + 1) % 10 == 0) {
				logger.info("Progress: {}/{} queries completed", i + 1, 20);
			}
		}

		final AggregatedPerformanceReport report = aggregator.aggregate();

		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 3: 극대 규모 상품 가격 계산 (500 products)", report);

		// Expected:
		// - N+1 최적화 전: 1 (PricingPolicy) + 500 (각 Product) = 501 queries
		// - N+1 최적화 후: 1 (PricingPolicy) + 1 (Batch Product) = 2 queries
	}

	@Test
	@DisplayName("Scenario 4: 대규모 환경 - 1000개 Place 랜덤 가격 조회")
	void testLargeScalePriceQuery() {
		// Given: 1000개 Place, 각 1개 Room, 각 5개 Product = 5,000개 Product
		logger.info("Creating large scale dataset: 1000 Places, 1000 Rooms, 5000 Products...");

		final java.util.Map<Long, List<Long>> placeProductIds = new java.util.HashMap<>();

		IntStream.rangeClosed(1, 1000).forEach(placeId -> {
			final Long roomId = placeId * 100L;
			createPricingPolicy((long) placeId, roomId);

			final List<Long> productIds = new ArrayList<>();
			IntStream.rangeClosed(1, 5).forEach(productIdx -> {
				final Long generatedId = createProduct(
						null,
						(long) placeId,
						roomId,
						"Product for Place " + placeId + "-" + productIdx,
						ProductScope.PLACE,
						new BigDecimal("5000"),
						100
				);
				productIds.add(generatedId);
			});
			placeProductIds.put((long) placeId, productIds);

			if (placeId % 100 == 0) {
				logger.info("Progress: {}/1000 places created", placeId);
			}
		});

		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");

		// When: 50번 랜덤 가격 미리보기 요청 (5개 상품 포함)
		final AggregatedPerformanceReport report = measureRandomAccess(
				() -> {
					final int randomPlaceIdx = (int) randomSelector.randomLong(1, 1001);
					final Long placeId = (long) randomPlaceIdx;
					final Long roomId = placeId * 100L;

					final List<Long> productIds = placeProductIds.get(placeId);
					final List<ProductRequest> productRequests = productIds.stream()
							.map(id -> new ProductRequest(id, 1))
							.toList();

					final List<LocalDateTime> timeSlots = generateTimeSlots(
							LocalDateTime.now().plusDays(1), 3
					);

					final CreateReservationRequest request = new CreateReservationRequest(
							roomId,
							timeSlots,
							productRequests
					);

					calculateReservationPriceUseCase.calculatePrice(request);
				}
		);

		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 4: 대규모 환경 (1000 places, 5000 products)", report);
	}

	/**
	 * 배치 예약 취소 성능 측정.
	 */
	private AggregatedPerformanceReport measureBatchCancellation(final List<Long> reservationIds) {
		final PerformanceReportAggregator aggregator = new PerformanceReportAggregator();

		logger.info("Starting batch cancellation of {} reservations...", reservationIds.size());

		for (final Long reservationId : reservationIds) {
			clearStatistics();

			final long startTime = System.currentTimeMillis();
			reservationPricingService.cancelReservation(reservationId);
			final long duration = System.currentTimeMillis() - startTime;

			aggregator.addMeasurement(statistics.getQueryExecutionCount(), duration);
		}

		logger.info("Batch cancellation completed.");
		return aggregator.aggregate();
	}

	/**
	 * 랜덤 접근 성능 측정.
	 */
	private AggregatedPerformanceReport measureRandomAccess(final Runnable operation) {
		final PerformanceReportAggregator aggregator = new PerformanceReportAggregator();

		logger.info("Starting {} random queries...", RANDOM_QUERY_COUNT);

		for (int i = 0; i < RANDOM_QUERY_COUNT; i++) {
			clearStatistics();

			final long startTime = System.currentTimeMillis();
			operation.run();
			final long duration = System.currentTimeMillis() - startTime;

			aggregator.addMeasurement(statistics.getQueryExecutionCount(), duration);

			if ((i + 1) % 20 == 0) {
				logger.info("Progress: {}/{} queries completed", i + 1, RANDOM_QUERY_COUNT);
			}
		}

		logger.info("All {} queries completed.", RANDOM_QUERY_COUNT);
		return aggregator.aggregate();
	}

	/**
	 * PricingPolicy 생성.
	 */
	private void createPricingPolicy(final Long placeId, final Long roomId) {
		final List<TimeRangePriceEmbeddable> timeRangePrices = createTimeRangePrices();

		final PricingPolicyEntity policy = new PricingPolicyEntity(
				new RoomIdEmbeddable(roomId),
				new PlaceIdEmbeddable(placeId),
				TimeSlot.HOUR,
				new BigDecimal("10000"),
				timeRangePrices
		);

		pricingPolicyRepository.save(policy);
	}

	/**
	 * TimeRangePrice 생성.
	 */
	private List<TimeRangePriceEmbeddable> createTimeRangePrices() {
		final List<TimeRangePriceEmbeddable> prices = new ArrayList<>();

		prices.add(new TimeRangePriceEmbeddable(
				DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0),
				new BigDecimal("12000")
		));

		prices.add(new TimeRangePriceEmbeddable(
				DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(18, 0),
				new BigDecimal("18000")
		));

		return prices;
	}

	/**
	 * Product 생성.
	 */
	private Long createProduct(
			final Long productId,
			final Long placeId,
			final Long roomId,
			final String name,
			final ProductScope scope,
			final BigDecimal price,
			final int quantity) {

		final ProductEntity product = ProductEntity.createForTest(
				productId,
				placeId,
				scope == ProductScope.ROOM || scope == ProductScope.RESERVATION ? roomId : null,
				name,
				scope,
				price,
				quantity
		);

		final ProductEntity saved = productRepository.save(product);
		return saved.getProductId();
	}

	/**
	 * 시간대 생성.
	 */
	private List<LocalDateTime> generateTimeSlots(
			final LocalDateTime start,
			final int slotCount) {

		final List<LocalDateTime> slots = new ArrayList<>();
		for (int i = 0; i < slotCount; i++) {
			slots.add(start.plusHours(i));
		}
		return slots;
	}

	/**
	 * Statistics 초기화.
	 */
	private void clearStatistics() {
		statistics.clear();
		entityManager.clear();
	}

	/**
	 * 집계된 성능 측정 결과 로깅.
	 */
	private void logAggregatedReport(
			final String scenarioName,
			final AggregatedPerformanceReport report) {
		logger.info("=".repeat(80));
		logger.info("[{}]", scenarioName);
		logger.info("");
		logger.info(report.toFormattedString());
		logger.info("=".repeat(80));
	}
}
