# 성능 측정 및 비교 가이드

## 목적

N+1 쿼리 최적화 전후의 성능을 객관적으로 측정하고 비교하기 위한 체계적인 테스트 방법론

---

## 측정 대상 메트릭

### 1. 쿼리 레벨 메트릭

**핵심 지표:**
- 쿼리 실행 횟수 (Query Count)
- 쿼리 실행 시간 (Execution Time)
- 데이터베이스 I/O 량
- 네트워크 왕복 횟수 (Round Trips)

**목표:**
- 쿼리 수: 70% 감소
- 실행 시간: 60% 단축
- 메모리 사용량: 40% 감소

### 2. 애플리케이션 레벨 메트릭

**핵심 지표:**
- API 응답 시간 (P50, P95, P99)
- 처리량 (Throughput - RPS)
- 메모리 힙 사용량
- GC 빈도 및 시간

**목표:**
- P95 응답 시간: 200ms → 80ms
- 처리량: 현재 대비 150% 향상
- 메모리 사용량: 안정화

### 3. 데이터베이스 레벨 메트릭

**핵심 지표:**
- Connection Pool 사용률
- Database CPU 사용률
- Slow Query 발생 건수
- Index Hit Rate

**목표:**
- Connection Pool: 80% 이하 유지
- Slow Query: 제로
- Index Hit Rate: 95% 이상

---

## 측정 방법

### Method 1: Hibernate Statistics (가장 정확)

**용도:** 쿼리 수 정확한 측정

**구현:**

```java
// 1. Test 설정
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PerformanceComparisonTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PricingPolicyRepository repository;

    private SessionFactory sessionFactory;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        sessionFactory = entityManager.getEntityManagerFactory()
            .unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("PricingPolicy 조회 시 쿼리 수 비교")
    void testQueryCountComparison() {
        // Given
        RoomId roomId = RoomId.of(1L);

        // When
        PricingPolicy policy = repository.findById(roomId).orElseThrow();
        policy.getTimeRangePrices(); // Lazy 컬렉션 접근

        // Then
        long queryCount = statistics.getQueryExecutionCount();
        assertThat(queryCount)
            .as("EAGER 전환 후 쿼리는 1개여야 함")
            .isEqualTo(1); // 최적화 전: 2, 최적화 후: 1

        // 상세 통계 출력
        System.out.println("=== Hibernate Statistics ===");
        System.out.println("Query Count: " + statistics.getQueryExecutionCount());
        System.out.println("Query Time (ms): " + statistics.getQueryExecutionMaxTime());
        System.out.println("Entity Load Count: " + statistics.getEntityLoadCount());
    }
}
```

**측정 결과 비교표:**

| 시나리오 | 최적화 전 | Batch Size | JPQL Fetch Join | QueryDSL |
|---------|----------|------------|-----------------|----------|
| PricingPolicy 단건 조회 | 2 쿼리 | 2 쿼리 | 1 쿼리 | 1 쿼리 |
| PricingPolicy 10건 조회 | 11 쿼리 | 2 쿼리 | 1 쿼리 | 1 쿼리 |
| ReservationPricing 조회 | 3 쿼리 | 3 쿼리 | 2 쿼리 | 2 쿼리 |
| Product 재고 확인 | N+1 쿼리 | N+1 쿼리 | 1 쿼리 | 1 쿼리 (집계) |

---

### Method 2: Spring Boot Actuator Metrics

**용도:** 프로덕션 환경 모니터링

**구현:**

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: metrics,health,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

```java
// Custom Metrics
@Component
@RequiredArgsConstructor
public class QueryMetricsAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(org.springframework.data.jpa.repository.Query)")
    public Object measureQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(Timer.builder("repository.query.time")
                .tag("method", joinPoint.getSignature().getName())
                .register(meterRegistry));
        }
    }
}
```

**Prometheus Query 예시:**

```promql
# 평균 쿼리 시간
rate(repository_query_time_sum[5m]) / rate(repository_query_time_count[5m])

# P95 응답 시간
histogram_quantile(0.95, http_server_requests_seconds_bucket)

# 쿼리 실행 횟수
increase(repository_query_time_count[1h])
```

---

### Method 3: Database Query Profiling

**용도:** 실제 DB 레벨 성능 측정

**PostgreSQL EXPLAIN ANALYZE:**

```sql
-- 1. 최적화 전
EXPLAIN ANALYZE
SELECT p.* FROM pricing_policies p WHERE p.room_id = 1;
-- +
SELECT trp.* FROM time_range_prices trp WHERE trp.room_id = 1;

-- 2. 최적화 후 (FETCH JOIN)
EXPLAIN ANALYZE
SELECT p.*, trp.*
FROM pricing_policies p
LEFT JOIN time_range_prices trp ON p.room_id = trp.room_id
WHERE p.room_id = 1;
```

