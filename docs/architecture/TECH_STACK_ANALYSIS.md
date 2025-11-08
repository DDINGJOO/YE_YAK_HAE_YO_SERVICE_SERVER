# 기술 스택 분석 및 선정

## 개요

Hexagonal Architecture + DDD 아키텍처와 도메인 모델 설계를 바탕으로 최적의 기술 스택을 선정합니다.
각 레이어별로 여러 기술 대안을 비교하고 프로젝트 요구사항에 가장 적합한 선택을 도출합니다.

---

## 1. 프로그래밍 언어 및 프레임워크

### Java 버전 선택

#### Java 17 (LTS)

**장점:**
- LTS 버전으로 장기 지원
- Record 클래스로 Value Object 표현 간편
- Sealed Class로 타입 안전성 향상
- Pattern Matching for switch (Preview)
- Text Blocks로 가독성 향상

**단점:**
- Java 21 대비 최신 기능 부족

#### Java 21 (LTS)

**장점:**
- **Record Pattern**: Value Object 분해 용이
```java
if (money instanceof Money(BigDecimal amount)) {
    // amount 직접 사용
}
```
- **Pattern Matching for switch**: ProductScope, PricingType 처리 간결
```java
return switch (scope) {
    case PLACE -> checkPlaceScopedAvailability(...);
    case ROOM -> checkRoomScopedAvailability(...);
    case RESERVATION -> checkSimpleStockAvailability(...);
};
```
- **Virtual Threads (Project Loom)**: 고성능 비동기 처리
- **Sequenced Collections**: List 순서 처리 개선

**단점:**
- 비교적 최신 버전 (하지만 LTS)

**선택: Java 21 LTS**

**근거:**
- Pattern Matching이 도메인 로직 표현에 최적
- Record Pattern으로 VO 활용도 증가
- Virtual Threads로 향후 성능 개선 여지

---

### Spring Boot 버전

#### Spring Boot 3.2.x

**장점:**
- Java 17+ 필수 (Java 21 지원)
- Spring Framework 6.1.x 기반
- Native Image 지원 (GraalVM)
- Observability 개선 (Micrometer Tracing)
- Problem Details (RFC 7807) 지원

**주요 기능:**
- `@Transactional` 개선
- JPA 3.1 지원
- Hibernate 6.x
- Jakarta EE 10

**선택: Spring Boot 3.2.5**

---

## 2. 영속성 (Persistence)

### JPA 구현체

#### Hibernate 6.4.x

**장점:**
- Spring Boot 3.2 기본 JPA 구현체
- Java Record 지원
- `@Embeddable` Record 지원
- Performance 개선 (Batch Fetch)

**Record as Embeddable:**
```java
@Embeddable
public record Money(BigDecimal amount) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }
}
```

**선택: Hibernate 6.4.x (Spring Boot 기본)**

---

### Database

#### PostgreSQL 16

**장점:**
- **JSON/JSONB 지원**: 향후 확장 시 유연성
- **Range Types**: TimeRange 표현 가능
```sql
CREATE TABLE time_range_prices (
    day_of_week VARCHAR(10),
    time_range TSRANGE, -- PostgreSQL Range Type
    price NUMERIC(10, 2)
);

-- 중복 검증 (Exclusion Constraint)
ALTER TABLE time_range_prices
ADD CONSTRAINT no_overlap
EXCLUDE USING GIST (day_of_week WITH =, time_range WITH &&);
```
- **MVCC**: 동시성 성능 우수
- **Index 성능**: B-Tree, GiST, GIN
- **Transactional DDL**: 스키마 변경 안전

**vs MySQL:**
- MySQL은 Range Type 미지원
- MySQL은 CHECK Constraint 제한적
- PostgreSQL이 복잡한 쿼리에 유리

**선택: PostgreSQL 16**

**근거:**
- 시간대 중복 검증을 DB 레벨에서 보장 가능
- 복잡한 재고 집계 쿼리 성능
- JSON 지원으로 향후 확장성

---

## 3. 메시징 시스템

### 이벤트 브로커

#### Apache Kafka

**장점:**
- **높은 처리량**: 100,000+ msg/sec
- **영속성**: 메시지 저장 (재처리 가능)
- **Offset 관리**: At-least-once 보장
- **Partition**: 수평 확장
- **Confluent 생태계**: Schema Registry, ksqlDB

