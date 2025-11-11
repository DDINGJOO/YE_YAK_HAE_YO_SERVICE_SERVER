package com.teambind.springproject.performance;

import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyEntity;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PricingPolicyJpaRepository;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.PlaceIdEmbeddable;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.RoomIdEmbeddable;
import com.teambind.springproject.adapter.out.persistence.pricingpolicy.TimeRangePriceEmbeddable;
import com.teambind.springproject.adapter.out.persistence.reservationpricing.ProductPriceBreakdownEmbeddable;
import com.teambind.springproject.adapter.out.persistence.reservationpricing.ReservationPricingEntity;
import com.teambind.springproject.adapter.out.persistence.reservationpricing.ReservationPricingJpaRepository;
import com.teambind.springproject.domain.product.PricingType;
import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.ReservationStatus;
import com.teambind.springproject.domain.shared.TimeSlot;
import com.teambind.springproject.performance.support.AggregatedPerformanceReport;
import com.teambind.springproject.performance.support.LargeScaleTestBase;
import com.teambind.springproject.performance.support.PerformanceReportAggregator;
import com.teambind.springproject.performance.support.RandomDataSelector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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

/**
 * 대규모 N+1 쿼리 최적화 성능 테스트.
 * <p>
 * EAGER fetch를 LAZY + Batch fetch로 변경하기 전후의 성능을 측정합니다.
 * <p>
 * 대규모 데이터 시뮬레이션 (기본 테스트 대비 20배):
 * - Place: 200개
 * - Room: 400개
 * - PricingPolicy: 2,000개
 * - ReservationPricing: 4,000개
 * <p>
 * 측정 방식: 50번 랜덤 위치 조회 후 평균 계산
 */
@Transactional
@Tag("large-scale")
@Tag("performance")
@DisplayName("쿼리 최적화 성능 테스트 (Large Scale)")
public class QueryOptimizationLargeScalePerformanceTest extends LargeScaleTestBase {

  private static final Logger logger = LoggerFactory.getLogger(
      QueryOptimizationLargeScalePerformanceTest.class);

  private static final int LARGE_SCALE_MULTIPLIER = 20;
  private static final int RANDOM_QUERY_COUNT = 50;

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private PricingPolicyJpaRepository pricingPolicyRepository;

