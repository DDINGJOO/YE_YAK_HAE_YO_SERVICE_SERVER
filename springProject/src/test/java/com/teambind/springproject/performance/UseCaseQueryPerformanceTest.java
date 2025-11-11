package com.teambind.springproject.performance;

import com.teambind.springproject.adapter.out.persistence.pricingpolicy.*;
import com.teambind.springproject.adapter.out.persistence.product.ProductEntity;
import com.teambind.springproject.adapter.out.persistence.product.ProductJpaRepository;
import com.teambind.springproject.application.dto.request.CreateReservationRequest;
import com.teambind.springproject.application.dto.request.ProductAvailabilityRequest;
import com.teambind.springproject.application.dto.request.ProductRequest;
import com.teambind.springproject.application.dto.response.PricePreviewResponse;
import com.teambind.springproject.application.dto.response.ProductAvailabilityResponse;
import com.teambind.springproject.application.port.in.CalculateReservationPriceUseCase;
import com.teambind.springproject.application.port.in.GetPricingPolicyUseCase;
import com.teambind.springproject.application.port.in.QueryProductAvailabilityUseCase;
import com.teambind.springproject.domain.pricingpolicy.PricingPolicy;
import com.teambind.springproject.domain.product.vo.ProductScope;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.RoomId;
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
 * Use Case 레벨 조회 성능 테스트.
 * <p>
 * 실제 비즈니스 Use Case의 종단간 조회 성능을 측정합니다.
 * <p>
 * 측정 대상 Use Case:
 * 1. GetPricingPolicyUseCase - 가격 정책 조회
 * 2. CalculateReservationPriceUseCase - 가격 미리보기 (다수 상품 조회)
 * 3. QueryProductAvailabilityUseCase - 상품 가용성 조회
 * <p>
 * 성능 최적화 포인트:
 * - PricePreviewService.fetchProducts(): N+1 문제
 * - ProductRepositoryAdapter.findAccessibleProducts(): 복잡한 조회
 * - ProductAvailabilityService: 각 상품별 재고 계산
 */
@Transactional
@Tag("large-scale")
@Tag("performance")
@DisplayName("Use Case 조회 성능 테스트")
public class UseCaseQueryPerformanceTest extends LargeScaleTestBase {
	
	private static final Logger logger = LoggerFactory.getLogger(
			UseCaseQueryPerformanceTest.class);
	
	private static final int RANDOM_QUERY_COUNT = 50;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private GetPricingPolicyUseCase getPricingPolicyUseCase;
	
	@Autowired
	private CalculateReservationPriceUseCase calculateReservationPriceUseCase;
	
	@Autowired
	private QueryProductAvailabilityUseCase queryProductAvailabilityUseCase;
	
	@Autowired
	private PricingPolicyJpaRepository pricingPolicyRepository;
	
	@Autowired
	private ProductJpaRepository productRepository;
	
	private Statistics statistics;
	private RandomDataSelector randomSelector;
	
	@BeforeEach
	void setUp() {
		// Hibernate Statistics 초기화
		final SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
				.unwrap(SessionFactory.class);
		statistics = sessionFactory.getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		
		// Random Selector 초기화 (재현 가능하도록 seed 고정)
		randomSelector = new RandomDataSelector(42L);
		
		logger.info("=".repeat(80));
		logger.info("Use Case Query Performance Test Setup Complete");
		logger.info("Random Query Count: {}", RANDOM_QUERY_COUNT);
		logger.info("=".repeat(80));
	}
	
	@Test
	@DisplayName("Scenario 1: GetPricingPolicy Use Case - 100개 정책 중 랜덤 조회")
	void testGetPricingPolicyUseCase() {
		// Given: 100개 PricingPolicy 생성
		logger.info("Creating 100 PricingPolicy entities...");
		IntStream.rangeClosed(1, 100).forEach(placeId ->
				createPricingPoliciesForPlace((long) placeId, 1)
		);
		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");
		
		// When: 50번 랜덤 조회
		final AggregatedPerformanceReport report = measureRandomAccess(
				() -> {
					final List<PricingPolicyEntity> allPolicies = pricingPolicyRepository.findAll();
					final PricingPolicyEntity randomPolicy = randomSelector.randomElement(allPolicies);
					final RoomId roomId = RoomId.of(randomPolicy.getRoomId().getValue());
					
					// Use Case 호출
					final PricingPolicy policy = getPricingPolicyUseCase.getPolicy(roomId);
					
					// 컬렉션 접근 (TimeRangePrices)
					policy.getTimeRangePrices().isEmpty();
				}
		);
		
		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 1: GetPricingPolicy Use Case", report);
		
		// Expected:
		// - EAGER: 각 조회마다 N+1 발생 → 평균 100+ queries
		// - LAZY + Batch: 1 + 1 = 2 queries per access
	}
	
