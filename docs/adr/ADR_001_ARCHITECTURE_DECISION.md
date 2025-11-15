# ADR 001: 예약 가격 관리 서비스 아키텍처 결정

## Status
**ACCEPTED** - 2025-11-08

## Context

### 비즈니스 요구사항
MSA 아키텍처 환경에서 플레이스(Place)와 룸(Room)의 예약 가격 계산 및 관리를 담당하는 독립적인 마이크로서비스 개발이 필요합니다.

**핵심 기능:**
1. 시간대별 예약 가격 세팅 및 계산
2. 추가상품 관리 (플레이스/룸/예약 범위별)
3. 예약 총 가격 정보 저장 및 계산 (형상관리)

**주요 도메인 복잡도:**
- 요일/시간대별 차등 가격 정책
- 시간대 중복 검증
- 3가지 범위(Scope)의 재고 관리 전략
- 예약 상태별 재고 차감/복구
- 가격 정보의 불변성 보장 (Snapshot)

**비기능 요구사항:**
- 가격 계산 정확도: 100% (금액 오차 허용 불가)
- 예약 생성 응답 시간: P95 500ms 이하
- 새로운 가격 정책 추가 용이 (OCP)
- 테스트 커버리지: 80% 이상

---

## Decision

다음과 같은 아키텍처 스택을 채택합니다:

### 1. Hexagonal Architecture + Domain-Driven Design (DDD)

#### 구조
```
Adapter Layer (Infrastructure)
    ↓ (Driving Ports)
Application Layer (Use Cases)
    ↓
Domain Layer (Business Logic)
    ↑ (Driven Ports)
Infrastructure Layer (Implementation)
```

#### 패키지 구조
```
com.example.reservationpricing
├── adapter
│   ├── in (Driving Adapters)
│   │   ├── web (REST API)
│   │   └── event (Kafka Listener)
│   └── out (Driven Adapters)
│       ├── persistence (JPA)
│       └── event (Kafka Publisher)
├── application
│   ├── port
│   │   ├── in (Use Case Interfaces)
│   │   └── out (Repository Ports)
│   └── service (Application Services)
└── domain
    ├── pricingpolicy (Aggregate)
    ├── product (Aggregate)
    ├── reservation (Aggregate)
    ├── service (Domain Services)
    └── shared (Shared VOs)
```

### 2. 도메인 모델

#### Aggregate Root 3개

**A. PricingPolicy (시간대별 가격 정책)**
```java
@Entity
class PricingPolicy {
    @EmbeddedId
    private RoomId roomId; // Aggregate ID

    @Embedded
    private Money defaultPrice;

    @Embedded
    private TimeRangePrices timeRangePrices; // Value Object Collection

    public void resetPrices(Money defaultPrice, List<TimeRangePrice> newPrices) {
        this.defaultPrice = requireNonNull(defaultPrice);
        this.timeRangePrices = TimeRangePrices.of(newPrices, timeSlot);
    }

    public TimeSlotPriceBreakdown calculatePriceBreakdown(List<LocalDateTime> slots) {
        return timeRangePrices.createBreakdown(slots, defaultPrice, timeSlot);
    }
}
```

**B. Product (추가상품)**
```java
@Entity
class Product {
    @Id @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProductScope scope; // PLACE | ROOM | RESERVATION

    private Long scopeId; // PlaceId or RoomId

    @Embedded
    private PricingStrategy pricingStrategy; // Strategy Pattern

    private int totalQuantity;

    public ProductPriceBreakdown calculatePrice(int quantity, List<LocalDateTime> slots) {
        Money unitPrice = pricingStrategy.calculate(slots);
        Money totalPrice = unitPrice.multiply(quantity);
        return new ProductPriceBreakdown(id, name, quantity, unitPrice, totalPrice);
    }
}
```

**C. ReservationPricing (예약 가격 스냅샷)**
```java
@Entity
class ReservationPricing {
    @Id @GeneratedValue
    private Long id;

    @Embedded
    private ReservationId reservationId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // PENDING | CONFIRMED | CANCELLED

    @Embedded
    private TimeSlotPriceBreakdown timeSlotBreakdown; // VO

    @ElementCollection
    private List<ProductPriceBreakdown> productBreakdowns; // VO

    @Embedded
    private Money totalPrice;

    private LocalDateTime calculatedAt;

    // Factory Method
    public static ReservationPricing calculate(...) {
        // 가격 계산 및 스냅샷 생성
    }

    // 상태 전이
    public void confirm() { ... }
    public void cancel() { ... }
}
```

