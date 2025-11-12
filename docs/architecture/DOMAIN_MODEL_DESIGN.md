# 도메인 모델 설계 대안 비교

## 개요

Hexagonal Architecture + DDD 아키텍처 선정 후, 구체적인 도메인 모델 설계 방안을 비교합니다.
3가지 핵심 기능별로 설계 대안을 제시하고 최적의 모델을 선택합니다.

---

## 1. 시간대별 가격 정책 모델

### 설계 대안 A: Embedded Collection (ElementCollection)

```java
@Entity
class PricingPolicy {
    @EmbeddedId
    private RoomId roomId;

    @Embedded
    private Money defaultPrice;

    @ElementCollection
    @CollectionTable(name = "time_range_prices")
    private List<TimeRangePrice> timeRangePrices;

    public void resetPrices(Money defaultPrice, List<TimeRangePrice> newPrices) {
        validateNoOverlap(newPrices);
        this.defaultPrice = defaultPrice;
        this.timeRangePrices.clear();
        this.timeRangePrices.addAll(newPrices);
    }
}

@Embeddable
record TimeRangePrice(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Money pricePerSlot
) {}
```

#### 장점
- Aggregate 일관성 보장 (Single Transaction)
- 중복 검증을 Aggregate 내부에서 처리
- JPA가 자동으로 cascade 처리

#### 단점
- ElementCollection 성능 이슈 (전체 삭제 후 재생성)
- 조회 시 N+1 문제 가능
- 대량 데이터 시 비효율

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 도메인 불변식 보장 | 5/5 | Aggregate 내부 검증 |
| 조회 성능 | 3/5 | Room당 최대 10개로 큰 문제 없음 |
| 수정 성능 | 2/5 | 전체 삭제 후 재생성 |
| 코드 복잡도 | 5/5 | 단순하고 명확 |
| **총점** | **15/20** | |

---

### 설계 대안 B: Separate Entity with Foreign Key

```java
@Entity
class PricingPolicy {
    @EmbeddedId
    private RoomId roomId;

    @Embedded
    private Money defaultPrice;

    // OneToMany가 아닌 Repository에서 조회
}

@Entity
class TimeRangePrice {
    @Id @GeneratedValue
    private Long id;

    @Embedded
    private RoomId roomId; // FK

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    @Embedded
    private Money pricePerSlot;
}

// Application Service에서 처리
@Service
class PricingPolicyService {
    void resetPrices(RoomId roomId, Money defaultPrice, List<TimeRangePrice> newPrices) {
        validateNoOverlap(newPrices); // Service 레벨 검증

        timeRangePriceRepository.deleteByRoomId(roomId);
        timeRangePriceRepository.saveAll(newPrices);

        pricingPolicyRepository.updateDefaultPrice(roomId, defaultPrice);
    }
}
```

#### 장점
- 조회 성능 우수 (DB 인덱스 활용)
- 부분 수정 가능

#### 단점
- **도메인 로직 유출**: 중복 검증이 Service에 위치
- Transaction 관리 복잡
- Aggregate 경계 모호

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 도메인 불변식 보장 | 2/5 | Service에 로직 유출 |
| 조회 성능 | 5/5 | DB 최적화 가능 |
| 수정 성능 | 5/5 | 부분 수정 가능 |
| 코드 복잡도 | 2/5 | Transaction 관리 복잡 |
| **총점** | **14/20** | |

---

### 설계 대안 C: Value Object Collection (추천)