	@Test
	@DisplayName("Scenario 2: PricePreview Use Case - 10개 상품 포함 가격 계산")
	void testPricePreviewUseCaseWithMultipleProducts() {
		// Given: 1개 Place, 1개 Room, 10개 Product
		logger.info("Creating test data: 1 Place, 1 Room, 10 Products...");
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
					ProductScope.PLACE,
					new BigDecimal("5000"),
					100
			);
			productIds.add(productId);
		});
		
		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");
		
		// When: 50번 가격 미리보기 요청 (10개 상품 포함)
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
					
					// Use Case 호출
					final PricePreviewResponse response = calculateReservationPriceUseCase
							.calculatePrice(request);
					
					// 결과 접근
					response.totalPrice();
				}
		);
		
		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 2: PricePreview Use Case (10 products)", report);
		
		// Expected:
		// - 현재 구현: 1 (PricingPolicy) + 10 (각 Product) = 11 queries per access
		// - 최적화 후: 1 (PricingPolicy) + 1 (Batch Product) = 2 queries per access
	}
	
	@Test
	@DisplayName("Scenario 3: ProductAvailability Use Case - 20개 상품 가용성 조회")
	void testProductAvailabilityUseCaseWithMultipleProducts() {
		// Given: 1개 Place, 1개 Room, 20개 Product
		logger.info("Creating test data: 1 Place, 1 Room, 20 Products...");
		final Long placeId = 1L;
		final Long roomId = 101L;
		
		createPricingPolicy(placeId, roomId);
		
		// PLACE, ROOM, RESERVATION scope별로 분산
		IntStream.rangeClosed(1, 20).forEach(idx -> {
			final ProductScope scope;
			if (idx <= 10) {
				scope = ProductScope.PLACE;
			} else if (idx <= 15) {
				scope = ProductScope.ROOM;
			} else {
				scope = ProductScope.RESERVATION;
			}
			
			createProduct(
					(long) idx,
					placeId,
					roomId,
					"Product " + idx,
					scope,
					new BigDecimal("5000"),
					100
			);
		});
		
		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");
		
		// When: 50번 상품 가용성 조회
		final AggregatedPerformanceReport report = measureRandomAccess(
				() -> {
					final List<LocalDateTime> timeSlots = generateTimeSlots(
							LocalDateTime.now().plusDays(1), 3
					);
					
					final ProductAvailabilityRequest request = new ProductAvailabilityRequest(
							roomId,
							placeId,
							timeSlots
					);
					
					// Use Case 호출
					final ProductAvailabilityResponse response = queryProductAvailabilityUseCase
							.queryAvailability(request);
					
					// 결과 접근
					response.availableProducts().size();
				}
		);
		
		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 3: ProductAvailability Use Case (20 products)", report);
		
		// Expected:
		// - findAccessibleProducts: 1 query
		// - findAllowedProductIdsByRoomId: 1 query
		// - 각 Product별 재고 계산: 20 queries
		// - Total: 22 queries per access
	}
	
	@Test
	@DisplayName("Scenario 4: 대규모 데이터셋 - 200개 Place 환경에서 조회")
	void testLargeScaleDatasetQuery() {
		// Given: 200개 Place, 각 1개 Room, 각 5개 Product = 1,000개 Product
		logger.info("Creating large scale dataset: 200 Places, 200 Rooms, 1000 Products...");
		
		// Place별 생성된 Product ID를 저장 (snowflake generator로 생성된 실제 ID)
		final java.util.Map<Long, List<Long>> placeProductIds = new java.util.HashMap<>();
		
		IntStream.rangeClosed(1, 200).forEach(placeId -> {
			final Long roomId = placeId * 100L;
			createPricingPolicy((long) placeId, roomId);
			
			final List<Long> productIds = new ArrayList<>();
			IntStream.rangeClosed(1, 5).forEach(productIdx -> {
				final Long generatedId = createProduct(
						null, // snowflake generator가 ID 생성
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
		});
		
		entityManager.flush();
		entityManager.clear();
		logger.info("Data creation completed.");
		
		// When: 50번 랜덤 가격 미리보기 요청 (5개 상품 포함)
		final AggregatedPerformanceReport report = measureRandomAccess(
				() -> {
					// 랜덤 Place 선택
					final int randomPlaceIdx = (int) randomSelector.randomLong(1, 201);
					final Long placeId = (long) randomPlaceIdx;
					final Long roomId = placeId * 100L;
					
					// 해당 Place의 실제 생성된 상품 ID 사용
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
					
					// Use Case 호출
					final PricePreviewResponse response = calculateReservationPriceUseCase
							.calculatePrice(request);
					
					response.totalPrice();
				}
		);
		
		// Then: 성능 측정 결과 로깅
		logAggregatedReport("Scenario 4: Large Scale Dataset Query (1000 products)", report);
	}
	
	/**
	 * 랜덤 접근 성능 측정.
	 *
	 * @param operation 측정할 작업
	 * @return 집계된 성능 리포트
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
	 * Place에 대한 PricingPolicy 생성.
	 */
	private void createPricingPoliciesForPlace(final Long placeId, final int roomCount) {
		IntStream.range(1, roomCount + 1).forEach(roomIdx -> {
			final Long roomId = placeId * 100 + roomIdx;
			createPricingPolicy(placeId, roomId);
		});
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
				DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0),
				new BigDecimal("15000")
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