#### Domain Service

**ProductAvailabilityService (재고 검증)**
```java
@DomainService
class ProductAvailabilityService {
    public boolean isAvailable(
        Product product,
        List<LocalDateTime> requestedSlots,
        int requestedQuantity,
        ReservationPricingRepository repository
    ) {
        return switch (product.getScope()) {
            case PLACE -> checkPlaceScopedAvailability(...);
            case ROOM -> checkRoomScopedAvailability(...);
            case RESERVATION -> checkSimpleStockAvailability(...);
        };
    }
}
```

### 3. 기술 스택

| 레이어 | 기술 | 버전 | 선정 이유 |
|--------|------|------|-----------|
| Language | Java | 21 LTS | Record, Pattern Matching, Sealed Class |
| Framework | Spring Boot | 3.2.5 | Jakarta EE 10, Virtual Threads |
| ORM | Hibernate (JPA) | 6.4.x | Record as Embeddable 지원 |
| Database | PostgreSQL | 16 | Range Type, Exclusion Constraint |
| Messaging | Apache Kafka | 3.6.x | 이벤트 영속성, 재처리 |
| Build | Gradle (Kotlin DSL) | 8.5 | Incremental Build, 타입 안전 |
| Test | JUnit 5 + Testcontainers | 5.10 / 1.19 | 실제 DB/Kafka 테스트 |
| Docs | Springdoc OpenAPI | 2.3.x | Swagger UI 자동 생성 |
| Code Quality | CheckStyle (Google) | 10.12 | Google Java Style Guide |

---

## Rationale

### 1. Hexagonal Architecture 선택 이유

#### vs Layered Architecture
| 비교 항목 | Layered | Hexagonal | 선택 근거 |
|----------|---------|-----------|----------|
| 도메인 복잡도 처리 | 2/5 | 5/5 | **복잡한 가격/재고 로직 캡슐화 필요** |
| OCP 준수 | 2/5 | 5/5 | **새로운 가격 정책 추가 빈번** |
| 테스트 용이성 | 2/5 | 5/5 | **100% 정확도 요구사항** |
| 개발 속도 | 5/5 | 3/5 | 초기 설계 비용 감수 |

#### vs Clean Architecture
| 비교 항목 | Clean | Hexagonal | 선택 근거 |
|----------|-------|-----------|----------|
| 복잡도 | 4 레이어 | 3 레이어 | **단순함 선호** |
| Use Case 명시성 | 5/5 | 4/5 | Application Service로 충분 |
| 학습 곡선 | 높음 | 중간 | **팀 생산성 고려** |

**결론:** Hexagonal이 도메인 복잡도를 처리하면서도 Clean보다 단순

---

### 2. DDD 패턴 적용 이유

#### Aggregate 설계

**PricingPolicy Aggregate:**
- **Invariant**: 시간대 중복 불가
- **Transaction Boundary**: Room 단위
- **Why Aggregate?**: 중복 검증이 전체 리스트 대상이므로 Aggregate 내부 처리 필수

**Product Aggregate:**
- **Invariant**: 총 재고 ≥ 0
- **Transaction Boundary**: Product 단위
- **Why Aggregate?**: 재고 변경 시 일관성 보장

**ReservationPricing Aggregate:**
- **Invariant**: 총가격 = 시간대 + 상품 가격
- **Transaction Boundary**: Reservation 단위
- **Why Aggregate?**: 가격 스냅샷 불변성 보장

#### Value Object 활용

**Money:**
```java
@Embeddable
public record Money(BigDecimal amount) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public static final Money ZERO = new Money(BigDecimal.ZERO);
}
```

**이점:**
- 도메인 개념 명시 (BigDecimal → Money)
- 불변성 보장
- 비즈니스 로직 캡슐화 (음수 방지)

---

### 3. 도메인 모델 설계 결정

#### A. 시간대별 가격: Value Object Collection

**대안 비교:**
| 대안 | 장점 | 단점 | 점수 |
|------|------|------|------|
| Separate Entity | 조회 성능 우수 | 도메인 로직 유출 | 14/20 |
| Embedded Collection | 단순 | 성능 이슈 | 15/20 |
| **VO Collection** | **도메인 캡슐화** | **코드 증가** | **16/20** (선택) |