```java
@Entity
class PricingPolicy {
    @EmbeddedId
    private RoomId roomId;

    @Embedded
    private Money defaultPrice;

    @Embedded
    private TimeRangePrices timeRangePrices; // Wrapper VO

    public void resetPrices(Money defaultPrice, List<TimeRangePrice> newPrices) {
        this.defaultPrice = requireNonNull(defaultPrice);
        this.timeRangePrices = TimeRangePrices.of(newPrices, timeSlot); // Factory
    }

    public Money calculatePrice(List<LocalDateTime> slots) {
        return timeRangePrices.calculateTotal(slots, defaultPrice, timeSlot);
    }
}

@Embeddable
class TimeRangePrices {
    @ElementCollection
    @CollectionTable(name = "time_range_prices")
    private List<TimeRangePrice> prices;

    private TimeRangePrices() {} // JPA

    private TimeRangePrices(List<TimeRangePrice> prices) {
        this.prices = List.copyOf(prices); // 불변
    }

    public static TimeRangePrices of(List<TimeRangePrice> prices, TimeSlot timeSlot) {
        validateNoOverlap(prices);
        validateTimeAlignment(prices, timeSlot);
        return new TimeRangePrices(prices);
    }

    private static void validateNoOverlap(List<TimeRangePrice> prices) {
        for (int i = 0; i < prices.size(); i++) {
            for (int j = i + 1; j < prices.size(); j++) {
                if (prices.get(i).overlaps(prices.get(j))) {
                    throw new TimeRangeOverlapException();
                }
            }
        }
    }

    public Money calculateTotal(List<LocalDateTime> slots, Money defaultPrice, TimeSlot timeSlot) {
        validateContinuousSlots(slots, timeSlot);

        return slots.stream()
            .map(slot -> findPriceForSlot(slot).orElse(defaultPrice))
            .reduce(Money.ZERO, Money::add);
    }

    private Optional<Money> findPriceForSlot(LocalDateTime slotStart) {
        DayOfWeek day = slotStart.getDayOfWeek();
        LocalTime time = slotStart.toLocalTime();

        return prices.stream()
            .filter(p -> p.dayOfWeek() == day)
            .filter(p -> !time.isBefore(p.startTime()) && time.isBefore(p.endTime()))
            .map(TimeRangePrice::pricePerSlot)
            .findFirst();
    }
}

@Embeddable
record TimeRangePrice(
    @Enumerated(EnumType.STRING)
    DayOfWeek dayOfWeek,

    LocalTime startTime,
    LocalTime endTime,

    @Embedded
    Money pricePerSlot
) {
    public boolean overlaps(TimeRangePrice other) {
        if (this.dayOfWeek != other.dayOfWeek) {
            return false;
        }

        return this.startTime.isBefore(other.endTime)
            && other.startTime.isBefore(this.endTime);
    }
}
```

#### 장점
- **도메인 로직 캡슐화**: 검증/계산 로직이 Value Object에 집중
- **불변성 보장**: Factory Method + List.copyOf()
- **의도 명확**: TimeRangePrices라는 도메인 개념 명시
- Aggregate 일관성 유지

#### 단점
- ElementCollection 성능 (하지만 최대 10개로 무시 가능)
- Wrapper 클래스로 인한 코드 증가

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 도메인 불변식 보장 | 5/5 | VO에 검증 로직 캡슐화 |
| 조회 성능 | 4/5 | 10개 이하로 문제없음 |
| 수정 성능 | 3/5 | 전체 교체지만 빈도 낮음 |
| 코드 복잡도 | 4/5 | 명확한 책임 분리 |
| **총점** | **16/20** | |

### 선택: 대안 C (Value Object Collection)

**선정 이유:**
1. 도메인 규칙을 Value Object에 완벽히 캡슐화
2. Room당 최대 10개 정책으로 성능 이슈 없음
3. 불변성 보장으로 Side Effect 방지
4. SOLID 원칙 준수 (SRP, OCP)

---

## 2. 추가상품 모델

### 설계 대안 A: Scope별 별도 Aggregate

```java
@Entity
class PlaceScopedProduct {
    @Id private ProductId id;
    @Embedded private PlaceId placeId;
    private String name;
    @Embedded private RentalPricing pricing;
    private int totalQuantity;
}

@Entity
class RoomScopedProduct {
    @Id private ProductId id;
    @Embedded private RoomId roomId;
    // 동일 구조
}

@Entity
class ReservationScopedProduct {
    @Id private ProductId id;
    private String name;
    @Embedded private Money price;
    private int totalQuantity;
}
```

#### 장점
- 타입 안전성 (컴파일 타임 검증)
- 각 Scope의 로직 명확히 분리

#### 단점
- 코드 중복 (3개 클래스)
- 공통 로직 추출 어려움
- 새로운 Scope 추가 시 클래스 증가

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 타입 안전성 | 5/5 | 각 Scope 타입 명확 |
| 코드 중복 | 2/5 | 높은 중복도 |
| 확장성 | 2/5 | 새 Scope마다 클래스 필요 |
| 유지보수성 | 2/5 | 공통 로직 변경 시 3곳 수정 |
| **총점** | **11/20** | |

---

