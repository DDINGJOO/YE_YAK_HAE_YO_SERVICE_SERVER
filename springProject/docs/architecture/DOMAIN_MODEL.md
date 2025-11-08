# 도메인 모델

## 개요

본 프로젝트는 Domain-Driven Design(DDD) 원칙을 따라 도메인 모델을 설계합니다.

## Bounded Context

현재 구현된 Bounded Context:

1. **Pricing Policy Context**: 가격 정책 관리
2. **Product Context**: 추가상품 관리 (예정)
3. **Reservation Pricing Context**: 예약 가격 계산 (예정)

## Shared Kernel

모든 Bounded Context에서 공유하는 공통 Value Objects

### Money

금액을 표현하는 Value Object

**위치**: `domain/shared/Money.java`

**속성**:
- `BigDecimal amount`: 금액 (소수점 이하 2자리)

**특징**:
- Immutable
- 음수 금액 불가
- 사칙연산 메서드 제공

**사용 예시**:
```java
Money price = Money.of(new BigDecimal("30000"));
Money doubled = price.multiply(2);
Money total = price.add(Money.of(new BigDecimal("10000")));

boolean isPositive = price.isPositive();
boolean isGreaterThan = price.isGreaterThan(Money.ZERO);
```

### RoomId

룸 식별자

**위치**: `domain/shared/RoomId.java`

**속성**:
- `Long value`: 룸 ID

**특징**:
- Immutable
- null 불가

### PlaceId

플레이스 식별자

**위치**: `domain/shared/PlaceId.java`

**속성**:
- `Long value`: 플레이스 ID

**특징**:
- Immutable
- null 불가

### TimeSlot

시간 단위 Enum

**위치**: `domain/shared/TimeSlot.java`

**값**:
- `HOUR`: 시간 단위
- `DAY`: 일 단위
- `HALF_DAY`: 반일 단위

**사용 예시**:
```java
TimeSlot slot = TimeSlot.HOUR;
```

### TimeRange

시간 범위를 표현하는 Value Object

**위치**: `domain/shared/TimeRange.java`

**속성**:
- `LocalTime startTime`: 시작 시간
- `LocalTime endTime`: 종료 시간

**특징**:
- Immutable
- startTime < endTime 검증
- 시간 중복 검사 메서드 제공

**사용 예시**:
```java
TimeRange morning = TimeRange.of(
    LocalTime.of(9, 0),
    LocalTime.of(12, 0)
);

TimeRange afternoon = TimeRange.of(
    LocalTime.of(13, 0),
    LocalTime.of(18, 0)
);

boolean overlaps = morning.overlaps(afternoon); // false
```

### DayOfWeek

요일 Enum (도메인 전용)

**위치**: `domain/shared/DayOfWeek.java`

**값**:
- `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

**특징**:
- `java.time.DayOfWeek` 변환 메서드 제공
- 도메인 로직에서 사용하기 위한 전용 Enum

## PricingPolicy Bounded Context

### Aggregate: PricingPolicy

룸별 가격 정책을 관리하는 Aggregate Root

**위치**: `domain/pricingpolicy/PricingPolicy.java`

#### 속성

- `RoomId roomId`: 룸 식별자 (Aggregate ID)
- `PlaceId placeId`: 플레이스 식별자
- `TimeSlot timeSlot`: 시간 단위
- `Money defaultPrice`: 기본 가격
- `TimeRangePrices timeRangePrices`: 시간대별 가격 목록

#### 생성 방법

**Factory Method 1: 기본 정책 생성**
```java
PricingPolicy policy = PricingPolicy.create(
    RoomId.of(1L),
    PlaceId.of(100L),
    TimeSlot.HOUR,
    Money.of(new BigDecimal("30000"))
);
```

**Factory Method 2: 시간대별 가격 포함 생성**
```java
List<TimeRangePrice> prices = List.of(
    new TimeRangePrice(
        DayOfWeek.MONDAY,
        TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
        Money.of(new BigDecimal("50000"))
    )
);

PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
    RoomId.of(1L),
    PlaceId.of(100L),
    TimeSlot.HOUR,
    Money.of(new BigDecimal("30000")),
    TimeRangePrices.of(prices)
);
```

#### 비즈니스 메서드

**1. 기본 가격 업데이트**
```java
policy.updateDefaultPrice(Money.of(new BigDecimal("40000")));
```

**2. 시간대별 가격 재설정**
```java
policy.resetPrices(newTimeRangePrices);
```

**3. 가격 조회**
```java
Money defaultPrice = policy.getDefaultPrice();
TimeRangePrices timeRangePrices = policy.getTimeRangePrices();
```

#### 불변식 (Invariants)

1. **roomId는 null일 수 없다**
2. **placeId는 null일 수 없다**
3. **defaultPrice는 0 이상이어야 한다**
4. **시간대별 가격은 중복될 수 없다** (TimeRangePrices에서 검증)

### Value Object: TimeRangePrice

특정 요일과 시간대의 가격 정보

**위치**: `domain/pricingpolicy/TimeRangePrice.java`

**구조**: Record class (Immutable)

**속성**:
- `DayOfWeek dayOfWeek`: 요일
- `TimeRange timeRange`: 시간 범위
- `Money pricePerSlot`: 시간 단위당 가격

**사용 예시**:
```java
TimeRangePrice mondayMorning = new TimeRangePrice(
    DayOfWeek.MONDAY,
    TimeRange.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
    Money.of(new BigDecimal("50000"))
);