**선택 이유:**
- Room당 최대 10개 정책 → 성능 이슈 없음
- 중복 검증 로직을 `TimeRangePrices` VO에 캡슐화
- 불변성 보장 (`List.copyOf()`)

```java
@Embeddable
class TimeRangePrices {
    @ElementCollection
    private List<TimeRangePrice> prices;

    public static TimeRangePrices of(List<TimeRangePrice> prices, TimeSlot timeSlot) {
        validateNoOverlap(prices); // 도메인 로직
        return new TimeRangePrices(List.copyOf(prices));
    }

    public Money calculateTotal(List<LocalDateTime> slots, Money defaultPrice) {
        return slots.stream()
            .map(slot -> findPriceForSlot(slot).orElse(defaultPrice))
            .reduce(Money.ZERO, Money::add);
    }
}
```

#### B. 추가상품: Strategy Pattern + Domain Service

**대안 비교:**
| 대안 | 장점 | 단점 | 점수 |
|------|------|------|------|
| Scope별 별도 Aggregate | 타입 안전 | 코드 중복 | 11/20 |
| **단일 Aggregate + Strategy** | **OCP 준수** | **범용 타입** | **18/20** (선택) |

**선택 이유:**
- 3가지 Scope의 본질은 "재고 확인 방식"과 "가격 계산 방식"의 차이
- `ProductScope` Enum + `PricingStrategy` VO로 확장성 확보
- 새로운 Scope/Pricing 추가 시 Enum만 확장

**Strategy Pattern:**
```java
enum PricingType {
    INITIAL_PLUS_ADDITIONAL, ONE_TIME, SIMPLE_STOCK
}

@Embeddable
class PricingStrategy {
    private PricingType type;
    private Money initialPrice;
    private Money additionalPrice; // nullable

    public Money calculate(List<LocalDateTime> slots) {
        return switch (type) {
            case INITIAL_PLUS_ADDITIONAL -> initial + (additional * (slots.size() - 1));
            case ONE_TIME -> initialPrice;
            case SIMPLE_STOCK -> initialPrice;
        };
    }
}
```

**Domain Service 사용:**
- 재고 검증 로직이 복잡 (다른 Aggregate 조회 필요)
- `ProductAvailabilityService`로 분리

#### C. 예약 가격: Value Object Snapshot

**대안 비교:**
| 대안 | 장점 | 단점 | 점수 |
|------|------|------|------|
| Flat Structure (BigDecimal) | 단순 | 타입 안전성 없음 | 12/20 |
| **VO Snapshot** | **도메인 표현력** | **JPA 매핑 복잡** | **18/20** (선택) |

**선택 이유:**
- 가격 정보는 **형상관리** 대상 (불변성 필수)
- `TimeSlotPriceBreakdown`, `ProductPriceBreakdown` VO로 명시적 표현
- 타입 안전성으로 컴파일 타임 검증

```java
@Embeddable
record ProductPriceBreakdown(
    ProductId productId,
    String productName,
    int quantity,
    Money unitPrice,
    Money totalPrice,
    PricingType pricingType
) {
    // Immutable snapshot
}
```

---

### 4. 기술 스택 결정

#### Java 21 선택
- **Record Pattern**: VO 분해 용이
```java
if (breakdown instanceof ProductPriceBreakdown(var id, var name, var qty, var unit, var total, var type)) {
    // 직접 사용
}
```
- **Pattern Matching for switch**: ProductScope, PricingType 처리 간결
- **Sealed Class**: 향후 확장 시 타입 안전성

#### PostgreSQL 선택
- **Range Type**: 시간대 중복 검증을 DB 레벨에서 보장
```sql
ALTER TABLE time_range_prices
ADD CONSTRAINT no_overlap
EXCLUDE USING GIST (day_of_week WITH =, time_range WITH &&);
```
- **JSONB**: 향후 가격 정책 확장 시 유연성
- **복잡한 재고 집계 쿼리 성능**

#### Kafka 선택
- **이벤트 영속성**: 중복 이벤트 재처리 가능
- **At-least-once 보장**: Offset 관리
- **MSA 표준**: 다른 서비스와 통합 용이

---

## Consequences