**Spring Kafka 활용:**
```java
@KafkaListener(topics = "room-created-events", groupId = "pricing-service")
public void handleRoomCreated(RoomCreatedEvent event) {
    // Idempotent 처리
}
```

**vs RabbitMQ:**
- RabbitMQ는 메시지 라우팅 복잡도 높음
- Kafka는 이벤트 소싱 확장 용이

**선택: Apache Kafka 3.6.x**

**근거:**
- MSA 환경에서 표준
- 이벤트 재처리 필요성 (중복 이벤트 대응)
- 향후 이벤트 소싱 전환 가능

---

## 4. 테스트 프레임워크

### 단위 테스트

#### JUnit 5 + AssertJ

```java
@Test
void 시간대_중복_시_예외_발생() {
    // Given
    List<TimeRangePrice> prices = List.of(
        new TimeRangePrice(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), Money.of(10000)),
        new TimeRangePrice(MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0), Money.of(15000))
    );

    // When & Then
    assertThatThrownBy(() -> TimeRangePrices.of(prices, TimeSlot.HOUR))
        .isInstanceOf(TimeRangeOverlapException.class)
        .hasMessageContaining("중복");
}
```

---

### 통합 테스트

#### Testcontainers

**장점:**
- 실제 PostgreSQL 컨테이너 사용
- 실제 Kafka 컨테이너 사용
- CI/CD 파이프라인 통합 용이

```java
@Testcontainers
@SpringBootTest
class ReservationPricingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Test
    void 예약_생성_시_재고_차감() {
        // 실제 DB, Kafka 사용
    }
}
```

**선택: Testcontainers 1.19.x**

---

### BDD 테스트

#### Spring Boot Test + Custom DSL

```java
@Test
void 예약_생성_플로우() {
    // Given - 가격 정책 설정
    var pricingPolicy = given_가격정책(
        디폴트(8000원),
        월요일_09시_13시(10000원),
        월요일_14시_21시(15000원)
    );

    // And - 추가상품 설정
    var product = given_플레이스상품(
        "빔프로젝터",
        총재고(2개),
        첫시간(10000원),
        추가시간(5000원)
    );

    // When - 예약 생성
    var reservation = when_예약생성(
        월요일_09시_15시,
        빔프로젝터(1개)
    );

    // Then - 가격 검증
    then_총가격은(
        시간대가격(10000 + 10000 + 15000 + 15000 + 15000 + 15000) +
        상품가격(10000 + 5000 + 5000 + 5000 + 5000 + 5000)
    );
}
```

---

## 5. API 문서화

### OpenAPI 3.0 (Springdoc)

```java
@Operation(summary = "예약 가격 계산", description = "시간대와 추가상품을 기반으로 총 가격 계산")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "계산 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (시간대 중복, 재고 부족 등)")
})
@PostMapping("/reservations/pricing")
public ResponseEntity<ReservationPricingResponse> calculatePricing(
    @Valid @RequestBody CalculatePricingRequest request
) {
    // ...
}
```

**장점:**
- Swagger UI 자동 생성
- Spring Boot 3.x 지원
- Jakarta EE annotation 지원

**선택: Springdoc OpenAPI 2.3.x**

---

## 6. 빌드 도구

### Gradle 8.x (Kotlin DSL)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
}
```

**vs Maven:**
- Gradle은 빌드 속도 빠름 (Incremental Build)
- Kotlin DSL이 타입 안전
- Multi-module 관리 용이

**선택: Gradle 8.5 (Kotlin DSL)**

---

## 7. 코드 품질 도구

### CheckStyle + SpotBugs + PMD

```gradle
plugins {
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.7"
    id("pmd")
}