**비교 지표:**

```
최적화 전:
- Execution Time: 12.345 ms
- Planning Time: 0.234 ms
- Rows Fetched: 11
- Buffers: shared hit=15

최적화 후:
- Execution Time: 5.678 ms (54% 개선)
- Planning Time: 0.189 ms
- Rows Fetched: 11
- Buffers: shared hit=8 (47% 감소)
```

**pg_stat_statements 활용:**

```sql
-- 가장 느린 쿼리 Top 10
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    stddev_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

### Method 4: JMeter 부하 테스트

**용도:** 실제 부하 환경에서 성능 비교

**테스트 시나리오:**

```xml
<!-- PricingPolicy 조회 시나리오 -->
<ThreadGroup guiclass="ThreadGroupGui" testname="PricingPolicy Load Test">
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">10</stringProp>
  <stringProp name="ThreadGroup.duration">60</stringProp>
</ThreadGroup>

<HTTPSamplerProxy guiclass="HttpTestSampleGui" testname="Get Pricing Policy">
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8080</stringProp>
  <stringProp name="HTTPSampler.path">/api/pricing-policies/1</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
</HTTPSamplerProxy>
```

**측정 결과 비교:**

| 메트릭 | 최적화 전 | Batch Size | JPQL | QueryDSL |
|--------|----------|------------|------|----------|
| 평균 응답 시간 | 245ms | 180ms | 95ms | 92ms |
| P95 응답 시간 | 520ms | 380ms | 180ms | 170ms |
| P99 응답 시간 | 850ms | 620ms | 280ms | 250ms |
| 처리량 (RPS) | 408 | 555 | 1052 | 1086 |
| 에러율 | 0.2% | 0.1% | 0% | 0% |

---

### Method 5: VisualVM/JProfiler 프로파일링

**용도:** 메모리 및 CPU 사용량 분석

**측정 항목:**

1. **메모리 프로파일링**
   - Heap Memory 사용량
   - GC 빈도 및 pause time
   - Object Allocation rate

2. **CPU 프로파일링**
   - Hot Spot 메서드 식별
   - Thread 상태 분석
   - Lock Contention 측정

**비교 예시:**

```
최적화 전:
- Heap Used: 512MB → 789MB (피크)
- GC Count: 15회/분
- GC Pause: 평균 45ms
- Entity 객체 생성: 1,200개/초

최적화 후:
- Heap Used: 412MB → 545MB (피크)
- GC Count: 8회/분
- GC Pause: 평균 28ms
- Entity 객체 생성: 450개/초
```

---

## 자동화된 성능 비교 테스트

### 통합 테스트로 성능 회귀 방지

```java
@SpringBootTest
@Sql(scripts = "/test-data.sql")
class PerformanceRegressionTest {