  @Autowired
  private ReservationPricingJpaRepository reservationPricingRepository;

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
    logger.info("Large Scale Performance Test Setup Complete");
    logger.info("Multiplier: {}x", LARGE_SCALE_MULTIPLIER);
    logger.info("Random Query Count: {}", RANDOM_QUERY_COUNT);
    logger.info("=".repeat(80));
  }

  @Test
  @DisplayName("Scenario 1: 대규모 PricingPolicy 랜덤 조회 (2,000개)")
  void testLargeScalePricingPolicyRandomAccess() {
    // Given: 200개 Place, 각 10개 Room = 2,000개 PricingPolicy
    logger.info("Creating 2,000 PricingPolicy entities...");
    IntStream.rangeClosed(1, 200).forEach(placeId ->
        createPricingPoliciesForPlace((long) placeId, 10)
    );
    entityManager.flush();
    entityManager.clear();
    logger.info("Data creation completed.");

    // When: 100번 랜덤 조회
    final AggregatedPerformanceReport report = measureRandomAccess(
        () -> {
          final List<PricingPolicyEntity> allPolicies = pricingPolicyRepository.findAll();
          final PricingPolicyEntity randomPolicy = randomSelector.randomElement(allPolicies);
          // toDomain() 호출하여 컬렉션 접근
          randomPolicy.toDomain();
        }
    );

    // Then: 성능 측정 결과 로깅
    logAggregatedReport("Scenario 1: 대규모 PricingPolicy 랜덤 조회", report);

    // Expected:
    // - EAGER: 각 조회마다 N+1 발생 → 평균 5,000+ queries
    // - LAZY + Batch: 1 + 1 = 2 queries per access
  }

  @Test
  @DisplayName("Scenario 2: 대규모 ReservationPricing 랜덤 조회 (4,000개)")
  void testLargeScaleReservationPricingRandomAccess() {
    // Given: 200개 Place, 각 2개 Room, 각 Room당 10개 예약 = 4,000개
    logger.info("Creating 4,000 ReservationPricing entities...");
    IntStream.rangeClosed(1, 200).forEach(placeId ->
        createReservationsForPlace((long) placeId, 2, 10)
    );
    entityManager.flush();
    entityManager.clear();
    logger.info("Data creation completed.");

    // When: 100번 랜덤 조회
    final AggregatedPerformanceReport report = measureRandomAccess(
        () -> {
          final List<ReservationPricingEntity> allReservations =
              reservationPricingRepository.findAll();
          final ReservationPricingEntity randomReservation =
              randomSelector.randomElement(allReservations);
          // toDomain() 호출하여 컬렉션 접근
          randomReservation.toDomain();
        }
    );

    // Then: 성능 측정 결과 로깅
    logAggregatedReport("Scenario 2: 대규모 ReservationPricing 랜덤 조회", report);

    // Expected:
    // - EAGER: 각 조회마다 N+1 발생 → 평균 10,000+ queries
    // - LAZY + Batch: 1 + 2 = 3 queries per access
  }

  @Test
  @DisplayName("Scenario 3: 대규모 PricingPolicy 전체 조회")
  void testLargeScalePricingPolicyFullScan() {
    // Given: 200개 Place, 각 10개 Room = 2,000개 PricingPolicy
    logger.info("Creating 2,000 PricingPolicy entities...");
    IntStream.rangeClosed(1, 200).forEach(placeId ->
        createPricingPoliciesForPlace((long) placeId, 10)
    );
    entityManager.flush();
    entityManager.clear();
    logger.info("Data creation completed.");

    // When: 100번 전체 조회 (캐시 없이)
    final AggregatedPerformanceReport report = measureRandomAccess(
        () -> {
          final List<PricingPolicyEntity> policies = pricingPolicyRepository.findAll();
          // 모든 policy의 컬렉션 접근
          policies.forEach(PricingPolicyEntity::toDomain);
        }
    );

    // Then: 성능 측정 결과 로깅
    logAggregatedReport("Scenario 3: 대규모 PricingPolicy 전체 조회", report);
  }

  @Test
  @DisplayName("Scenario 4: 특정 상태의 ReservationPricing 조회")
  void testLargeScaleReservationPricingByStatus() {
    // Given: 200개 Place, 각 2개 Room, 각 Room당 10개 예약 = 4,000개
    logger.info("Creating 4,000 ReservationPricing entities...");
    IntStream.rangeClosed(1, 200).forEach(placeId ->
        createReservationsForPlace((long) placeId, 2, 10)
    );
    entityManager.flush();
    entityManager.clear();
    logger.info("Data creation completed.");

    // When: 100번 PENDING 상태 조회
    final AggregatedPerformanceReport report = measureRandomAccess(
        () -> {
          final List<ReservationPricingEntity> pendingReservations =
              reservationPricingRepository.findByStatusIn(List.of(ReservationStatus.PENDING));
          // 모든 reservation의 컬렉션 접근
          pendingReservations.forEach(ReservationPricingEntity::toDomain);
        }
    );

    // Then: 성능 측정 결과 로깅
    logAggregatedReport("Scenario 4: 특정 상태의 ReservationPricing 조회", report);
  }

  /**
   * 랜덤 접근 성능 측정.
   * <p>
   * 100번 반복 실행 후 통계 집계.
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
      final PricingPolicyEntity policy = createPricingPolicy(placeId, roomId);
      pricingPolicyRepository.save(policy);
    });
  }

  /**
   * PricingPolicy Entity 생성.
   */
  private PricingPolicyEntity createPricingPolicy(final Long placeId, final Long roomId) {
    final List<TimeRangePriceEmbeddable> timeRangePrices = createTimeRangePrices();

    return new PricingPolicyEntity(
        new RoomIdEmbeddable(roomId),
        new PlaceIdEmbeddable(placeId),
        TimeSlot.HOUR,
        new BigDecimal("10000"),
        timeRangePrices
    );
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

    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.SATURDAY, LocalTime.of(18, 0), LocalTime.of(22, 0),
        new BigDecimal("20000")
    ));

    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(18, 0),
        new BigDecimal("18000")
    ));

    return prices;
  }

  /**
   * Place에 대한 ReservationPricing 생성.
   */
  private void createReservationsForPlace(
      final Long placeId,
      final int roomCount,
      final int reservationsPerRoom) {

    IntStream.range(1, roomCount + 1).forEach(roomIdx -> {
      final Long roomId = placeId * 100 + roomIdx;

      IntStream.range(1, reservationsPerRoom + 1).forEach(resIdx -> {
        final ReservationPricingEntity reservation = createReservationPricing(
            placeId, roomId, resIdx);
        reservationPricingRepository.save(reservation);
      });
    });
  }

  /**
   * ReservationPricing Entity 생성.
   */
  private ReservationPricingEntity createReservationPricing(
      final Long placeId,
      final Long roomId,
      final int index) {

    // SlotPrices: 24개 (하루 24시간)
    final Map<LocalDateTime, BigDecimal> slotPrices = new HashMap<>();
    final LocalDateTime baseDate = LocalDateTime.now().plusDays(index);

    IntStream.range(0, 24).forEach(hour -> {
      final LocalDateTime slotTime = baseDate.withHour(hour).withMinute(0).withSecond(0);
      slotPrices.put(slotTime, new BigDecimal("10000"));
    });

    // ProductBreakdowns: 5개
    final List<ProductPriceBreakdownEmbeddable> productBreakdowns = new ArrayList<>();
    IntStream.range(1, 6).forEach(productIdx -> {
      productBreakdowns.add(new ProductPriceBreakdownEmbeddable(
          (long) productIdx,
          "Product " + productIdx,
          1,
          new BigDecimal("5000"),
          new BigDecimal("5000"),
          PricingType.SIMPLE_STOCK
      ));
    });

    final BigDecimal totalPrice = new BigDecimal("240000")
        .add(new BigDecimal("25000"));

    return new ReservationPricingEntity(
        null,
        roomId,
        placeId,
        ReservationStatus.PENDING,
        TimeSlot.HOUR,
        slotPrices,
        productBreakdowns,
        totalPrice,
        LocalDateTime.now(),
        LocalDateTime.now().plusMinutes(15)
    );
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