checkstyle {
    toolVersion = "10.12.7"
    configFile = file("${rootDir}/config/checkstyle/google_checks.xml")
}
```

**Google Java Style Guide 적용:**
- 2 spaces 들여쓰기
- 100자 줄 길이 제한
- Import 순서 규칙

**선택: CheckStyle 10.x (Google Style)**

---

## 8. 모니터링 및 Observability

### Spring Boot Actuator + Micrometer

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**주요 메트릭:**
- 예약 생성 응답 시간 (P50, P95, P99)
- 재고 검증 실패율
- 이벤트 처리 지연 시간
- DB Connection Pool 사용률

**선택: Micrometer + Prometheus + Grafana**

---

## 9. 로깅

### SLF4J + Logback

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>

    <logger name="com.example.reservationpricing.domain" level="DEBUG"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

**구조화된 로깅:**
- JSON 포맷
- Trace ID 포함 (분산 추적)
- 도메인 이벤트 로깅

**선택: Logback + Logstash Encoder**

---

## 10. 보안

### Spring Security (선택적)

현재 요구사항에는 인증/인가 없음.
향후 필요 시 추가 예정.

**JWT 검증만 필요한 경우:**
```java
@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = extractToken(request);
        // JWT 검증 로직
    }
}
```

---

## 최종 기술 스택 요약

### Core

| 레이어 | 기술 | 버전 |
|--------|------|------|
| Language | Java | 21 LTS |
| Framework | Spring Boot | 3.2.5 |
| Build Tool | Gradle (Kotlin DSL) | 8.5 |

### Persistence

| 항목 | 기술 | 버전 |
|------|------|------|
| ORM | Hibernate (JPA) | 6.4.x |
| Database | PostgreSQL | 16 |
| Migration | Flyway | 10.x |

### Messaging

| 항목 | 기술 | 버전 |
|------|------|------|
| Event Broker | Apache Kafka | 3.6.x |
| Client | Spring Kafka | 3.1.x |

### Testing

| 항목 | 기술 | 버전 |
|------|------|------|
| Unit Test | JUnit 5 + AssertJ | 5.10.x |
| Integration Test | Testcontainers | 1.19.x |
| Mocking | Mockito | 5.x |

### Documentation

| 항목 | 기술 | 버전 |
|------|------|------|
| API Docs | Springdoc OpenAPI | 2.3.x |
| Swagger UI | Auto-generated | - |

### Code Quality

| 항목 | 기술 | 버전 |
|------|------|------|
| Style | CheckStyle (Google) | 10.12.x |
| Bug Detection | SpotBugs | 4.8.x |
| Static Analysis | PMD | 7.0.x |

### Observability

| 항목 | 기술 | 버전 |
|------|------|------|
| Metrics | Micrometer + Prometheus | - |
| Logging | Logback + Logstash | 1.4.x |
| Tracing | Spring Boot Actuator | - |

---

## 의존성 버전 관리 전략

### Spring Boot BOM 활용

```kotlin
dependencies {
    // Spring Boot가 버전 관리
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 명시적 버전 (BOM 외부)
    implementation("org.apache.commons:commons-lang3:3.14.0")
}
```

### Dependabot 설정

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
```

---

## 성능 최적화 전략

### 1. Database

#### Connection Pool (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### JPA 최적화
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        format_sql: true
    show-sql: false
```

### 2. Kafka

#### Producer 설정
```yaml
spring:
  kafka:
    producer:
      acks: 1  # Leader 확인만 (성능 우선)
      compression-type: lz4
      batch-size: 16384
      linger-ms: 10
```

#### Consumer 설정
```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 100
      fetch-min-size: 1024
      enable-auto-commit: false  # 수동 커밋
```

### 3. Caching (선택적)

현재는 불필요하지만 향후 필요 시:
```java
@Cacheable(value = "pricingPolicies", key = "#roomId")
public PricingPolicy findByRoomId(RoomId roomId) {
    // ...
}
```

---

## 개발 환경 구성

### Docker Compose (로컬 개발)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: reservation_pricing
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev
    ports:
      - "5432:5432"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
```

---

## CI/CD 파이프라인 (GitHub Actions)

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run Tests
        run: ./gradlew test

      - name: CheckStyle
        run: ./gradlew checkstyleMain

      - name: Test Coverage
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

---

## 결론

**선정된 기술 스택은 다음 원칙을 충족합니다:**

1. **최신 LTS 버전**: Java 21, Spring Boot 3.2
2. **DDD 친화적**: Record, Pattern Matching
3. **성능**: PostgreSQL, Kafka, HikariCP
4. **테스트 용이성**: Testcontainers, JUnit 5
5. **코드 품질**: Google Style, CheckStyle
6. **Observability**: Micrometer, Prometheus

다음 단계: **최종 아키텍처 결정 문서(ADR) 작성**