DayOfWeek day = mondayMorning.dayOfWeek();
Money price = mondayMorning.pricePerSlot();
```

### Value Object: TimeRangePrices

시간대별 가격 목록을 관리하는 컬렉션 Value Object

**위치**: `domain/pricingpolicy/TimeRangePrices.java`

**속성**:
- `List<TimeRangePrice> prices`: 시간대별 가격 목록

**특징**:
- Immutable
- 시간 중복 검증
- 빈 리스트 허용

**Factory Method**:
```java
List<TimeRangePrice> priceList = List.of(...);
TimeRangePrices prices = TimeRangePrices.of(priceList);
```

**비즈니스 메서드**:
```java
List<TimeRangePrice> allPrices = prices.getPrices();
boolean isEmpty = prices.isEmpty();
```

**불변식**:
- 같은 요일의 시간대가 중복되지 않아야 함

### 도메인 예외

**위치**: `domain/pricingpolicy/exception/`

#### PricingPolicyException

모든 가격 정책 예외의 기반 클래스

**특징**:
- Abstract class
- `PricingPolicyErrorCode` 보유
- HTTP Status 자동 매핑

#### PricingPolicyNotFoundException

가격 정책을 찾을 수 없을 때

**ErrorCode**: `PRICING_001`
**HTTP Status**: `404 Not Found`

**사용 예시**:
```java
throw new PricingPolicyNotFoundException(
    "Pricing policy not found for roomId: " + roomId
);
```

#### CannotCopyDifferentPlaceException

다른 플레이스 간 정책 복사 시도 시

**ErrorCode**: `PRICING_003`
**HTTP Status**: `400 Bad Request`

**사용 예시**:
```java
if (!sourcePolicy.getPlaceId().equals(targetPolicy.getPlaceId())) {
    throw new CannotCopyDifferentPlaceException(
        "Cannot copy pricing policy between different places"
    );
}
```

#### InvalidTimeRangeException

잘못된 시간 범위

**ErrorCode**: `PRICING_004`
**HTTP Status**: `400 Bad Request`

#### TimeRangeOverlapException

시간 범위 중복

**ErrorCode**: `PRICING_005`
**HTTP Status**: `400 Bad Request`

## 도메인 이벤트

### RoomCreatedEvent

룸이 생성되었을 때 발행되는 이벤트

**발행자**: Room 서비스 (외부)
**구독자**: PricingPolicy 서비스

**Payload**:
```json
{
  "eventType": "RoomCreated",
  "roomId": 1,
  "placeId": 100,
  "timeSlot": "HOUR"
}
```

**처리 플로우**:
1. Kafka를 통해 이벤트 수신
2. `EventConsumer`가 이벤트 파싱
3. `RoomCreatedEventHandler`가 비즈니스 로직 실행
4. `CreatePricingPolicyService`를 통해 기본 정책 생성

**생성되는 기본 정책**:
- defaultPrice: 0원
- timeRangePrices: 빈 목록

## 비즈니스 규칙

### 1. 가격 정책 자동 생성

**규칙**: 룸 생성 시 해당 룸의 기본 가격 정책이 자동 생성됨

**이유**: 모든 룸은 가격 정책을 가져야 함 (예약 가격 계산을 위해)

**구현**: RoomCreatedEvent 리스너

### 2. 같은 플레이스 제약

**규칙**: 가격 정책 복사는 같은 placeId를 가진 룸 간에만 가능

**이유**: 플레이스마다 가격 정책이 다를 수 있으므로, 다른 플레이스의 정책을 무단 복사하는 것을 방지

**구현**:
```java
public PricingPolicy copyFromRoom(RoomId targetRoomId, RoomId sourceRoomId) {
    PricingPolicy source = findById(sourceRoomId);
    PricingPolicy target = findById(targetRoomId);

    if (!source.getPlaceId().equals(target.getPlaceId())) {
        throw new CannotCopyDifferentPlaceException(...);
    }

    target.updateDefaultPrice(source.getDefaultPrice());
    target.resetPrices(source.getTimeRangePrices());

    return repository.save(target);
}
```

### 3. 시간대별 가격 우선순위

**규칙**: 특정 시간대에 시간대별 가격이 설정되어 있으면 기본 가격보다 우선 적용

**예시**:
- 기본 가격: 30,000원
- 월요일 09:00-18:00: 50,000원

월요일 10시 예약 시 50,000원 적용

### 4. 시간 범위 중복 불가

**규칙**: 같은 요일에 시간대가 중복되는 가격을 설정할 수 없음

**검증**: `TimeRangePrices.of()` 메서드에서 검증

**예시** (허용되지 않음):
```java
// 월요일 09:00-12:00: 40,000원
// 월요일 11:00-14:00: 50,000원  <- 중복!
```

### 5. 음수 가격 불가

**규칙**: 모든 가격은 0 이상이어야 함

**검증**: `Money` Value Object에서 검증

## 설계 결정사항

### 1. Aggregate 경계

**결정**: PricingPolicy를 Aggregate Root로 선정

**이유**:
- 가격 정책은 독립적인 생명주기를 가짐
- 시간대별 가격(TimeRangePrice)은 PricingPolicy 없이 존재할 수 없음
- 트랜잭션 일관성이 Aggregate 내에서만 보장되면 충분

### 2. TimeRangePrices를 별도 Value Object로 분리

**결정**: List<TimeRangePrice> 대신 TimeRangePrices 사용

**이유**:
- 시간 중복 검증 로직을 캡슐화
- 불변성 보장
- 빈 리스트와 null을 명확히 구분

### 3. Record 사용

**결정**: TimeRangePrice를 Record로 구현

**이유**:
- Immutable 보장
- 간결한 코드
- Equals/HashCode 자동 생성
- Java 17+ 지원

### 4. Shared Kernel 분리

**결정**: Money, RoomId 등을 shared 패키지로 분리

**이유**:
- 여러 Bounded Context에서 공통 사용
- 일관된 타입 사용 (Long 대신 RoomId)
- 타입 안정성 향상

### 5. 도메인별 예외 구조

**결정**: 각 Bounded Context는 독립적인 예외 계층 보유

**이유**:
- 도메인 간 결합도 감소
- 예외 메시지 및 코드 관리 용이
- 헥사고날 아키텍처 원칙 준수

## 향후 확장 (Product Context)

### Product Aggregate

추가상품 관리를 위한 Aggregate (Issue #11 예정)

**주요 속성**:
- `ProductId`: 상품 ID
- `ProductScope`: 상품 범위 (PLACE, ROOM, RESERVATION)
- `Long scopeId`: 범위 ID (placeId, roomId, reservationId)
- `String name`: 상품명
- `PricingStrategy`: 가격 전략
- `int totalQuantity`: 총 재고

**PricingStrategy**:
- `INITIAL_PLUS_ADDITIONAL`: 초기 + 추가 요금
- `ONE_TIME`: 1회 대여료
- `SIMPLE_STOCK`: 단순 재고 (시간 무관)

## 관련 문서

- [아키텍처 개요](ARCHITECTURE.md)
- [API 문서](../features/pricing-policy/API.md)