### 설계 대안 B: 단일 Aggregate + Enum Strategy (추천)

```java
@Entity
class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProductScope scope;

    private Long scopeId; // PlaceId or RoomId (범용)

    private String name;

    @Embedded
    private PricingStrategy pricingStrategy;

    private int totalQuantity;

    // 가격 계산 (Snapshot 생성용)
    public ProductPriceBreakdown calculatePrice(int quantity, List<LocalDateTime> slots) {
        Money unitPrice = pricingStrategy.calculate(slots);
        Money totalPrice = unitPrice.multiply(quantity);

        return new ProductPriceBreakdown(
            new ProductId(id),
            name,
            quantity,
            unitPrice,
            totalPrice,
            pricingStrategy.getType()
        );
    }
}

enum ProductScope {
    PLACE,
    ROOM,
    RESERVATION;

    public boolean requiresTimeSlots() {
        return this == PLACE || this == ROOM;
    }
}

@Embeddable
class PricingStrategy {
    @Enumerated(EnumType.STRING)
    private PricingType type;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "initial_price"))
    private Money initialPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "additional_price"))
    private Money additionalPrice; // nullable for ONE_TIME

    public Money calculate(List<LocalDateTime> slots) {
        return switch (type) {
            case INITIAL_PLUS_ADDITIONAL -> {
                if (slots.isEmpty()) yield Money.ZERO;
                yield initialPrice.add(
                    additionalPrice.multiply(slots.size() - 1)
                );
            }
            case ONE_TIME -> slots.isEmpty() ? Money.ZERO : initialPrice;
            case SIMPLE_STOCK -> initialPrice; // 시간 무관
        };
    }

    public static PricingStrategy initialPlusAdditional(Money initial, Money additional) {
        return new PricingStrategy(PricingType.INITIAL_PLUS_ADDITIONAL, initial, additional);
    }

    public static PricingStrategy oneTime(Money price) {
        return new PricingStrategy(PricingType.ONE_TIME, price, null);
    }

    public static PricingStrategy simpleStock(Money price) {
        return new PricingStrategy(PricingType.SIMPLE_STOCK, price, null);
    }
}

enum PricingType {
    INITIAL_PLUS_ADDITIONAL, // 첫 슬롯 + 추가 시간
    ONE_TIME,                // 1회 대여료
    SIMPLE_STOCK;            // 단순 재고 (시간 무관)
}
```

#### 장점
- **코드 중복 제거**: 단일 클래스
- **확장성**: 새로운 Scope/PricingType은 Enum 추가만
- **Strategy Pattern**: OCP 준수
- Factory Method로 의도 표현

#### 단점
- scopeId가 Long 타입 (타입 안전성 낮음)
- Scope별 다른 검증 로직은 Domain Service 필요

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 타입 안전성 | 3/5 | scopeId가 범용 타입 |
| 코드 중복 | 5/5 | 중복 없음 |
| 확장성 | 5/5 | Enum 추가로 확장 |
| 유지보수성 | 5/5 | 단일 클래스 관리 |
| **총점** | **18/20** | |

---

### 설계 대안 C: 단일 Aggregate + Domain Service

대안 B와 동일하지만, 재고 검증은 Domain Service로 분리:

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

    private boolean checkPlaceScopedAvailability(...) {
        // 1. 해당 플레이스의 모든 PENDING/CONFIRMED 예약 조회
        List<ReservationPricing> overlapping =
            repository.findByPlaceIdAndTimeRange(
                product.getPlaceId(),
                start,
                end,
                List.of(PENDING, CONFIRMED)
            );

        // 2. 각 타임슬롯별 최대 사용량 계산
        int maxUsed = requestedSlots.stream()
            .mapToInt(slot -> calculateUsedAtSlot(overlapping, product.getId(), slot))
            .max()
            .orElse(0);

        // 3. 가용 재고 확인
        return maxUsed + requestedQuantity <= product.getTotalQuantity();
    }
}
```

#### 장점
- Aggregate는 순수 데이터 + 불변식
- 복잡한 조회 로직은 Service로 분리
- Repository 의존성을 Domain Service가 관리

#### 단점
- 일부 도메인 로직이 Service에 위치
- Domain Service가 Repository에 의존

#### 평가
- 대안 B와 동일한 **18/20점**
- 재고 검증 로직의 복잡도에 따라 선택

### 선택: 대안 B/C (단일 Aggregate + Strategy/Domain Service)

**선정 이유:**
1. 3가지 Scope의 본질은 "재고 확인 방식"과 "가격 계산 방식"의 차이
2. Strategy Pattern으로 OCP 준수
3. 새로운 Scope/Pricing 추가 용이
4. 코드 중복 최소화

**Domain Service 사용 여부:**
- 재고 검증 로직이 복잡하므로 **Domain Service 사용 권장**

---

## 3. 예약 가격 스냅샷 모델

### 설계 대안 A: Flat Structure (모든 정보 Embedded)

```java
@Entity
class ReservationPricing {
    @Id private Long reservationId;
    @Embedded private RoomId roomId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    // 시간대 가격 정보
    @ElementCollection
    @CollectionTable(name = "reservation_time_slot_prices")
    private Map<LocalDateTime, BigDecimal> slotPrices;

