# ReservationPricing 도메인 모델

## Aggregate: ReservationPricing

### 구조
```
ReservationPricing (Aggregate Root)
├── ReservationId (Aggregate ID)
├── RoomId
├── ReservationStatus (PENDING | CONFIRMED | CANCELLED)
├── TimeSlotPriceBreakdown
│   ├── Map<LocalDateTime, Money> slotPrices
│   └── TimeSlot (HOUR | HALFHOUR)
├── List<ProductPriceBreakdown>
│   ├── ProductId
│   ├── String productName
│   ├── int quantity
│   ├── Money unitPrice
│   ├── Money totalPrice
│   └── PricingType
├── Money totalPrice
├── LocalDateTime calculatedAt
└── LocalDateTime expiresAt
```

### 주요 책임
1. **예약 가격 스냅샷 관리**
   - 예약 시점의 가격 정보를 불변 스냅샷으로 저장
   - 시간대별 가격 + 상품별 가격 계산
   - 가격 일관성 검증 (시간대 총액 + 상품 총액 = 전체 총액)

2. **예약 상태 관리**
   - PENDING: 예약 대기 (결제 전)
   - CONFIRMED: 예약 확정 (결제 완료)
   - CANCELLED: 예약 취소
   - 상태 전이 규칙 준수

3. **상품 업데이트**
   - PENDING 상태에서만 상품 추가/변경 가능
   - 상품 변경 시 가격 재계산 및 calculatedAt 갱신

4. **만료 시간 관리**
   - PENDING 상태의 타임아웃 관리
   - expiresAt 시각 초과 시 자동 취소 대상