### Positive

1. **도메인 로직 캡슐화**
   - Aggregate와 VO에 비즈니스 로직 집중
   - Service 레이어는 얇게 유지 (Orchestration만)

2. **확장성 (OCP)**
   - 새로운 가격 정책: `PricingType` Enum 추가
   - 새로운 상품 Scope: `ProductScope` Enum 추가
   - 기존 코드 수정 불필요

3. **테스트 용이성**
   - 도메인 로직: 순수 단위 테스트 (Infrastructure 의존 없음)
   - Application Service: Port Mocking
   - Adapter: Testcontainers로 통합 테스트

4. **정확성 보장**
   - Money VO로 금액 계산 타입 안전
   - Aggregate 불변식으로 잘못된 상태 방지
   - PostgreSQL Constraint로 DB 레벨 검증

5. **유지보수성**
   - 명확한 레이어 분리 (Hexagonal)
   - 높은 응집도 (Aggregate)
   - 낮은 결합도 (Port/Adapter)

### Negative

1. **초기 개발 비용**
   - Port/Adapter 인터페이스 정의 필요
   - VO 설계 시간 증가
   - JPA 매핑 복잡도

2. **코드량 증가**
   - 인터페이스와 구현체 분리
   - VO 클래스 증가
   - 파일 수 증가

3. **학습 곡선**
   - 팀원의 Hexagonal/DDD 이해 필요
   - Java 21 신규 기능 학습
   - PostgreSQL Range Type 학습

### Mitigation (완화 방안)

1. **초기 비용 감수**
   - 장기적으로 유지보수 비용 감소
   - 새로운 요구사항 추가 시간 단축

2. **코드 템플릿 제공**
   - Aggregate, VO, Port/Adapter 템플릿
   - IntelliJ Live Template 활용

3. **문서화 및 교육**
   - ADR 문서 시리즈 작성
   - 코드 리뷰를 통한 지식 공유
   - 내부 세미나 진행

---

## Trade-offs

### 선택한 것 (What We Gain)

1. **도메인 중심 설계**
   - 비즈니스 로직이 순수하게 유지
   - 가격 계산 정확도 100% 보장

2. **확장성**
   - 새로운 가격 정책 추가: 1일 이내
   - 새로운 상품 타입 추가: 2일 이내

3. **테스트 커버리지 80%+**
   - 도메인 로직 100% 커버
   - Integration Test로 전체 플로우 검증

### 포기한 것 (What We Trade)

1. **빠른 초기 개발**
   - Layered Architecture 대비 2주 추가 소요 예상

2. **단순함**
   - 파일 수 증가 (약 1.5배)
   - 인터페이스 관리 필요

3. **팀원 친숙도**
   - 전통적 Layered 구조 대비 낯설음

### Why the Trade is Worth It

**요구사항 우선순위:**
1. 가격 계산 정확도 (필수)
2. 새로운 정책 추가 용이 (중요)
3. 개발 속도 (낮음)

→ **정확도와 확장성이 개발 속도보다 중요**하므로 Trade-off는 합리적

---

## Validation

### 설계 검증 체크리스트

#### 기능 요구사항

- 시간대별 가격 정책 설정 및 계산
  - `PricingPolicy` Aggregate로 구현
  - `TimeRangePrices` VO로 중복 검증

- 추가상품 관리 (3가지 Scope)
  - `Product` Aggregate + `ProductScope` Enum
  - `ProductAvailabilityService`로 재고 검증

- 예약 가격 형상관리
  - `ReservationPricing` Aggregate
  - `TimeSlotPriceBreakdown`, `ProductPriceBreakdown` VO

#### 비기능 요구사항

- 가격 계산 정확도 100%
  - Money VO (타입 안전)
  - BigDecimal 사용 (부동소수점 오차 방지)
  - Aggregate 불변식

- 응답 시간 P95 500ms 이하
  - PostgreSQL 인덱스
  - HikariCP Connection Pool
  - Kafka 비동기 처리

- OCP 준수
  - Strategy Pattern
  - Port/Adapter 분리

- 테스트 커버리지 80%+
  - 순수 도메인 로직 단위 테스트
  - Testcontainers 통합 테스트

#### SOLID 원칙

