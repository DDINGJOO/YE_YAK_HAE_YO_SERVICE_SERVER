# 예약 가격 관리 서비스 (Reservation Pricing Service)

**Version:** 0.0.1-SNAPSHOT
**Team:** DDINGJOO
**Tech Stack:** Java 21 | Spring Boot 3.2.5 | PostgreSQL 16 | Kafka 3.6 | Hexagonal Architecture + DDD

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [핵심 기능](#핵심-기능)
- [아키텍처](#아키텍처)
- [디자인 패턴](#디자인-패턴)
- [데이터베이스 스키마](#데이터베이스-스키마)
- [API 엔드포인트](#api-엔드포인트)
- [기술 스택](#기술-스택)
- [테스팅](#테스팅)
- [실행 가이드](#실행-가이드)
- [프로젝트 분석](#프로젝트-분석)
- [향후 개선사항](#향후-개선사항)
- [문서](#문서)

---

## 프로젝트 개요

### 비즈니스 목표

플레이스(Place)와 룸(Room)을 관리하는 공간 예약 시스템에서 가격 계산 로직을 독립된 마이크로서비스로 분리하여:

- **유연한 가격 정책 관리**: 시간대별, 요일별 차등 가격 설정으로 수익 최적화
- **정확한 가격 계산**: 복잡한 추가상품 가격 전략(렌탈, 일회성, 단순 재고)을 자동 계산
- **가격 이력 관리**: 예약 시점의 가격을 불변 스냅샷으로 저장하여 분쟁 방지

### 핵심 가치

1. **일관성**: 과거 예약의 가격 정보가 정책 변경에 영향받지 않음 (Immutable Snapshot)
2. **확장성**: MSA 아키텍처로 독립적인 배포 및 스케일링
3. **유지보수성**: Hexagonal Architecture와 DDD로 도메인 로직과 인프라 완전 분리

---

## 핵심 기능

### 1. 시간대별 가격 정책 관리

룸별로 요일과 시간대에 따른 차등 가격을 설정하고, 예약 시점에 정확한 가격을 계산합니다.

#### 비즈니스 시나리오

```
스터디 카페 룸 가격 정책:
- 기본 가격: 8,000원/시간
- 평일 피크타임 (14:00-21:00): 15,000원/시간
- 주말 전 시간대: 12,000원/시간
```

#### 도메인 모델 (코드 예제)

```java
public class PricingPolicy {
  private final RoomId roomId;
  private final PlaceId placeId;
  private final TimeSlot timeSlot;           // HOUR | HALFHOUR
  private Money defaultPrice;
  private TimeRangePrices timeRangePrices;   // 시간대별 가격 컬렉션

  // Factory Method Pattern
  public static PricingPolicy create(
      RoomId roomId, PlaceId placeId, TimeSlot timeSlot, Money defaultPrice) {
    return new PricingPolicy(roomId, placeId, timeSlot, defaultPrice,
                             TimeRangePrices.empty());
  }

  // 시간대별 가격 계산
  public Money calculatePrice(DayOfWeek dayOfWeek, LocalTime time) {
    return timeRangePrices
        .findPriceForTime(dayOfWeek, time)
        .orElse(defaultPrice);
  }
}
```

#### Value Object를 통한 도메인 규칙 강제

```java
// 불변 금액 객체
public record Money(BigDecimal amount) {
  public Money {
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
    }
  }

  public Money add(Money other) {
    return new Money(this.amount.add(other.amount));
  }
}

// 시간 범위 Value Object
public record TimeRange(LocalTime start, LocalTime end) {
  public TimeRange {
    if (start.isAfter(end) || start.equals(end)) {
      throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다");
    }
  }

  public boolean contains(LocalTime time) {
    return !time.isBefore(start) && time.isBefore(end);
  }
}
```

**비즈니스 규칙 강제:**
- 생성자에서 음수 금액 차단
- 시간 범위 유효성 자동 검증
- 불변성으로 예기치 않은 상태 변경 방지

---

### 2. 추가상품 재고 및 가격 관리

플레이스, 룸, 예약 단위로 적용 범위가 다른 상품을 관리하며, 3가지 가격 책정 전략을 지원합니다.

#### 상품 범위 (Scope)

```java
public enum ProductScope {
  PLACE,        // 플레이스 전체 공유 (예: 빔프로젝터)
  ROOM,         // 특정 룸 전용 (예: 전용 장비)
  RESERVATION   // 시간 독립적 (예: 음료, 간식)
}
```

#### Factory Pattern을 활용한 상품 생성

```java
public class Product {
  private final ProductId productId;
  private final ProductScope scope;
  private final PlaceId placeId;    // PLACE, ROOM scope만 사용
  private final RoomId roomId;      // ROOM scope만 사용
  private String name;
  private PricingStrategy pricingStrategy;
  private int totalQuantity;

  // Private 생성자 - 직접 생성 차단
  private Product(ProductId productId, ProductScope scope, ...) {
    validateScopeIds(placeId, roomId, scope);  // 도메인 규칙 강제
    this.productId = productId;
    // ...
  }

  // PLACE scope 상품 생성
  public static Product createPlaceScoped(
      ProductId productId, PlaceId placeId, String name,
      PricingStrategy strategy, int quantity) {
    return new Product(productId, ProductScope.PLACE,
                       placeId, null, name, strategy, quantity);
  }

  // ROOM scope 상품 생성
  public static Product createRoomScoped(
      ProductId productId, PlaceId placeId, RoomId roomId,
      String name, PricingStrategy strategy, int quantity) {
    return new Product(productId, ProductScope.ROOM,
                       placeId, roomId, name, strategy, quantity);
  }

  // RESERVATION scope 상품 생성
  public static Product createReservationScoped(
      ProductId productId, String name,
      PricingStrategy strategy, int quantity) {
    return new Product(productId, ProductScope.RESERVATION,
                       null, null, name, strategy, quantity);
  }
}
```

**Factory Pattern 장점:**
- Scope별로 필수 파라미터가 명확함 (컴파일 타임 검증)
- 불가능한 조합 생성 차단 (예: PLACE scope인데 roomId 있음)
- 자기 문서화 (메서드명으로 의도 명확)

#### Strategy Pattern을 활용한 가격 책정

```java
// 전략 인터페이스
public interface PricingStrategy {
  Money calculatePrice(int quantity);
  PricingType getType();
}

// 전략 1: 초기 가격 + 추가 가격 (1개 초과 시)
public class InitialPlusAdditionalPricing implements PricingStrategy {
  private final Money initialPrice;
  private final Money additionalPrice;

  @Override
  public Money calculatePrice(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
    }
    if (quantity == 1) {
      return initialPrice;
    }
    // 첫 1개 초기 가격 + 나머지 추가 가격
    Money additionalTotal = additionalPrice.multiply(quantity - 1);
    return initialPrice.add(additionalTotal);
  }
}

// 전략 2: 1회 대여료 (수량 무관)
public class OneTimePricing implements PricingStrategy {
  private final Money price;

  @Override
  public Money calculatePrice(int quantity) {
    return price;  // 수량과 관계없이 고정 가격
  }
}

// 전략 3: 단순 재고 (단가 × 수량)
public class SimpleStockPricing implements PricingStrategy {
  private final Money unitPrice;

  @Override
  public Money calculatePrice(int quantity) {
    return unitPrice.multiply(quantity);
  }
}
```

#### 사용 예시

```java
// 빔프로젝터: 첫 1시간 10,000원, 추가 5,000원/시간
Product projector = Product.createPlaceScoped(
    productId,
    placeId,
    "빔프로젝터",
    new InitialPlusAdditionalPricing(
        Money.of(10000),
        Money.of(5000)
    ),
    5  // 총 재고 5개
);

// 3시간 대여 가격 계산: 10,000 + 5,000 + 5,000 = 20,000원
Money totalPrice = projector.getPricingStrategy().calculatePrice(3);
```

**Strategy Pattern 장점:**
- 런타임에 가격 계산 알고리즘 교체 가능 (OCP 준수)
- 각 전략이 단일 책임만 가짐 (SRP 준수)
- 새로운 가격 정책 추가 시 기존 코드 수정 불필요

---

### 3. 예약 가격 계산 및 불변 스냅샷 저장

예약 시점의 가격 정보를 불변 객체로 저장하여 이후 가격 정책 변경에 영향받지 않습니다.

#### 예약 프로세스 (3단계)

```
1. 슬롯 예약 (외부 서비스)
   ↓ SlotReservedEvent 발행

2. 가격 계산 및 PENDING 상태 예약 생성 (이 서비스)
   - 시간대별 가격 계산
   - 추가상품 재고 확인
   - 총 가격 계산 및 스냅샷 저장
   ↓

3. 결제 완료 후 CONFIRMED 상태 전환 (외부 서비스)
   ↓ PaymentCompletedEvent 수신

4. 예약 확정 (이 서비스)
```

#### 불변 스냅샷 구현

```java
public class ReservationPricing {
  private final ReservationId reservationId;
  private final RoomId roomId;
  private final PlaceId placeId;
  private ReservationStatus status;  // PENDING → CONFIRMED → CANCELLED

  // 불변 가격 정보 (생성 후 변경 불가)
  private final TimeSlot timeSlot;
  private final List<TimeSlotPriceBreakdown> timeSlotPrices;
  private final List<ProductPriceBreakdown> productPrices;
  private final Money totalPrice;
  private final LocalDateTime calculatedAt;

  // 생성 시점 가격 계산
  public static ReservationPricing create(
      ReservationId reservationId,
      RoomId roomId,
      PlaceId placeId,
      TimeSlot timeSlot,
      List<TimeSlotPriceBreakdown> timeSlotPrices,
      List<ProductPriceBreakdown> productPrices) {

    // 총 가격 = 시간대 가격 합 + 상품 가격 합
    Money totalPrice = calculateTotalPrice(timeSlotPrices, productPrices);

    return new ReservationPricing(
        reservationId, roomId, placeId,
        ReservationStatus.PENDING,
        timeSlot, timeSlotPrices, productPrices,
        totalPrice, LocalDateTime.now()
    );
  }

  // 상태 전이만 허용
  public void confirm() {
    if (status != ReservationStatus.PENDING) {
      throw new InvalidReservationStatusException(
          "PENDING 상태만 확정할 수 있습니다"
      );
    }
    this.status = ReservationStatus.CONFIRMED;
  }

  public void cancel() {
    if (status == ReservationStatus.CANCELLED) {
      throw new InvalidReservationStatusException(
          "이미 취소된 예약입니다"
      );
    }
    this.status = ReservationStatus.CANCELLED;
  }
}
```

#### 상품 가격 스냅샷 (Embeddable)

```java
@Embeddable
public class ProductPriceBreakdown {
  private final Long productId;        // 참조용 (FK 아님)
  private final String productName;    // 스냅샷 (상품명 변경되어도 유지)
  private final int quantity;
  private final Money unitPrice;       // 스냅샷 (단가)
  private final Money totalPrice;      // 계산된 총 가격
  private final PricingType pricingType;  // 전략 타입 스냅샷

  // 불변 객체
  public ProductPriceBreakdown(
      Long productId, String productName, int quantity,
      Money unitPrice, Money totalPrice, PricingType pricingType) {
    // 모든 필드 final + 생성자에서만 초기화
  }
}
```

**불변성의 이점:**
- 예약 후 가격 정책이 변경되어도 기존 예약 가격 유지
- 멀티스레드 환경에서 동기화 불필요
- 이력 추적 및 감사(Audit) 용이

---

## 아키텍처

### Hexagonal Architecture (Ports & Adapters)

도메인 로직을 외부 기술 스택으로부터 완전히 격리하여 테스트 가능성과 유지보수성을 극대화합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      Adapter Layer (외부)                       │
│                                                                 │
│  ┌──────────────────┐                    ┌──────────────────┐  │
│  │  REST Controller │                    │ JPA Repository   │  │
│  │  (Inbound)       │                    │  (Outbound)      │  │
│  └────────┬─────────┘                    └────────▲─────────┘  │
│           │                                       │            │
│           │ HTTP Request                          │ Persist    │
│           │                                       │            │
├───────────┼───────────────────────────────────────┼────────────┤
│           │         Application Layer             │            │
│           │                                       │            │
│           │    ┌──────────────────────────┐       │            │
│           │    │   Use Case Service       │       │            │
│           ├───►│  - CreateReservation     │───────┤            │
│           │    │  - CalculatePrice        │       │            │
│           │    │  - ConfirmReservation    │       │            │
│           │    └──────────┬───────────────┘       │            │
│           │               │ Use Domain Logic      │            │
├───────────┼───────────────┼───────────────────────┼────────────┤
│           │    Domain Layer (핵심 비즈니스 로직)   │            │
│           │               │                       │            │
│           │    ┌──────────▼───────────┐           │            │
│           └───►│   Aggregates         │◄──────────┘            │
│                │  - PricingPolicy     │                        │
│                │  - Product           │                        │
│                │  - ReservationPricing│                        │
│                │                      │                        │
│                │  Value Objects       │                        │
│                │  - Money, RoomId     │                        │
│                │  - TimeRange, ...    │                        │
│                └──────────────────────┘                        │
│                                                                 │
│  ┌──────────────────┐                    ┌──────────────────┐  │
│  │ Kafka Consumer   │                    │ Kafka Producer   │  │
│  │  (Inbound)       │                    │  (Outbound)      │  │
│  └──────────────────┘                    └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 의존성 규칙 (Dependency Rule)

```
┌────────────────────────────────────────┐
│         Adapter Layer (외부)           │  ─┐
│  의존성: Application Layer에만 의존    │   │
└────────────────────────────────────────┘   │
                  ↓ depends on               │ 의존성 방향
┌────────────────────────────────────────┐   │ (안쪽으로만)
│      Application Layer (중간)          │   │
│  의존성: Domain Layer에만 의존         │   │
└────────────────────────────────────────┘   │
                  ↓ depends on               │
┌────────────────────────────────────────┐   │
│        Domain Layer (핵심)             │   │
│  의존성: 없음 (순수 Java)              │  ─┘
└────────────────────────────────────────┘
```

**핵심 원칙:**
- Domain Layer는 어떤 레이어에도 의존하지 않음 (Spring, JPA, Kafka 모름)
- Application Layer는 Domain을 사용하지만 Adapter는 모름
- Adapter Layer는 Port 인터페이스를 구현하여 주입됨 (DIP)

### Domain-Driven Design (DDD)

#### Bounded Context

```
┌─────────────────────────────────────────────────────────┐
│         Reservation Pricing Service (이 서비스)         │
├─────────────────────────────────────────────────────────┤
│  Aggregates:                                            │
│  - PricingPolicy (가격 정책)                            │
│  - Product (추가상품)                                   │
│  - ReservationPricing (예약 가격 스냅샷)                │
│                                                         │
│  Shared Kernel:                                         │
│  - Money, RoomId, PlaceId, TimeRange                    │
└─────────────────────────────────────────────────────────┘
         ▲                              ▲
         │ RoomCreatedEvent            │ SlotReservedEvent
         │                              │
┌────────┴────────┐          ┌─────────┴────────┐
│  Room Service   │          │ Reservation Svc  │
│  (외부 서비스)   │          │  (외부 서비스)   │
└─────────────────┘          └──────────────────┘
```

#### Aggregate 경계

각 Aggregate는 트랜잭션 일관성 경계를 정의합니다:

**PricingPolicy Aggregate:**
- Root: PricingPolicy
- Entities: 없음
- Value Objects: TimeRangePrice, TimeRangePrices
- 불변 규칙: 같은 요일 내 시간대 중복 불가

**Product Aggregate:**
- Root: Product
- Entities: 없음
- Value Objects: PricingStrategy, ProductScope
- 불변 규칙: Scope에 따른 placeId/roomId 조합 검증

**ReservationPricing Aggregate:**
- Root: ReservationPricing
- Entities: 없음
- Value Objects: TimeSlotPriceBreakdown, ProductPriceBreakdown
- 불변 규칙: 생성 후 가격 정보 변경 불가

---

## 디자인 패턴

### 1. Factory Pattern

**목적:** 복잡한 객체 생성 로직을 캡슐화하고, 생성 시점에 비즈니스 규칙을 강제합니다.

#### 단순 Factory

```java
public class PricingPolicy {
  private PricingPolicy(...) { /* private 생성자 */ }

  // 기본 가격만 설정
  public static PricingPolicy create(
      RoomId roomId, PlaceId placeId,
      TimeSlot timeSlot, Money defaultPrice) {
    return new PricingPolicy(
        roomId, placeId, timeSlot,
        defaultPrice, TimeRangePrices.empty()
    );
  }

  // 시간대별 가격 포함
  public static PricingPolicy createWithTimeRangePrices(
      RoomId roomId, PlaceId placeId, TimeSlot timeSlot,
      Money defaultPrice, TimeRangePrices timeRangePrices) {
    return new PricingPolicy(
        roomId, placeId, timeSlot,
        defaultPrice, timeRangePrices
    );
  }
}
```

#### 다중 시나리오 Factory

```java
public class Product {
  // Scope별로 다른 Factory Method
  public static Product createPlaceScoped(...) { }
  public static Product createRoomScoped(...) { }
  public static Product createReservationScoped(...) { }
}
```

**장점:**
- 불가능한 상태 조합 컴파일 타임에 차단
- 메서드명으로 생성 의도 명확히 표현 (자기 문서화)
- 생성자 파라미터 순서 오류 방지

### 2. Strategy Pattern

**목적:** 알고리즘 군을 정의하고, 각각을 캡슐화하여 런타임에 교체 가능하도록 합니다.

#### 구현 예시

```java
// 전략 인터페이스
public interface PricingStrategy {
  Money calculatePrice(int quantity);
  PricingType getType();
}

// 구체 전략 1
public class InitialPlusAdditionalPricing implements PricingStrategy {
  private final Money initialPrice;
  private final Money additionalPrice;

  @Override
  public Money calculatePrice(int quantity) {
    if (quantity == 1) return initialPrice;
    return initialPrice.add(
        additionalPrice.multiply(quantity - 1)
    );
  }

  @Override
  public PricingType getType() {
    return PricingType.INITIAL_PLUS_ADDITIONAL;
  }
}

// 컨텍스트
public class Product {
  private PricingStrategy pricingStrategy;

  public Money calculateTotalPrice(int quantity) {
    return pricingStrategy.calculatePrice(quantity);
  }
}
```

**OCP (Open-Closed Principle) 준수:**
- 새로운 가격 전략 추가 시 기존 코드 수정 불필요
- PricingStrategy 인터페이스만 구현하면 됨

### 3. Value Object Pattern

**목적:** 불변 객체로 도메인 개념을 표현하고, 생성자에서 비즈니스 규칙을 강제합니다.

```java
// Money Value Object
public record Money(BigDecimal amount) {
  public Money {
    if (amount == null) {
      throw new IllegalArgumentException("금액은 null일 수 없습니다");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
    }
  }

  public static Money of(long amount) {
    return new Money(BigDecimal.valueOf(amount));
  }

  public Money add(Money other) {
    return new Money(this.amount.add(other.amount));
  }

  public Money multiply(int multiplier) {
    return new Money(
        this.amount.multiply(BigDecimal.valueOf(multiplier))
    );
  }
}
```

**장점:**
- 불변성으로 스레드 안전성 보장
- equals/hashCode 자동 구현 (record)
- 생성자에서 유효성 검증으로 항상 유효한 상태 보장

### 4. Repository Pattern

**목적:** 도메인과 데이터 접근 로직을 분리하여 도메인 계층의 순수성을 유지합니다.

```java
// Domain Layer: Port 정의
public interface PricingPolicyRepository {
  PricingPolicy findByRoomId(RoomId roomId);
  void save(PricingPolicy pricingPolicy);
  void delete(PricingPolicy pricingPolicy);
}

// Adapter Layer: Port 구현
@Repository
public class PricingPolicyRepositoryAdapter
    implements PricingPolicyRepository {

  private final PricingPolicyJpaRepository jpaRepository;

  @Override
  public PricingPolicy findByRoomId(RoomId roomId) {
    PricingPolicyEntity entity = jpaRepository
        .findByRoomId(roomId.value())
        .orElseThrow(() -> new PricingPolicyNotFoundException(...));

    return toDomain(entity);  // Entity → Domain 변환
  }

  @Override
  public void save(PricingPolicy pricingPolicy) {
    PricingPolicyEntity entity = toEntity(pricingPolicy);
    jpaRepository.save(entity);
  }
}
```

**DIP (Dependency Inversion Principle) 준수:**
- Domain이 Repository 인터페이스(Port)를 정의
- Adapter가 구현체를 주입
- Domain은 JPA, PostgreSQL 등의 세부사항을 모름

### 5. Domain Event Pattern

**목적:** 도메인 이벤트를 통해 Bounded Context 간 느슨한 결합을 유지합니다.

```java
// 도메인 이벤트
public record ReservationPricingCreatedEvent(
    Long reservationId,
    Long roomId,
    Long placeId,
    BigDecimal totalPrice,
    LocalDateTime calculatedAt
) {
  public static ReservationPricingCreatedEvent from(
      ReservationPricing reservationPricing) {
    return new ReservationPricingCreatedEvent(
        reservationPricing.getReservationId().value(),
        reservationPricing.getRoomId().value(),
        reservationPricing.getPlaceId().value(),
        reservationPricing.getTotalPrice().amount(),
        reservationPricing.getCalculatedAt()
    );
  }
}

// Application Service에서 이벤트 발행
@Service
public class ReservationPricingService {
  private final EventPublisher eventPublisher;

  @Transactional
  public ReservationPricingResponse createReservation(...) {
    ReservationPricing reservationPricing = ReservationPricing.create(...);
    repository.save(reservationPricing);

    // 이벤트 발행
    eventPublisher.publish(
        ReservationPricingCreatedEvent.from(reservationPricing)
    );

    return ReservationPricingResponse.from(reservationPricing);
  }
}
```

---

## 데이터베이스 스키마

### ERD

```
┌────────────────────────────┐
│   pricing_policies         │
├────────────────────────────┤
│ PK  id          BIGSERIAL  │
│     room_id     BIGINT     │──┐ 외부 서비스 참조
│     place_id    BIGINT     │  │
│     day_of_week VARCHAR(10)│  │
│     start_time  TIME       │  │
│     end_time    TIME       │  │
│     price       DECIMAL    │  │
└────────────────────────────┘  │
                                │
┌────────────────────────────┐  │
│       products             │  │
├────────────────────────────┤  │
│ PK  product_id  BIGINT     │ Snowflake ID
│     scope       VARCHAR(20)│  │ PLACE|ROOM|RESERVATION
│     place_id    BIGINT     │──┤
│     room_id     BIGINT     │──┤
│     name        VARCHAR    │  │
│     pricing_type VARCHAR   │  │
│     initial_price DECIMAL  │  │
│     total_quantity INTEGER │  │
└────────────────────────────┘  │
                                │
┌────────────────────────────┐  │
│  reservation_pricings      │  │
├────────────────────────────┤  │
│ PK  reservation_id BIGINT  │ Snowflake ID
│     room_id        BIGINT  │──┘
│     place_id       BIGINT  │
│     status         VARCHAR │  PENDING|CONFIRMED|CANCELLED
│     total_price    DECIMAL │
│     calculated_at  TIMESTAMP│
└───────────┬────────────────┘
            │ 1
            │
            │ *
┌───────────▼────────────────┐
│ reservation_pricing_slots  │ ElementCollection
├────────────────────────────┤
│ FK  reservation_id BIGINT  │
│ PK  slot_time   TIMESTAMP  │
│     slot_price  DECIMAL    │
└────────────────────────────┘

            │ *
┌───────────▼────────────────┐
│reservation_pricing_products│ ElementCollection
├────────────────────────────┤
│ FK  reservation_id BIGINT  │
│     product_id     BIGINT  │ 스냅샷 (FK 아님)
│     product_name   VARCHAR │ 스냅샷
│     quantity       INTEGER │
│     unit_price     DECIMAL │ 스냅샷
│     total_price    DECIMAL │
│     pricing_type   VARCHAR │ 스냅샷
└────────────────────────────┘
```

### Snowflake ID Generator

분산 환경에서 고유 ID를 생성하는 알고리즘입니다.

**구조 (64-bit Long):**
```
[Sign 1bit] [Timestamp 41bits] [Node ID 10bits] [Sequence 12bits]
```

**특징:**
- Custom Epoch: 2024-01-01T00:00:00Z
- 초당 최대 400만개 ID 생성 (단일 노드)
- 시간 기반 정렬 가능 (생성 순서 보장)
- 69년간 사용 가능

**사용 예시:**
```java
@Entity
public class ProductEntity {
  @Id
  @GeneratedValue(generator = "snowflake-id")
  @GenericGenerator(
      name = "snowflake-id",
      type = SnowflakeIdGenerator.class
  )
  private Long id;
}
```

### 인덱싱 전략

```sql
-- 가격 정책: 룸별 조회 최적화
CREATE INDEX idx_pricing_policies_room_id
    ON pricing_policies(room_id);

-- 추가상품: Scope + PlaceId 복합 조회
CREATE INDEX idx_products_scope_place_id
    ON products(scope, place_id)
    WHERE place_id IS NOT NULL;

-- 예약 가격: 룸 + 상태 복합 조회 (재고 관리 쿼리 최적화)
CREATE INDEX idx_reservation_pricings_room_status
    ON reservation_pricings(room_id, status);
```

### 도메인 규칙 (DB Constraints)

```sql
-- PricingPolicy: 시간대 중복 방지
ALTER TABLE pricing_policies
  ADD CONSTRAINT uq_room_day_time
  UNIQUE (room_id, day_of_week, start_time, end_time);

-- PricingPolicy: 시간 순서 검증
ALTER TABLE pricing_policies
  ADD CONSTRAINT chk_time_range
  CHECK (start_time < end_time);

-- Product: Scope에 따른 ID 조합 검증
ALTER TABLE products
  ADD CONSTRAINT chk_scope_ids CHECK (
    (scope = 'PLACE' AND place_id IS NOT NULL AND room_id IS NULL) OR
    (scope = 'ROOM' AND place_id IS NOT NULL AND room_id IS NOT NULL) OR
    (scope = 'RESERVATION' AND place_id IS NULL AND room_id IS NULL)
  );

-- ReservationPricing: 총 가격 0 이상
ALTER TABLE reservation_pricings
  ADD CONSTRAINT chk_total_price
  CHECK (total_price >= 0);
```

---

## API 엔드포인트

### 가격 정책 관리 API

**Base URL:** `/api/pricing-policies`

```http
GET    /{roomId}                           # 가격 정책 조회
PUT    /{roomId}/default-price             # 기본 가격 업데이트
PUT    /{roomId}/time-range-prices         # 시간대별 가격 업데이트
POST   /{targetRoomId}/copy                # 가격 정책 복사
```

### 추가상품 관리 API

**Base URL:** `/api/products`

```http
GET    /availability                       # 상품 재고 가용성 조회
  Query Params:
  - roomId: Long
  - placeId: Long
  - timeSlots: List<LocalDateTime>

Response:
{
  "availableProducts": [
    {
      "productId": 1,
      "name": "빔프로젝터",
      "scope": "PLACE",
      "totalQuantity": 5,
      "availableQuantity": 3,
      "pricingType": "SIMPLE_STOCK",
      "initialPrice": 30000
    }
  ]
}
```

### 예약 가격 관리 API

**Base URL:** `/api/reservations/pricing`

```http
POST   /                                   # 예약 생성 (가격 계산 및 저장)
POST   /preview                            # 가격 미리보기
PUT    /{reservationId}/confirm            # 예약 확정
PUT    /{reservationId}/cancel             # 예약 취소
PUT    /{reservationId}/products           # 예약 상품 업데이트
```

#### 예약 생성 요청 예시

```json
POST /api/reservations/pricing
{
  "reservationId": 1234567890,
  "roomId": 1,
  "placeId": 100,
  "timeSlots": [
    "2025-01-15T10:00:00",
    "2025-01-15T11:00:00",
    "2025-01-15T12:00:00"
  ],
  "products": [
    {
      "productId": 1,
      "quantity": 1
    },
    {
      "productId": 2,
      "quantity": 2
    }
  ]
}
```

#### 예약 생성 응답 예시

```json
{
  "reservationId": 1234567890,
  "roomId": 1,
  "placeId": 100,
  "status": "PENDING",
  "timeSlotPrices": [
    {
      "slotTime": "2025-01-15T10:00:00",
      "price": 10000
    },
    {
      "slotTime": "2025-01-15T11:00:00",
      "price": 10000
    },
    {
      "slotTime": "2025-01-15T12:00:00",
      "price": 15000
    }
  ],
  "productPrices": [
    {
      "productId": 1,
      "productName": "빔프로젝터",
      "quantity": 1,
      "unitPrice": 30000,
      "totalPrice": 30000,
      "pricingType": "SIMPLE_STOCK"
    },
    {
      "productId": 2,
      "productName": "화이트보드",
      "quantity": 2,
      "unitPrice": 10000,
      "totalPrice": 20000,
      "pricingType": "ONE_TIME"
    }
  ],
  "totalPrice": 85000,
  "calculatedAt": "2025-01-10T14:30:00"
}
```

---

## 기술 스택

### 언어 및 프레임워크

| 기술 | 버전 | 목적 |
|------|------|------|
| Java | 21 LTS | Virtual Threads 지원, 최신 언어 기능 |
| Spring Boot | 3.2.5 | 안정성과 최신 기능의 균형 |
| Spring Data JPA | 3.2.5 | 영속성 관리 |
| Spring Kafka | 3.2.5 | 이벤트 기반 통신 |

### 데이터베이스 및 메시징

| 기술 | 버전 | 목적 |
|------|------|------|
| PostgreSQL | 16 | 메인 데이터베이스, JSONB 지원 |
| Flyway | - | 데이터베이스 마이그레이션 |
| Apache Kafka | 3.6 | 이벤트 스트리밍 플랫폼 |

### 빌드 및 테스트

| 기술 | 버전 | 목적 |
|------|------|------|
| Gradle | 8.14 | 빌드 도구 |
| JUnit 5 | - | 단위 테스트 프레임워크 |
| Mockito | - | Mock 객체 생성 |
| AssertJ | - | Fluent Assertion |
| H2 Database | - | 통합 테스트용 인메모리 DB |
| Testcontainers | 1.19.3 | E2E 테스트 (PostgreSQL, Kafka) |

### 코드 품질

| 도구 | 목적 |
|------|------|
| CheckStyle | Google Java Style Guide 준수 |
| PMD | 코드 품질 분석 |
| SpotBugs | 버그 패턴 탐지 |

### CI/CD

| 도구 | 목적 |
|------|------|
| GitHub Actions | 자동화된 빌드 및 테스트 |
| Docker Compose | 로컬 개발 환경 (PostgreSQL, Kafka) |

---

## 테스팅

### 테스트 전략

#### 1. 단위 테스트 (Domain Layer)

순수 Java로 작성된 도메인 로직을 빠르게 검증합니다.

```java
@DisplayName("Money Value Object 테스트")
class MoneyTest {

  @Test
  @DisplayName("음수 금액으로 생성 시 예외 발생")
  void createWithNegativeAmount_ShouldThrowException() {
    // Given
    BigDecimal negativeAmount = BigDecimal.valueOf(-1000);

    // When & Then
    assertThatThrownBy(() -> new Money(negativeAmount))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("금액은 0 이상이어야 합니다");
  }

  @Test
  @DisplayName("두 금액을 더할 수 있다")
  void add_ShouldReturnSumOfTwoAmounts() {
    // Given
    Money money1 = Money.of(10000);
    Money money2 = Money.of(5000);

    // When
    Money result = money1.add(money2);

    // Then
    assertThat(result.amount()).isEqualByComparingTo("15000");
  }
}
```

#### 2. 통합 테스트 (Repository Layer)

JPA와 PostgreSQL의 실제 동작을 검증합니다.

```java
@DataJpaTest
@DisplayName("PricingPolicy Repository 통합 테스트")
class PricingPolicyRepositoryAdapterTest {

  @Autowired
  private PricingPolicyJpaRepository jpaRepository;

  @Test
  @DisplayName("RoomId로 가격 정책을 조회할 수 있다")
  void findByRoomId_ShouldReturnPricingPolicy() {
    // Given
    PricingPolicyEntity entity = PricingPolicyEntity.builder()
        .roomId(1L)
        .placeId(100L)
        .dayOfWeek("MONDAY")
        .startTime(LocalTime.of(9, 0))
        .endTime(LocalTime.of(12, 0))
        .price(BigDecimal.valueOf(10000))
        .build();
    jpaRepository.save(entity);

    // When
    Optional<PricingPolicyEntity> result = jpaRepository.findByRoomId(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getRoomId()).isEqualTo(1L);
  }
}
```

#### 3. E2E 테스트 (Testcontainers)

실제 PostgreSQL, Kafka 컨테이너로 전체 플로우를 검증합니다.

```java
@SpringBootTest
@Testcontainers
@DisplayName("예약 가격 계산 E2E 테스트")
class ReservationPricingE2ETest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16");

  @Container
  static KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

  @Test
  @DisplayName("예약 생성부터 확정까지 전체 플로우")
  void createAndConfirmReservation_ShouldWork() {
    // Given: 가격 정책 설정
    // When: 예약 생성 API 호출
    // Then: 가격 계산 검증
    // When: 예약 확정 API 호출
    // Then: 상태 전이 검증
  }
}
```

### 테스트 통계

- **총 테스트 수:** 501개
- **성공:** 486개
- **실패:** 15개 (수정 중)

### 테스트 분류

| 계층 | 테스트 수 | 설명 |
|------|-----------|------|
| Domain | 180+ | Value Object, Aggregate 로직 |
| Application | 120+ | Use Case 서비스 |
| Adapter (In) | 80+ | Controller 통합 테스트 |
| Adapter (Out) | 80+ | Repository 통합 테스트 |
| E2E | 40+ | Testcontainers 기반 |

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 계층 테스트
./gradlew test --tests "*.domain.*"

# 커버리지 리포트
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

---

## 실행 가이드

### 사전 요구사항

- Java 21 JDK
- Docker Desktop
- Git

### 1. 프로젝트 클론

```bash
git clone https://github.com/DDINGJOO/YE_YAK_HAE_YO_SERVICE_SERVER.git
cd YE_YAK_HAE_YO_SERVICE_SERVER
```

### 2. 인프라 실행 (Docker Compose)

```bash
cd springProject
docker-compose up -d
```

실행되는 서비스:
- PostgreSQL 16 (포트: 5432)
- Kafka 3.6 (포트: 9092)
- Zookeeper (포트: 2181)
- Kafka UI (포트: 8090)

상태 확인:
```bash
docker-compose ps
```

### 3. 환경 변수 설정

`.env` 파일이 자동 생성됩니다. 필요 시 수정:

```env
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=reservation_pricing_db
DATABASE_USER_NAME=postgres
DATABASE_PASSWORD=postgres

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=reservation-pricing-service

SPRING_PROFILES_ACTIVE=dev
```

### 4. 애플리케이션 실행

#### Gradle 사용
```bash
./gradlew bootRun
```

#### IDE 사용 (IntelliJ IDEA)
1. `SpringProjectApplication.java` 실행
2. Active profiles: `dev`

### 5. 동작 확인

#### Health Check
```bash
curl http://localhost:8080/actuator/health
```

#### Kafka UI 접속
```
http://localhost:8090
```

#### PostgreSQL 접속
```bash
docker exec -it reservation-pricing-postgres psql -U postgres -d reservation_pricing_db

# 테이블 목록 확인
\dt

# 마이그레이션 이력 확인
SELECT * FROM flyway_schema_history;
```

### 6. 코드 품질 검사

```bash
# 모든 품질 검사 실행
./gradlew codeQuality

# 개별 실행
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew spotbugsMain

# 리포트 확인
open build/reports/checkstyle/main.html
open build/reports/pmd/main.html
open build/reports/spotbugs/main/spotbugs.html
```

### 7. 빌드

```bash
# 전체 빌드 (테스트 + 코드 품질 검사 포함)
./gradlew build

# 빌드만 (테스트 제외)
./gradlew build -x test

# JAR 파일 확인
ls -la build/libs/
```

---

## 프로젝트 분석

### 아키텍처 강점

#### 1. 도메인 중심 설계 (Domain-Centric)

**강점:**
- 비즈니스 로직이 Spring, JPA 등의 기술 스택에 의존하지 않음
- 도메인 레이어는 순수 Java로 작성되어 테스트가 빠르고 쉬움
- 외부 기술 변경 시 도메인 로직 수정 불필요

**증거:**
- `domain/` 패키지에 Spring Annotation 없음
- Value Object는 record 클래스로 불변성 보장
- 180개 이상의 도메인 단위 테스트

#### 2. SOLID 원칙 준수

**SRP (Single Responsibility Principle):**
- 각 Aggregate가 단일 책임만 가짐
  - PricingPolicy: 가격 정책 관리
  - Product: 추가상품 관리
  - ReservationPricing: 예약 가격 스냅샷

**OCP (Open-Closed Principle):**
- PricingStrategy 인터페이스로 새로운 가격 전략 추가 가능
- 기존 코드 수정 없이 확장

**LSP (Liskov Substitution Principle):**
- 모든 PricingStrategy 구현체는 대체 가능

**ISP (Interface Segregation Principle):**
- Use Case별로 분리된 Port 인터페이스
  - QueryPricingPolicyUseCase
  - UpdateDefaultPriceUseCase
  - CopyPricingPolicyUseCase

**DIP (Dependency Inversion Principle):**
- Domain이 Repository 인터페이스 정의
- Adapter가 구현체 주입

#### 3. 불변성 (Immutability)

**강점:**
- 예약 가격 스냅샷이 불변 객체로 저장됨
- 가격 정책 변경이 과거 예약에 영향 없음
- 멀티스레드 환경에서 동기화 불필요

**구현:**
- Value Object는 모두 record 클래스 (final 필드)
- ReservationPricing의 가격 정보는 final
- 상태 전이만 허용 (PENDING → CONFIRMED → CANCELLED)

#### 4. 테스트 용이성

**강점:**
- Domain Layer는 Spring 없이 단위 테스트
- Testcontainers로 실제 PostgreSQL, Kafka 테스트
- 501개의 자동화된 테스트

**계층별 테스트 전략:**
- Domain: Mock 없이 순수 Java 테스트
- Application: Repository를 Mock으로 대체
- Adapter: Spring Context로 통합 테스트
- E2E: Testcontainers로 실제 환경 테스트

### 성능 최적화

#### 1. 인덱싱 전략

- 룸별 조회 최적화: `idx_pricing_policies_room_id`
- 재고 관리 쿼리 최적화: `idx_reservation_pricings_room_status`
- Partial Index 활용: PlaceId가 NULL이 아닐 때만 인덱스

#### 2. Snowflake ID Generator

- Auto Increment 대비 분산 환경에서 성능 우수
- 시간 기반 정렬로 인덱스 효율 향상
- 단일 노드에서 초당 400만개 ID 생성 가능

#### 3. 비정규화 (Denormalization)

- ReservationPricing에 PlaceId 중복 저장
- PlaceId 기준 조회 시 조인 없이 빠른 검색

---

## 향후 개선사항

### 단기 (1-2개월)

#### 1. 재고 동시성 제어 강화

**현재 상태:**
- 애플리케이션 레벨에서 재고 확인

**개선 방향:**
- Optimistic Lock 또는 Pessimistic Lock 적용
- Redis 분산 락 고려

**ADR 작성 예정:** ADR-002

#### 2. 이벤트 중복 처리 멱등성 보장

**현재 상태:**
- 기본적인 멱등성 처리

**개선 방향:**
- 이벤트 ID 기반 중복 처리 차단
- Outbox Pattern 적용 고려

**ADR 작성 예정:** ADR-004

### 중기 (3-6개월)

#### 1. 가격 정책 변경 이력 관리

**현재 상태:**
- 현재 가격 정책만 저장

**개선 방향:**
- Temporal Table 또는 별도 히스토리 테이블 추가
- 가격 정책 변경 이력 조회 API

**ADR 작성 예정:** ADR-003

#### 2. 캐싱 전략

**개선 방향:**
- Redis 캐시 도입
- 가격 정책 조회 성능 향상
- TTL 전략 수립

#### 3. API 문서화

**개선 방향:**
- Swagger UI 추가
- API 사용 예제 제공

### 장기 (6개월 이상)

#### 1. 동적 가격 정책 (Dynamic Pricing)

**개선 방향:**
- 수요 예측 기반 가격 자동 조정
- ML 모델 연동

#### 2. 성능 모니터링

**개선 방향:**
- Prometheus + Grafana
- 분산 추적 (Zipkin, Jaeger)
- 알림 시스템

#### 3. 데이터 아카이빙

**개선 방향:**
- 오래된 예약 가격 데이터 아카이빙
- 파티셔닝 전략 수립

---

## 문서

### 전체 문서 목록

- [docs/INDEX.md](docs/INDEX.md) - 전체 문서 인덱스 및 읽기 가이드
- [docs/INFO.md](docs/INFO.md) - 프로젝트 자동화 시스템 개요
- [docs/ISSUE_GUIDE.md](docs/ISSUE_GUIDE.md) - 이슈 작성 가이드
- [docs/PROJECT_SETUP.md](docs/PROJECT_SETUP.md) - 프로젝트 설정 가이드

### 요구사항

- [docs/requirements/PROJECT_REQUIREMENTS.md](docs/requirements/PROJECT_REQUIREMENTS.md) - 전체 요구사항 명세

### 아키텍처

- [docs/architecture/ARCHITECTURE_ANALYSIS.md](docs/architecture/ARCHITECTURE_ANALYSIS.md) - 아키텍처 패턴 비교
- [docs/architecture/DOMAIN_MODEL_DESIGN.md](docs/architecture/DOMAIN_MODEL_DESIGN.md) - 도메인 모델 설계
- [docs/architecture/TECH_STACK_ANALYSIS.md](docs/architecture/TECH_STACK_ANALYSIS.md) - 기술 스택 분석

### ADR (Architecture Decision Records)

- [docs/adr/ADR_001_ARCHITECTURE_DECISION.md](docs/adr/ADR_001_ARCHITECTURE_DECISION.md) - 최종 아키텍처 결정

### 기능별 상세 문서

- [docs/features/pricing-policy/](docs/features/pricing-policy/) - 가격 정책 기능
- [docs/features/product/](docs/features/product/) - 추가상품 기능
- [docs/features/reservation-pricing/](docs/features/reservation-pricing/) - 예약 가격 기능
- [docs/features/reservation/RESERVATION_FLOW.md](docs/features/reservation/RESERVATION_FLOW.md) - 예약 플로우
- [docs/features/event-handling/](docs/features/event-handling/) - 이벤트 처리

### 개발 가이드

- [springProject/PACKAGE_STRUCTURE.md](springProject/PACKAGE_STRUCTURE.md) - 패키지 구조 상세
- [springProject/DATABASE_SCHEMA.md](springProject/DATABASE_SCHEMA.md) - 데이터베이스 스키마
- [springProject/DOCKER_SETUP.md](springProject/DOCKER_SETUP.md) - Docker 설정 가이드

---

## 기여 가이드

1. 이슈 생성 ([ISSUE_GUIDE.md](docs/ISSUE_GUIDE.md) 참조)
2. 브랜치 생성 (`feature/이슈번호-작업내용`)
3. 코드 작성
4. 코드 품질 검사 (`./gradlew codeQuality`)
5. 테스트 실행 (`./gradlew test`)
6. PR 생성
7. CI 통과 확인
8. 코드 리뷰 후 머지

---

## 문제 해결

### Docker 컨테이너 재시작

```bash
# 전체 재시작
docker-compose restart

# 특정 서비스만
docker-compose restart postgres
docker-compose restart kafka
```

### 포트 충돌

`docker-compose.yml`에서 포트 변경:
```yaml
ports:
  - "15432:5432"  # PostgreSQL
  - "19092:9092"  # Kafka
```

### 데이터 완전 초기화

```bash
# 컨테이너 및 볼륨 삭제
docker-compose down -v

# 재시작
docker-compose up -d
```

### Gradle 캐시 문제

```bash
# Gradle 캐시 삭제
./gradlew clean --refresh-dependencies
```

---

## 라이선스

이 프로젝트는 상어 프로젝트입니다.

---

## 연락처

- Project Lead: @DDINGJOO
- Repository: https://github.com/DDINGJOO/YE_YAK_HAE_YO_SERVICE_SERVER

---

Last Updated: 2025-11-10