    @Autowired
    private PricingPolicyRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
            .unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @DisplayName("PricingPolicy 조회 성능 - 쿼리 수 제한")
    void pricingPolicyQueryCountLimit() {
        // Given
        RoomId roomId = RoomId.of(1L);

        // When
        PricingPolicy policy = repository.findById(roomId).orElseThrow();
        policy.getTimeRangePrices().getPrices(); // 컬렉션 접근
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(statistics.getQueryExecutionCount())
            .as("PricingPolicy 조회는 1개의 쿼리만 실행되어야 함")
            .isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Reservation 생성 성능 - 쿼리 시간 제한")
    void reservationCreationPerformanceLimit() {
        // Given
        CreateReservationRequest request = /* ... */;

        // When
        long startTime = System.currentTimeMillis();
        reservationService.createReservation(request);
        long endTime = System.currentTimeMillis();

        // Then
        long executionTime = endTime - startTime;
        assertThat(executionTime)
            .as("Reservation 생성은 200ms 이내에 완료되어야 함")
            .isLessThan(200);

        assertThat(statistics.getQueryExecutionCount())
            .as("Reservation 생성은 최대 5개 쿼리만 허용")
            .isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Product 재고 확인 - 메모리 효율")
    void productAvailabilityMemoryEfficiency() {
        // Given
        Product product = /* ... */;
        List<LocalDateTime> slots = /* 100개 슬롯 */;

        // When
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        boolean available = productAvailabilityService.isAvailable(
            product, slots, 5, repository);

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        // Then
        long memoryUsed = memoryAfter - memoryBefore;
        assertThat(memoryUsed)
            .as("재고 확인은 10MB 이하의 메모리만 사용해야 함")
            .isLessThan(10 * 1024 * 1024);
    }
}
```

---

## 성능 비교 리포트 템플릿

### 최적화 전후 비교표

```markdown
## 성능 최적화 결과 리포트

### 테스트 환경
- Java Version: 21
- Spring Boot: 3.2.5
- PostgreSQL: 16
- 테스트 데이터: PricingPolicy 100건, Reservation 1000건
- 동시 사용자: 100명
- 테스트 지속 시간: 60초

### 결과 요약

| 메트릭 | 최적화 전 | 최적화 후 | 개선율 |
|--------|----------|----------|--------|
| 평균 쿼리 수 | 12.5 | 2.8 | 77.6% |
| P95 응답 시간 | 420ms | 95ms | 77.4% |
| 처리량 (RPS) | 450 | 1150 | 155.6% |
| DB CPU | 65% | 28% | 56.9% |
| 메모리 사용량 | 678MB | 412MB | 39.2% |

### 세부 결과

#### 1. PricingPolicy 조회

**쿼리 분석:**
- 최적화 전: 2 queries (1 policy + 1 time_range_prices)
- 최적화 후: 1 query (LEFT JOIN FETCH)
- 개선율: 50%

**응답 시간:**
- P50: 45ms → 18ms (60% 개선)
- P95: 120ms → 35ms (70.8% 개선)
- P99: 250ms → 65ms (74% 개선)

#### 2. Reservation 생성

**쿼리 분석:**
- 최적화 전: 15 queries (1 policy + 5 products + 1 save + ...)
- 최적화 후: 4 queries (1 policy + 1 batch products + 1 save + ...)
- 개선율: 73.3%

**응답 시간:**
- P50: 180ms → 65ms (63.9% 개선)
- P95: 420ms → 120ms (71.4% 개선)
- P99: 780ms → 210ms (73.1% 개선)

#### 3. Product 재고 확인

**쿼리 분석:**
- 최적화 전: 1 query + in-memory processing
- 최적화 후: 1 aggregation query
- 개선율: CPU 사용량 85% 감소

**응답 시간:**
- P50: 95ms → 12ms (87.4% 개선)
- P95: 250ms → 28ms (88.8% 개선)
- P99: 500ms → 55ms (89% 개선)

### 결론

모든 주요 메트릭에서 목표치(60-80% 개선)를 달성하였으며,
특히 쿼리 수 감소(77.6%)와 응답 시간 개선(77.4%)이 두드러짐.

프로덕션 배포 권장.
```

---

## CI/CD 파이프라인 통합

### GitHub Actions 성능 테스트 자동화

```yaml
# .github/workflows/performance-test.yml
name: Performance Regression Test

on:
  pull_request:
    branches: [ main ]

jobs:
  performance-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Performance Tests
        run: |
          cd springProject
          ./gradlew test --tests '*PerformanceRegressionTest'

      - name: Check Query Count Threshold
        run: |
          # 테스트 결과에서 쿼리 수 추출 및 임계치 검증
          QUERY_COUNT=$(grep "QueryExecutionCount" build/test-results/test/*.xml | awk '{print $2}')
          if [ "$QUERY_COUNT" -gt 5 ]; then
            echo "Query count ($QUERY_COUNT) exceeds threshold (5)"
            exit 1
          fi
```

---

## 권장 측정 프로세스

### Step 1: 베이스라인 수집 (최적화 전)

1. Hibernate Statistics로 쿼리 수 측정
2. JMeter로 부하 테스트 (100명 동시 사용자, 60초)
3. PostgreSQL EXPLAIN ANALYZE로 실행 계획 수집
4. 결과를 `docs/performance-baseline.md`에 기록

### Step 2: 최적화 적용

1. Phase 1 (Batch Size) 적용
2. 성능 재측정 및 비교
3. 목표치 달성 시 다음 단계, 미달성 시 조정

### Step 3: 최종 검증

1. 모든 최적화 완료 후 전체 재측정
2. 베이스라인 대비 개선율 계산
3. 회귀 테스트로 기능 정상 동작 확인
4. 스테이징 환경에 배포 및 모니터링

### Step 4: 프로덕션 모니터링

1. Prometheus + Grafana로 실시간 모니터링
2. 슬로우 쿼리 알람 설정
3. 주간 성능 리포트 생성

---

## 도구 요약

| 목적 | 도구 | 장점 | 사용 시점 |
|------|------|------|----------|
| 쿼리 수 측정 | Hibernate Statistics | 정확, 간단 | 개발/테스트 |
| 부하 테스트 | JMeter | 실전 시뮬레이션 | 배포 전 |
| DB 분석 | EXPLAIN ANALYZE | DB 레벨 최적화 | 쿼리 튜닝 |
| 메모리 분석 | VisualVM | 상세 프로파일 | 메모리 이슈 |
| 프로덕션 모니터링 | Prometheus | 실시간, 히스토리 | 운영 환경 |

---

## 참고 자료

- Hibernate Performance Tuning: https://vladmihalcea.com/
- JMeter Best Practices: https://jmeter.apache.org/usermanual/best-practices.html
- PostgreSQL Performance Tips: https://wiki.postgresql.org/wiki/Performance_Optimization

---

End of Document