- **SRP**: 각 Aggregate는 단일 책임
- **OCP**: Strategy Pattern으로 확장
- **LSP**: VO는 모두 불변 객체
- **ISP**: Repository Port는 Aggregate별 분리
- **DIP**: Domain이 Infrastructure에 의존하지 않음

---

## Alternatives Considered

### 대안 1: Layered Architecture + Transaction Script

```java
@Service
class ReservationService {
    public ReservationPricing createReservation(...) {
        // 1. 가격 정책 조회
        PricingPolicy policy = pricingPolicyRepository.findById(roomId);

        // 2. 가격 계산 (Service에서 직접)
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (LocalDateTime slot : slots) {
            totalPrice = totalPrice.add(calculateSlotPrice(policy, slot));
        }

        // 3. 상품 가격 계산 (Service에서 직접)
        for (ProductRequest req : products) {
            Product product = productRepository.findById(req.productId());
            totalPrice = totalPrice.add(calculateProductPrice(product, slots));
        }

        // 4. 저장
        return reservationPricingRepository.save(...);
    }
}
```

**거부 이유:**
- 도메인 로직이 Service에 분산
- 테스트 어려움 (Repository Mocking 필요)
- OCP 위반 (새로운 정책 추가 시 Service 수정)

---

### 대안 2: CQRS + Event Sourcing

**Command Side:**
```java
class CreateReservationCommand {
    // 예약 생성 커맨드
}

class ReservationAggregate {
    public void handle(CreateReservationCommand cmd) {
        // 이벤트 발행
        apply(new ReservationCreatedEvent(...));
        apply(new InventoryDeductedEvent(...));
    }
}
```

**Query Side:**
```java
class ReservationPricingReadModel {
    // 조회 최적화 모델
}
```

**거부 이유:**
- **Over-engineering**: 현재 요구사항 대비 과도한 복잡도
- **학습 곡선**: 팀 전체의 Event Sourcing 이해 필요
- **인프라 비용**: Event Store 필요
- **이벤트 스키마 관리**: 복잡도 증가

**향후 고려 가능:**
- 이력 추적 요구사항 증가 시
- 감사(Audit) 요구사항 강화 시

---

## Implementation Plan

### Phase 1: 핵심 도메인 모델 (Week 1-2)

1. **Shared Kernel**
   - Money VO
   - RoomId, PlaceId, ProductId VO
   - TimeSlot Enum

2. **PricingPolicy Aggregate**
   - TimeRangePrices VO
   - TimeRangePrice VO
   - 중복 검증 로직

3. **Product Aggregate**
   - ProductScope Enum
   - PricingStrategy VO
   - PricingType Enum

### Phase 2: Application Layer (Week 3)

1. **Port 정의**
   - Use Case Interfaces
   - Repository Ports

2. **Application Services**
   - PricingPolicyService
   - ProductService
   - ReservationPricingService

3. **Domain Service**
   - ProductAvailabilityService

### Phase 3: Adapter Layer (Week 4)

1. **Driving Adapters**
   - REST API Controller
   - Kafka Event Listener

2. **Driven Adapters**
   - JPA Repository Adapter
   - Kafka Event Publisher

### Phase 4: 테스트 및 문서화 (Week 5)

1. **단위 테스트**
   - Aggregate 테스트
   - VO 테스트
   - Domain Service 테스트

2. **통합 테스트**
   - Testcontainers
   - End-to-End 시나리오

3. **문서화**
   - OpenAPI 문서
   - README 업데이트

---

## References

- **Books:**
  - "Domain-Driven Design" by Eric Evans
  - "Implementing Domain-Driven Design" by Vaughn Vernon
  - "Clean Architecture" by Robert C. Martin

- **Articles:**
  - Alistair Cockburn, "Hexagonal Architecture" (2005)
  - Martin Fowler, "Patterns of Enterprise Application Architecture"

- **Documentation:**
  - [Spring Boot 3.2 Reference](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
  - [PostgreSQL Range Types](https://www.postgresql.org/docs/16/rangetypes.html)

---

## Authors

- **Architecture Design**: Senior Java Developer (7 years MSA experience)
- **Review**: Development Team
- **Date**: 2025-11-08

---

## Next ADRs

- ADR 002: 재고 관리 동시성 제어 전략
- ADR 003: 가격 정책 변경 이력 관리
- ADR 004: 이벤트 중복 처리 멱등성 보장

---

**Last Updated**: 2025-11-15
