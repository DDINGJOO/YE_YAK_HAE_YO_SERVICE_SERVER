# PricingPolicy 도메인 모델

## Aggregate: PricingPolicy

### 구조
```
PricingPolicy (Aggregate Root)
├── RoomId (Aggregate ID)
├── PlaceId
├── TimeSlot (HOUR | HALFHOUR)
├── Money defaultPrice
└── TimeRangePrices
    └── List<TimeRangePrice>
        ├── DayOfWeek
        ├── TimeRange
        └── Money pricePerSlot
```

### 주요 책임
1. **가격 정책 관리**
   - 기본 가격 설정/변경
   - 시간대별 가격 설정/재설정
   - 다른 룸의 정책 복사 (같은 플레이스 내에서만)

2. **가격 계산**
   - 예약 기간에 대한 슬롯별 가격 계산
   - 시간대별 가격 우선 적용, 없으면 기본 가격 적용
   - PriceBreakdown 생성 (슬롯별 가격 상세)

3. **불변식 유지**
   - 시간대별 가격 중복 방지
   - 음수 가격 방지
   - 같은 플레이스 제약 (복사 시)

## Value Objects

### TimeRangePrices
**역할**: 시간대별 가격 컬렉션 관리

**불변식**:
- 동일 요일에 겹치는 시간대 불가
- 불변 객체 (방어적 복사)

**주요 메서드**:
- `findPriceForSlot(DayOfWeek, LocalTime)`: 특정 슬롯의 가격 조회
- `isEmpty()`: 시간대별 가격 존재 여부
- `size()`: 시간대별 가격 개수

### TimeRangePrice (Record)
**역할**: 단일 시간대별 가격 정보

**구성**:
- `DayOfWeek dayOfWeek`: 요일
- `TimeRange timeRange`: 시간대 (시작~종료)
- `Money pricePerSlot`: 슬롯당 가격

**검증**:
- null 불가
- overlaps() 메서드로 겹침 검증

### SlotPrice (Record)
**역할**: 계산된 슬롯별 가격 정보

**구성**:
- `LocalDateTime slotStart`: 슬롯 시작 시각
- `Money price`: 해당 슬롯 가격

### PriceBreakdown
**역할**: 예약 기간의 가격 분석 정보

**구성**:
- `List<SlotPrice> slotPrices`: 슬롯별 가격 목록
- `Money totalPrice`: 총 가격
- `int slotCount`: 총 슬롯 개수

## 비즈니스 규칙

### 1. 가격 계산 우선순위
1. 해당 요일/시간대의 시간대별 가격 (있는 경우)
2. 기본 가격 (fallback)

### 2. 시간 슬롯 분할
- `TimeSlot.HOUR`: 1시간 단위
- `TimeSlot.HALFHOUR`: 30분 단위
- 예약 시작 시각부터 슬롯 단위로 분할

### 3. 가격 변경
- `updateDefaultPrice()`: 기본 가격만 변경
- `resetPrices()`: 시간대별 가격 전체 재설정 (기존 삭제 후 새로 추가)

### 4. 가격 정책 복사
- 같은 `placeId`를 가진 룸 간에만 복사 가능
- 원본 룸의 기본 가격 및 시간대별 가격을 대상 룸으로 복사
- 다른 플레이스 간 복사 시 `CannotCopyDifferentPlaceException` 발생

## 예시

### 기본 가격만 있는 정책
```java
PricingPolicy policy = PricingPolicy.create(
    RoomId.of(1L),
    PlaceId.of(100L),
    TimeSlot.HOUR,
    Money.of(new BigDecimal("10000"))
);
// 모든 시간대에 10,000원 적용
```

### 시간대별 가격이 있는 정책
```java
TimeRangePrices timeRangePrices = TimeRangePrices.of(List.of(
    new TimeRangePrice(
        DayOfWeek.MONDAY,
        TimeRange.of(LocalTime.of(18, 0), LocalTime.of(22, 0)),
        Money.of(new BigDecimal("15000"))
    ),
    new TimeRangePrice(
        DayOfWeek.SATURDAY,
        TimeRange.of(LocalTime.of(9, 0), LocalTime.of(18, 0)),
        Money.of(new BigDecimal("20000"))
    )
));

PricingPolicy policy = PricingPolicy.createWithTimeRangePrices(
    RoomId.of(1L),
    PlaceId.of(100L),
    TimeSlot.HOUR,
    Money.of(new BigDecimal("10000")),
    timeRangePrices
);
// 월요일 18:00~22:00 -> 15,000원
// 토요일 09:00~18:00 -> 20,000원
// 그 외 시간대 -> 10,000원
```

### 가격 계산 예시
```java
LocalDateTime start = LocalDateTime.of(2025, 1, 6, 18, 0); // 월요일 18:00
LocalDateTime end = LocalDateTime.of(2025, 1, 6, 20, 0);   // 월요일 20:00

PriceBreakdown breakdown = policy.calculatePriceBreakdown(start, end);
// slotPrices: [
//   SlotPrice(2025-01-06T18:00, 15000원),
//   SlotPrice(2025-01-06T19:00, 15000원)
// ]
// totalPrice: 30000원
// slotCount: 2
```

### 가격 정책 복사 예시
```java
// 원본 정책 (Room 1)
PricingPolicy sourcePolicy = PricingPolicy.createWithTimeRangePrices(
    RoomId.of(1L),
    PlaceId.of(100L),  // 같은 placeId
    TimeSlot.HOUR,
    Money.of(new BigDecimal("30000")),
    timeRangePrices
);

// 대상 정책 (Room 2)
PricingPolicy targetPolicy = PricingPolicy.create(
    RoomId.of(2L),
    PlaceId.of(100L),  // 같은 placeId (필수)
    TimeSlot.HOUR,
    Money.of(BigDecimal.ZERO)
);

// 복사 실행
targetPolicy.updateDefaultPrice(sourcePolicy.getDefaultPrice());
targetPolicy.resetPrices(sourcePolicy.getTimeRangePrices());
// Room 2는 이제 Room 1과 동일한 가격 정책을 가짐
```

## Use Cases (Issue #10)

### CreatePricingPolicyUseCase
- RoomCreatedEvent 수신 시 기본 정책 자동 생성
- 기본 가격 0원으로 초기화

### GetPricingPolicyUseCase
- RoomId로 가격 정책 조회
- 정책이 없으면 `PricingPolicyNotFoundException` 발생

### UpdatePricingPolicyUseCase
- 기본 가격 업데이트: `updateDefaultPrice(roomId, money)`
- 시간대별 가격 재설정: `updateTimeRangePrices(roomId, prices)`

### CopyPricingPolicyUseCase
- 가격 정책 복사: `copyFromRoom(targetRoomId, sourceRoomId)`
- 같은 PlaceId 검증
- 원본 및 대상 정책 존재 확인

## 테스트 커버리지
- 도메인: 158개 단위 테스트
- 서비스: 12개 단위 테스트 (생성, 조회, 업데이트, 복사)
- 컨트롤러: 11개 통합 테스트 (API 엔드포인트)
- 생성, 가격 변경, 가격 계산, 복사, 검증 로직 모두 커버

---

**Last Updated**: 2025-11-12
