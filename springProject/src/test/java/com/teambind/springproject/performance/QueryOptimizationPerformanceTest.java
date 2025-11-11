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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1 쿼리 최적화 성능 비교 테스트.
 * <p>
 * EAGER fetch를 LAZY + Batch fetch로 변경하기 전후의 성능을 측정합니다.
 * <p>
 * 실제 상황을 시뮬레이션:
 * - Place: 100개
 * - Room: 각 Place당 10개 = 총 1,000개
 * - ReservationPricing: 피크 타임 5,000개
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("performance")
public class QueryOptimizationPerformanceTest {

  private static final Logger logger = LoggerFactory.getLogger(
      QueryOptimizationPerformanceTest.class);

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private PricingPolicyJpaRepository pricingPolicyRepository;

  @Autowired
  private ReservationPricingJpaRepository reservationPricingRepository;

  private Statistics statistics;

  @BeforeEach
  void setUp() {
    // Hibernate Statistics 초기화
    final SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
        .unwrap(SessionFactory.class);
    statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    logger.info("=".repeat(80));
    logger.info("Performance Test Setup Complete");
    logger.info("Hibernate Statistics Enabled");
    logger.info("=".repeat(80));
  }

  @Test
  @DisplayName("Scenario 1: Place의 Room 10개 가격 정책 조회")
  void testFetchPricingPoliciesForPlace() {
    // Given: 1개 Place, 10개 Room with PricingPolicy
    final Long placeId = 1L;
    createPricingPoliciesForPlace(placeId, 10);
    entityManager.flush();
    entityManager.clear();
    clearStatistics();

    // When: Place의 모든 PricingPolicy 조회
    final PerformanceReport report = measurePerformance(() -> {
      final List<PricingPolicyEntity> policies = pricingPolicyRepository.findAll();
      // toDomain() 호출하여 TimeRangePrices 컬렉션 접근
      policies.forEach(entity -> entity.toDomain());
    });

    // Then: 성능 측정 결과 로깅
    logReport("Scenario 1: Place Room 10개 가격 정책 조회", report);

    // EAGER 방식 예상: 1 + 10 = 11 queries
    // LAZY + Batch 예상: 1 + 1 = 2 queries
  }

  @Test
  @DisplayName("Scenario 2: Place의 예약 50개 조회")
  void testFetchReservationsForPlace() {
    // Given: 1개 Place, 5개 Room, 각 Room당 10개 예약 = 총 50개
    final Long placeId = 1L;
    createReservationsForPlace(placeId, 5, 10);
    entityManager.flush();
    entityManager.clear();
    clearStatistics();

    // When: Place의 모든 ReservationPricing 조회
    final PerformanceReport report = measurePerformance(() -> {
      final List<ReservationPricingEntity> reservations = reservationPricingRepository.findAll();
      // toDomain() 호출하여 slotPrices, productBreakdowns 컬렉션 접근
      reservations.forEach(entity -> entity.toDomain());
    });

    // Then: 성능 측정 결과 로깅
    logReport("Scenario 2: Place 예약 50개 조회", report);

    // EAGER 방식 예상: 1 + 50 + 50 = 101 queries
    // LAZY + Batch 예상: 1 + 1 + 1 = 3 queries
  }

  @Test
  @DisplayName("Scenario 3: 대량 가격 정책 100개 조회")
  void testFetchBulkPricingPolicies() {
    // Given: 10개 Place, 각 10개 Room = 100개 PricingPolicy
    IntStream.range(1, 11).forEach(placeId ->
        createPricingPoliciesForPlace((long) placeId, 10)
    );
    entityManager.flush();
    entityManager.clear();
    clearStatistics();

    // When: 전체 PricingPolicy 조회
    final PerformanceReport report = measurePerformance(() -> {
      final List<PricingPolicyEntity> policies = pricingPolicyRepository.findAll();
      policies.forEach(entity -> entity.toDomain());
    });

    // Then: 성능 측정 결과 로깅
    logReport("Scenario 3: 대량 가격 정책 100개 조회", report);

    // EAGER 방식 예상: 1 + 100 = 101 queries
    // LAZY + Batch(100) 예상: 1 + 1 = 2 queries
  }