5. **상품 재고 관리 통합** (Issue #157, #164)
   - 예약 생성 시 상품 재고 예약 (ProductRepository.reserveQuantity)
   - 예약 취소/환불 시 상품 재고 해제 (ProductRepository.releaseQuantity)
   - RESERVATION Scope: totalQuantity에서 차감/복원
   - ROOM/PLACE Scope: product_time_slot_inventory 테이블에서 시간대별 재고 관리

## Value Objects

### TimeSlotPriceBreakdown
**역할**: 시간대별 가격 내역 관리

**구성**:
- `Map<LocalDateTime, Money> slotPrices`: 시간 슬롯별 가격
- `TimeSlot timeSlot`: 시간 단위 (HOUR/HALFHOUR)

**불변식**:
- slotPrices는 null이거나 비어있으면 안됨
- 모든 슬롯 가격은 0 이상이어야 함
- 불변 객체 (방어적 복사)

**주요 메서드**:
- `getTotalPrice()`: 모든 슬롯 가격의 합계
- `getSlotCount()`: 슬롯 개수
- `getSlotPrices()`: 슬롯별 가격 목록 (불변)

### ProductPriceBreakdown (Record)
**역할**: 상품별 가격 스냅샷

**구성**:
- `ProductId productId`: 상품 ID
- `String productName`: 상품명
- `int quantity`: 수량
- `Money unitPrice`: 단가
- `Money totalPrice`: 총 가격 (단가 × 수량)
- `PricingType pricingType`: 가격 책정 방식

**검증**:
- 모든 필드 null 불가
- quantity는 1 이상
- unitPrice와 totalPrice는 0 이상
- 가격 일관성: unitPrice × quantity = totalPrice (PricingType에 따라 다름)

**생성 예시**:
```java
ProductPriceBreakdown breakdown = Product.calculatePrice(quantity);
// Product의 PricingStrategy에 따라 자동 계산
```

## Factory Methods

### calculate() - 예약 생성
**목적**: 새로운 예약 가격 계산 및 생성

**파라미터**:
- `ReservationId reservationId`: 예약 ID
- `RoomId roomId`: 룸 ID
- `TimeSlotPriceBreakdown timeSlotBreakdown`: 시간대 가격 내역
- `List<ProductPriceBreakdown> productBreakdowns`: 상품 가격 내역
- `long timeoutMinutes`: PENDING 타임아웃 (분)

**동작**:
1. 시간대 가격 총액 계산
2. 상품 가격 총액 계산
3. 전체 총액 = 시간대 총액 + 상품 총액
4. calculatedAt = 현재 시각
5. expiresAt = 현재 시각 + timeoutMinutes
6. 초기 상태는 PENDING

**예시**:
```java
ReservationPricing pricing = ReservationPricing.calculate(
    ReservationId.of(1L),
    RoomId.of(10L),
    timeSlotBreakdown,
    productBreakdowns,
    10L  // 10분 후 만료
);
```

### restore() - 저장된 데이터 복원
**목적**: Repository에서 조회한 데이터를 도메인 객체로 복원

**파라미터**:
- `ReservationId reservationId`
- `RoomId roomId`
- `ReservationStatus status`
- `TimeSlotPriceBreakdown timeSlotBreakdown`
- `List<ProductPriceBreakdown> productBreakdowns`
- `Money totalPrice`
- `LocalDateTime calculatedAt`
- `LocalDateTime expiresAt`

**주의사항**:
- 검증은 수행하지만, 이미 저장된 데이터를 복원하는 용도로만 사용

## 상태 전이 메서드

### confirm()
**목적**: 예약 확정 (결제 완료 시)

**조건**:
- 현재 상태가 PENDING이어야 함

**동작**:
- status를 CONFIRMED로 변경

**예외**:
- IllegalStateException: PENDING 상태가 아닌 경우

### cancel()
**목적**: 예약 취소

**조건**:
- 현재 상태가 CANCELLED가 아니어야 함

**동작**:
- status를 CANCELLED로 변경

**예외**:
- IllegalStateException: 이미 CANCELLED인 경우

### updateProducts()
**목적**: 상품 목록 업데이트 및 가격 재계산

**조건**:
- 현재 상태가 PENDING이어야 함

**동작**:
1. 상태 검증 (PENDING이 아니면 예외)
2. 상품 목록 업데이트
3. 상품 총액 재계산
4. 전체 총액 재계산 (시간대 총액 + 새 상품 총액)
5. calculatedAt 갱신

**예외**:
- InvalidReservationStatusException: PENDING 상태가 아닌 경우
- IllegalArgumentException: productBreakdowns가 null인 경우

## 비즈니스 규칙

### 1. 가격 불변성
- 예약 생성 시점의 가격 정보를 스냅샷으로 저장
- 이후 PricingPolicy나 Product 가격 변경에도 예약 가격은 불변
- 단, PENDING 상태에서 상품 업데이트 시에만 가격 재계산 허용

### 2. 상태 전이 규칙
```
PENDING → CONFIRMED (confirm())
PENDING → CANCELLED (cancel())
CONFIRMED → CANCELLED (cancel())
CANCELLED → (전이 불가)
```

### 3. 가격 일관성
- totalPrice = timeSlotTotal + productTotal
- 생성 시와 restore 시 모두 일관성 검증 수행
- 일관성 위반 시 IllegalArgumentException 발생

### 4. 상품 업데이트 제약
- PENDING 상태에서만 상품 업데이트 가능
- CONFIRMED/CANCELLED 상태에서는 InvalidReservationStatusException 발생
- 상품 업데이트 시 calculatedAt 갱신

### 5. 만료 시간 규칙
- expiresAt은 calculatedAt보다 미래여야 함
- PENDING 상태에서만 만료 체크 의미 있음
- isExpired() = (status == PENDING && now > expiresAt)

## 가격 계산 로직

### 시간대 가격 계산
```java
// PricingPolicy에서 계산
PriceBreakdown breakdown = pricingPolicy.calculatePriceBreakdown(
    startTime,
    endTime
);

// TimeSlotPriceBreakdown 변환
Map<LocalDateTime, Money> slotPriceMap = breakdown.getSlotPrices()
    .stream()
    .collect(toMap(
        SlotPrice::slotTime,
        SlotPrice::price
    ));

TimeSlotPriceBreakdown timeSlotBreakdown =
    new TimeSlotPriceBreakdown(slotPriceMap, pricingPolicy.getTimeSlot());
```

### 상품 가격 계산
```java
// 각 상품별로 계산
List<ProductPriceBreakdown> productBreakdowns = new ArrayList<>();
for (ProductRequest req : request.getProducts()) {
    Product product = productRepository.findById(req.productId());
    ProductPriceBreakdown breakdown = product.calculatePrice(req.quantity());
    productBreakdowns.add(breakdown);
}
```

### 총 가격 계산
```java
Money timeSlotTotal = timeSlotBreakdown.getTotalPrice();
Money productTotal = productBreakdowns.stream()
    .map(ProductPriceBreakdown::totalPrice)
    .reduce(Money.ZERO, Money::add);

Money total = timeSlotTotal.add(productTotal);
```

## Domain Service: ProductAvailabilityService

### 역할
상품 재고 가용성 검증

### 주요 메서드
```java
boolean isAvailable(
    Product product,
    List<LocalDateTime> requestedSlots,
    int requestedQuantity,
    ReservationPricingRepository repository
)
```

### Scope별 재고 검증 전략

#### RESERVATION Scope
- 단순 재고 체크
- `requestedQuantity <= product.getTotalQuantity()`

#### PLACE Scope
- 플레이스 내 모든 룸의 예약 확인
- 요청 시간대와 겹치는 예약 조회 (PENDING/CONFIRMED)
- 각 시간 슬롯별 사용량 계산
- 최대 사용량 < totalQuantity 확인

**계산 로직**:
```java
// 1. 겹치는 예약 조회
List<ReservationPricing> overlappingReservations =
    repository.findByPlaceIdAndTimeRange(
        placeId, start, end,
        List.of(PENDING, CONFIRMED)
    );

// 2. 각 슬롯별 사용량 계산
int maxUsedQuantity = requestedSlots.stream()
    .mapToInt(slot -> calculateUsedAtSlot(overlappingReservations, productId, slot))
    .max()
    .orElse(0);

// 3. 가용 수량 = 전체 수량 - 최대 사용량
int available = product.getTotalQuantity() - maxUsedQuantity;

// 4. 요청 수량 <= 가용 수량
return requestedQuantity <= available;
```

#### ROOM Scope
- 특정 룸의 예약만 확인
- PLACE Scope와 동일한 계산 로직 (범위만 다름)

### 재고 차감 정책
- PENDING 상태: 소프트 락 (임시 예약)
- CONFIRMED 상태: 하드 락 (확정 예약)
- CANCELLED 상태: 재고 차감 제외

## 예시

### 예약 생성
```java
// 1. 시간대 가격 계산
PricingPolicy policy = pricingPolicyRepository.findById(roomId);
PriceBreakdown breakdown = policy.calculatePriceBreakdown(
    startTime, endTime
);
TimeSlotPriceBreakdown timeSlotBreakdown = convertToTimeSlotBreakdown(breakdown);

// 2. 상품 가격 계산 및 재고 검증
List<ProductPriceBreakdown> productBreakdowns = new ArrayList<>();
for (ProductRequest req : request.getProducts()) {
    Product product = productRepository.findById(req.productId());

    // 재고 가용성 검증
    if (!productAvailabilityService.isAvailable(
        product, timeSlots, req.quantity(), repository)) {
        throw new ProductNotAvailableException(product.getProductId());
    }

    // 가격 계산
    productBreakdowns.add(product.calculatePrice(req.quantity()));
}

// 3. 예약 생성
ReservationPricing pricing = ReservationPricing.calculate(
    ReservationId.of(1L),
    roomId,
    timeSlotBreakdown,
    productBreakdowns,
    10L  // 10분 타임아웃
);

// 4. 저장
reservationPricingRepository.save(pricing);
```

### 예약 확정 (이벤트 처리)
```java
@KafkaListener(topics = "payment-events")
public void handleReservationConfirmed(ReservationConfirmedEvent event) {
    ReservationId reservationId = ReservationId.of(event.getReservationId());
    ReservationPricing pricing = repository.findById(reservationId)
        .orElseThrow();

    // 상태 전이
    pricing.confirm();

    // 저장
    repository.save(pricing);
}
```

### 예약 취소 (이벤트 처리)
```java
@KafkaListener(topics = "reservation-events")
public void handleReservationCancelled(ReservationCancelledEvent event) {
    ReservationId reservationId = ReservationId.of(event.getReservationId());
    ReservationPricing pricing = repository.findById(reservationId)
        .orElseThrow();

    // 상태 전이
    pricing.cancel();

    // 저장
    repository.save(pricing);
}
```

### 예약 환불 (이벤트 처리, Issue #164)
```java
@KafkaListener(topics = "reservation-events")
public void handleReservationRefund(ReservationRefundEvent event) {
    ReservationId reservationId = ReservationId.of(event.getReservationId());
    ReservationPricing pricing = repository.findById(reservationId)
        .orElseThrow();

    // 멱등성 체크: 이미 CANCELLED 상태면 무시
    if (pricing.getStatus() == ReservationStatus.CANCELLED) {
        return;
    }

    // 상태 전이: CONFIRMED → CANCELLED
    pricing.cancel();

    // 상품 재고 해제 (Issue #157)
    for (ProductPriceBreakdown product : pricing.getProductBreakdowns()) {
        productRepository.releaseQuantity(
            product.productId(),
            product.quantity(),
            pricing.getTimeSlots()  // ROOM/PLACE Scope용
        );
    }

    // 저장
    repository.save(pricing);
}
```

**재고 해제 로직**:
- **RESERVATION Scope**: `UPDATE products SET reserved_quantity = reserved_quantity - ? WHERE product_id = ?`
- **ROOM/PLACE Scope**: `UPDATE product_time_slot_inventory SET reserved_quantity = reserved_quantity - ? WHERE product_id = ? AND slot_time IN (?)`

### 상품 업데이트
```java
// PENDING 상태의 예약만 가능
ReservationPricing pricing = repository.findById(reservationId)
    .orElseThrow();

// 새로운 상품 목록으로 가격 재계산
List<ProductPriceBreakdown> newBreakdowns = calculateProductBreakdowns(
    newProductRequests
);

// 상품 업데이트 (가격 재계산 포함)
pricing.updateProducts(newBreakdowns);

// 저장
repository.save(pricing);
```

### PENDING 타임아웃 처리
```java
@Scheduled(cron = "0 */1 * * * *")
@SchedulerLock(name = "PendingReservationTimeoutScheduler")
public void cancelExpiredPendingReservations() {
    LocalDateTime now = LocalDateTime.now();

    // 만료된 PENDING 예약 조회
    List<ReservationPricing> expiredReservations =
        repository.findExpiredPending(now);

    // 자동 취소
    for (ReservationPricing reservation : expiredReservations) {
        reservation.cancel();
        repository.save(reservation);
    }
}
```

## 테스트 전략

### Domain Layer
1. **상태 전이 테스트**
   - PENDING → CONFIRMED 성공
   - PENDING → CANCELLED 성공
   - CONFIRMED → CANCELLED 성공
   - 잘못된 전이 시도 시 예외

2. **가격 계산 테스트**
   - 시간대 가격 + 상품 가격 = 총 가격
   - 가격 일관성 검증

3. **상품 업데이트 테스트**
   - PENDING 상태에서 성공
   - CONFIRMED/CANCELLED 상태에서 예외

4. **만료 체크 테스트**
   - PENDING + 만료 시간 초과 = true
   - 그 외 = false

### Repository Layer
1. **CRUD 동작 테스트**
   - save, findById, delete

2. **시간대별 조회 테스트**
   - findByPlaceIdAndTimeRange
   - findByRoomIdAndTimeRange

3. **상태별 조회 테스트**
   - findByStatus

4. **ElementCollection 저장/조회 테스트**
   - 시간대별 가격 (reservation_pricing_slots)
   - 상품별 가격 (reservation_pricing_products)

### Application Layer
1. **Use Case 테스트**
   - 예약 생성 성공/실패
   - 예약 확정 성공/실패
   - 예약 취소 성공/실패
   - 가격 미리보기
   - 상품 업데이트 성공/실패

2. **재고 가용성 테스트**
   - Scope별 재고 검증 (PLACE/ROOM/RESERVATION)
   - 시간대 겹침 처리
   - 재고 부족 예외

3. **이벤트 처리 테스트**
   - ReservationConfirmed 이벤트 처리
   - ReservationCancelled 이벤트 처리
   - ReservationRefund 이벤트 처리 (Issue #164)
   - 상품 재고 해제 검증
   - 멱등성 보장

4. **스케줄러 테스트**
   - 만료된 PENDING 예약 자동 취소
   - ShedLock 분산 락

## 설계 결정 사항

### 1. 가격 불변성 보장
**결정**: Value Object Snapshot 패턴 사용

**근거**:
- 예약 시점의 가격 정보를 스냅샷으로 저장
- 이후 PricingPolicy나 Product 가격 변경에도 영향 없음
- 히스토리 보존 및 감사 추적 용이

### 2. 상품 정보 비정규화
**결정**: ProductPriceBreakdown에 productName, pricingType 포함

**근거**:
- 스냅샷 시점의 상품 정보 보존
- Product 삭제나 변경에도 예약 정보 유지
- 조회 성능 향상 (JOIN 불필요)

### 3. PlaceId 비정규화
**결정**: ReservationPricing에 placeId 포함 (roomId만으로 조회 가능하지만)

**근거**:
- PLACE Scope 상품 재고 조회 성능 향상
- 플레이스 단위 통계/리포트 쿼리 최적화
- 인덱스 전략: (place_id, status), (place_id, calculated_at)

### 4. PENDING 타임아웃 처리
**결정**: 스케줄러 + ShedLock 분산 락

**근거**:
- 다중 인스턴스 환경에서도 안전
- JDBC 기반 락으로 별도 인프라 불필요
- 타임아웃 시간 설정 유연성 (application.yml)

### 5. 상태 전이를 이벤트 기반으로 처리
**결정**: Payment 서비스의 이벤트 수신하여 상태 변경

**근거**:
- 마이크로서비스 간 느슨한 결합
- 결제 로직과 예약 로직 분리
- 멱등성 보장으로 중복 이벤트 처리 안전

## Trade-offs

### 장점
- 가격 불변성 보장 (스냅샷 패턴)
- 명확한 상태 전이 규칙
- 재고 가용성 검증 로직의 명확성
- 테스트 용이성

### 단점
- 상품 정보 비정규화로 인한 저장 공간 증가
- PlaceId 비정규화로 인한 일관성 관리 필요
- ElementCollection 사용으로 인한 성능 trade-off (JOIN 비용)

### 향후 개선 방향
- Redis 캐싱으로 재고 조회 성능 향상
- 예약 히스토리 조회 API
- 통계 및 리포트 기능 (예약 패턴 분석)
---

**Last Updated**: 2025-11-12