    // 상품 가격 정보
    @ElementCollection
    @CollectionTable(name = "reservation_product_prices")
    private List<ProductPriceInfo> productPrices;

    private BigDecimal totalPrice;
    private LocalDateTime calculatedAt;
}

@Embeddable
class ProductPriceInfo {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
```

#### 장점
- 단순한 구조
- 조회 성능 우수

#### 단점
- **타입 안전성 없음**: BigDecimal 직접 사용
- **도메인 개념 부재**: Money, ProductPriceBreakdown 등 VO 없음
- 가격 계산 로직 재사용 불가

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 도메인 표현력 | 2/5 | 원시 타입 사용 |
| 불변성 보장 | 3/5 | 변경 가능 |
| 재사용성 | 2/5 | VO 없음 |
| 코드 복잡도 | 5/5 | 매우 단순 |
| **총점** | **12/20** | |

---

### 설계 대안 B: Value Object 활용 (추천)

```java
@Entity
class ReservationPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "reservation_id"))
    private ReservationId reservationId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "room_id"))
    private RoomId roomId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Embedded
    private TimeSlotPriceBreakdown timeSlotBreakdown;

    @ElementCollection
    @CollectionTable(name = "reservation_product_prices")
    private List<ProductPriceBreakdown> productBreakdowns;

    @Embedded
    private Money totalPrice;

    private LocalDateTime calculatedAt;

    // Factory Method
    public static ReservationPricing calculate(
        ReservationId reservationId,
        RoomId roomId,
        PricingPolicy pricingPolicy,
        List<LocalDateTime> timeSlots,
        List<ProductRequest> productRequests,
        ProductRepository productRepository
    ) {
        // 1. 시간대별 가격 계산
        TimeSlotPriceBreakdown timeSlotBreakdown =
            pricingPolicy.calculatePriceBreakdown(timeSlots);

        // 2. 상품별 가격 계산
        List<ProductPriceBreakdown> productBreakdowns = productRequests.stream()
            .map(req -> {
                Product product = productRepository.findById(req.productId())
                    .orElseThrow();
                return product.calculatePrice(req.quantity(), timeSlots);
            })
            .toList();

        // 3. 총 가격 계산
        Money totalPrice = calculateTotal(timeSlotBreakdown, productBreakdowns);

        return new ReservationPricing(
            reservationId,
            roomId,
            ReservationStatus.PENDING,
            timeSlotBreakdown,
            productBreakdowns,
            totalPrice,
            LocalDateTime.now()
        );
    }

    private static Money calculateTotal(
        TimeSlotPriceBreakdown timeSlotBreakdown,
        List<ProductPriceBreakdown> productBreakdowns
    ) {
        Money timeSlotTotal = timeSlotBreakdown.getTotalPrice();
        Money productTotal = productBreakdowns.stream()
            .map(ProductPriceBreakdown::totalPrice)
            .reduce(Money.ZERO, Money::add);

        return timeSlotTotal.add(productTotal);
    }

    // 상태 전이
    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Already cancelled");
        }
        this.status = ReservationStatus.CANCELLED;
    }
}

// TimeSlotPriceBreakdown (Value Object)
@Embeddable
class TimeSlotPriceBreakdown {
    @ElementCollection
    @CollectionTable(name = "time_slot_prices")
    @MapKeyColumn(name = "slot_time")
    @Column(name = "price_amount")
    private Map<LocalDateTime, BigDecimal> slotPrices;