  @Test
  @DisplayName("Scenario 4: 대량 예약 200개 조회")
  void testFetchBulkReservations() {
    // Given: 10개 Place, 각 2개 Room, 각 Room당 10개 예약 = 200개
    IntStream.range(1, 11).forEach(placeId ->
        createReservationsForPlace((long) placeId, 2, 10)
    );
    entityManager.flush();
    entityManager.clear();
    clearStatistics();

    // When: 전체 ReservationPricing 조회
    final PerformanceReport report = measurePerformance(() -> {
      final List<ReservationPricingEntity> reservations = reservationPricingRepository.findAll();
      reservations.forEach(entity -> entity.toDomain());
    });

    // Then: 성능 측정 결과 로깅
    logReport("Scenario 4: 대량 예약 200개 조회", report);

    // EAGER 방식 예상: 1 + 200 + 200 = 401 queries
    // LAZY + Batch(100) 예상: 1 + 2 + 2 = 5 queries
  }

  @Test
  @DisplayName("Scenario 5: 특정 상태 예약 조회 (PENDING)")
  void testFetchReservationsByStatus() {
    // Given: 다양한 상태의 예약 생성
    createReservationsForPlace(1L, 5, 20);
    entityManager.flush();
    entityManager.clear();
    clearStatistics();

    // When: PENDING 상태 예약만 조회
    final PerformanceReport report = measurePerformance(() -> {
      final List<ReservationPricingEntity> pendingReservations =
          reservationPricingRepository.findByStatusIn(List.of(ReservationStatus.PENDING));
      pendingReservations.forEach(entity -> entity.toDomain());
    });

    // Then: 성능 측정 결과 로깅
    logReport("Scenario 5: 특정 상태(PENDING) 예약 조회", report);
  }

  /**
   * Place에 대한 PricingPolicy 생성.
   *
   * @param placeId Place ID
   * @param roomCount Room 개수
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
   * TimeRangePrice 생성 (10개).
   */
  private List<TimeRangePriceEmbeddable> createTimeRangePrices() {
    final List<TimeRangePriceEmbeddable> prices = new ArrayList<>();

    // 평일 주간 (10:00-18:00)
    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0),
        new BigDecimal("12000")
    ));

    // 평일 야간 (18:00-22:00)
    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0),
        new BigDecimal("15000")
    ));

    // 주말 주간
    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(18, 0),
        new BigDecimal("18000")
    ));

    // 주말 야간
    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.SATURDAY, LocalTime.of(18, 0), LocalTime.of(22, 0),
        new BigDecimal("20000")
    ));

    // 일요일
    prices.add(new TimeRangePriceEmbeddable(
        DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(18, 0),
        new BigDecimal("18000")
    ));

    return prices;
  }

  /**
   * Place에 대한 ReservationPricing 생성.
   *
   * @param placeId Place ID
   * @param roomCount Room 개수
   * @param reservationsPerRoom Room당 예약 개수
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
        .add(new BigDecimal("25000")); // slot 24개 + product 5개

    return new ReservationPricingEntity(
        null, // ID는 자동 생성
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
   * 성능 측정 유틸리티.
   */
  private PerformanceReport measurePerformance(final Runnable operation) {
    clearStatistics();

    final long startTime = System.currentTimeMillis();
    operation.run();
    final long duration = System.currentTimeMillis() - startTime;

    return new PerformanceReport(
        statistics.getQueryExecutionCount(),
        statistics.getCollectionFetchCount(),
        duration
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
   * 성능 측정 결과 로깅.
   */
  private void logReport(final String scenarioName, final PerformanceReport report) {
    logger.info("=".repeat(80));
    logger.info("[{}]", scenarioName);
    logger.info("Total Queries: {}", report.queryCount());
    logger.info("Collection Fetches: {}", report.collectionFetchCount());
    logger.info("Execution Time: {}ms", report.duration());
    logger.info("=".repeat(80));
  }

  /**
   * 성능 측정 결과.
   */
  private record PerformanceReport(
      long queryCount,
      long collectionFetchCount,
      long duration
  ) {

  }
}