    @Enumerated(EnumType.STRING)
    private TimeSlot timeSlot;

    public Money getTotalPrice() {
        return slotPrices.values().stream()
            .map(Money::new)
            .reduce(Money.ZERO, Money::add);
    }
}

// ProductPriceBreakdown (Value Object)
@Embeddable
record ProductPriceBreakdown(
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "product_id"))
    ProductId productId,

    String productName,
    int quantity,

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price"))
    Money unitPrice,

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_price"))
    Money totalPrice,

    @Enumerated(EnumType.STRING)
    PricingType pricingType
) {
    // Immutable snapshot
}
```

#### 장점
- **강한 타입**: Money, ProductId 등 도메인 타입 사용
- **불변성**: VO로 구성
- **의도 명확**: TimeSlotPriceBreakdown, ProductPriceBreakdown
- **재사용성**: VO를 다른 곳에서도 활용 가능
- Factory Method로 생성 로직 캡슐화

#### 단점
- 코드량 증가
- JPA 매핑 복잡도

#### 평가

| 항목 | 점수 | 근거 |
|------|------|------|
| 도메인 표현력 | 5/5 | 명확한 도메인 개념 |
| 불변성 보장 | 5/5 | VO 불변 |
| 재사용성 | 5/5 | VO 재사용 가능 |
| 코드 복잡도 | 3/5 | JPA 매핑 복잡 |
| **총점** | **18/20** | |

### 선택: 대안 B (Value Object 활용)

**선정 이유:**
1. **형상관리 정확성**: 불변 VO로 가격 변경 방지
2. **도메인 표현력**: Money, ProductPriceBreakdown으로 의도 명확
3. **타입 안전성**: 컴파일 타임 검증
4. DDD 원칙 준수

---

## 최종 도메인 모델 구조

### Aggregate Root 3개

```
1. PricingPolicy (시간대별 가격 정책)
   └─ TimeRangePrices (VO)
       └─ List<TimeRangePrice> (VO)

2. Product (추가상품)
   └─ PricingStrategy (VO)

3. ReservationPricing (예약 가격 스냅샷)
   ├─ TimeSlotPriceBreakdown (VO)
   └─ List<ProductPriceBreakdown> (VO)
```

### Domain Service 1개

```
ProductAvailabilityService
├─ checkPlaceScopedAvailability()
├─ checkRoomScopedAvailability()
└─ checkReservationScopedAvailability()
```

### Shared Value Objects

```
Money (금액)
RoomId (룸 식별자)
PlaceId (플레이스 식별자)
ProductId (상품 식별자)
ReservationId (예약 식별자)
```

---

## 설계 품질 검증

### SOLID 원칙 준수 확인

#### Single Responsibility Principle
- PricingPolicy: 시간대별 가격 정책 관리
- Product: 추가상품 정보 관리
- ReservationPricing: 예약 가격 스냅샷 관리
- ProductAvailabilityService: 재고 가용성 검증

#### Open-Closed Principle
- PricingStrategy: 새로운 가격 정책 추가 시 Enum 확장
- ProductScope: 새로운 Scope 추가 시 Enum 확장
- Domain Service: switch expression으로 확장 가능

#### Liskov Substitution Principle
- Value Object: 모두 불변 객체로 대체 가능

#### Interface Segregation Principle
- Repository Port: 각 Aggregate별 독립적 인터페이스

#### Dependency Inversion Principle
- Domain이 Infrastructure에 의존하지 않음
- Repository Port를 통한 의존성 역전

---

## 결론

**선정된 설계:**

1. **시간대별 가격**: Value Object Collection (TimeRangePrices)
   - 도메인 로직 캡슐화
   - 불변성 보장
   - 성능 충분

2. **추가상품**: 단일 Aggregate + Strategy + Domain Service
   - 코드 중복 제거
   - OCP 준수
   - 복잡한 재고 검증은 Domain Service

3. **예약 가격**: Value Object 활용
   - 형상관리 정확성
   - 타입 안전성
   - 도메인 표현력

**핵심 설계 원칙:**
- Aggregate로 불변식 보장
- Value Object로 도메인 개념 표현
- Domain Service로 복잡한 로직 분리
- Factory Method로 생성 로직 캡슐화

다음 단계: **기술 스택 분석 및 선정**

---

**Last Updated**: 2025-11-12
