# 예약 플로우 전체 시나리오

## 문서 개요

**목적**: 예약 서비스와 가격 서비스 간 협업을 통한 전체 예약 프로세스 설명

**대상**: 백엔드 개발자, 아키텍트

**최종 업데이트**: 2025-11-15

---

## 전체 예약 플로우

### 1단계: 슬롯 예약 (예약 서비스)

#### 사용자 액션
사용자가 원하는 시간대(슬롯)를 선택하고 예약 요청

#### 예약 서비스 동작
```
POST /api/slots/reserve
Request:
{
  "roomId": 1,
  "slotDate": "2025-11-15",
  "startTimes": ["10:00", "10:30", "11:00", "11:30"]
}

Response:
{
  "reservationId": 1000,
  "roomId": 1,
  "status": "SLOT_RESERVED",
  "slots": [...],
  "expiresAt": "2025-11-15T10:45:00"  // 15분 후 만료
}
```

#### 이벤트 발행
```json
{
  "topic": "reservation-reserved",
  "eventType": "SlotReservedEvent",
  "reservationId": 1000,
  "roomId": 1,
  "slotDate": "2025-11-15",
  "startTimes": ["10:00", "10:30", "11:00", "11:30"],
  "occurredAt": "2025-11-09T10:00:00"
}
```

**포인트**:
- placeId는 이벤트에 포함하지 않음
- 상품 정보는 아직 없음
- 시간 슬롯만 예약된 상태

---

### 2단계: 시간대 가격 계산 (가격 서비스)

#### SlotReservedEvent 수신
가격 서비스의 EventConsumer가 이벤트 수신

#### 가격 서비스 동작
```java
@KafkaListener(topics = "reservation-reserved")
public void consumeReservationEvents(String message) {
    // 1. 이벤트 역직렬화
    SlotReservedEvent event = deserialize(message);

    // 2. SlotReservedEventHandler로 라우팅
    handler.handle(event);
}
```

#### 핸들러 처리
```java
public void handle(SlotReservedEvent event) {
    // 1. 멱등성 검사
    if (reservationPricingRepository.existsById(event.getReservationId())) {
        return; // 이미 처리됨
    }

    // 2. PricingPolicy 조회 (placeId 획득)
    PricingPolicy policy = pricingPolicyRepository.findById(event.getRoomId());

    // 3. 시간대별 가격 계산
    TimeSlotPriceBreakdown breakdown = calculateTimeSlotPrices(event);

    // 4. ReservationPricing 생성 (PENDING, 상품 없음)
    ReservationPricing pricing = ReservationPricing.calculate(
        reservationId: event.getReservationId(),
        roomId: event.getRoomId(),
        timeSlotBreakdown: breakdown,
        productBreakdowns: []  // 빈 리스트
    );

    // 5. 저장
    reservationPricingRepository.save(pricing);
}
```

#### 생성된 ReservationPricing
```json
{
  "reservationId": 1000,
  "roomId": 1,
  "placeId": 100,
  "status": "PENDING",
  "timeSlotBreakdown": {
    "slots": [
      {"time": "2025-11-15T10:00", "price": 10000},
      {"time": "2025-11-15T10:30", "price": 10000},
      {"time": "2025-11-15T11:00", "price": 10000},
      {"time": "2025-11-15T11:30", "price": 10000}
    ],
    "subtotal": 40000
  },
  "productBreakdowns": [],  // 비어있음
  "totalPrice": 40000,
  "calculatedAt": "2025-11-09T10:00:05"
}
```

**포인트**:
- 시간대 가격만 계산됨
- 상품 정보는 아직 없음
- PENDING 상태로 저장

---

### 3단계: 재고 조회 (가격 서비스)

#### 사용자 액션
사용자가 추가할 상품을 선택하기 전 재고 확인

#### API 호출
```
GET /api/products/availability?roomId=1&placeId=100&startTime=2025-11-15T10:00&endTime=2025-11-15T12:00

Response:
{
  "products": [
    {
      "productId": 1,
      "name": "빔프로젝터",
      "totalQuantity": 3,
      "availableQuantity": 2,
      "scope": "PLACE",
      "pricingStrategy": "INITIAL_PLUS_ADDITIONAL"
    },
    {
      "productId": 2,
      "name": "음료",
      "totalQuantity": 50,
      "availableQuantity": 50,
      "scope": "RESERVATION",
      "pricingStrategy": "PER_ITEM"
    }
  ]
}
```

**포인트**:
- 사용자가 상품 선택 전 재고 확인
- 이미 구현되어 있음 (Issue #58)
- **룸별 상품 필터링 적용** (Epic #77):
  - PLACE 상품: 룸별 허용 목록 설정된 상품만 표시 (화이트리스트)
  - ROOM 상품: 해당 룸 전용 상품 표시
  - RESERVATION 상품: 모든 예약에서 사용 가능한 상품 표시

---

### 4단계: 상품 추가 및 재고 락 (가격 서비스) - 구현 필요

#### 사용자 액션
상품 선택 완료 후 예약에 추가

#### API 호출
```
PUT /api/reservations/1000/products
Request:
{
  "products": [
    {"productId": 1, "quantity": 1},  // 빔프로젝터 1개
    {"productId": 2, "quantity": 10}  // 음료 10개
  ]
}

Response:
{
  "reservationId": 1000,
  "status": "PENDING",  // 여전히 PENDING
  "timeSlotBreakdown": {
    "subtotal": 40000
  },
  "productBreakdowns": [
    {
      "productId": 1,
      "name": "빔프로젝터",
      "quantity": 1,
      "unitPrice": 10000,
      "additionalPrice": 15000,  // 3시간 * 5000
      "subtotal": 25000
    },
    {
      "productId": 2,
      "name": "음료",
      "quantity": 10,
      "unitPrice": 3000,
      "subtotal": 30000
    }
  ],
  "totalPrice": 95000,  // 40000 + 25000 + 30000
  "calculatedAt": "2025-11-09T10:01:00"
}
```

#### 내부 동작
```java
public ReservationPricingResponse updateProducts(
    Long reservationId,
    List<ProductRequest> products) {

    // 1. 기존 PENDING ReservationPricing 조회
    ReservationPricing pricing = reservationPricingRepository
        .findById(reservationId)
        .orElseThrow(...);

    // 2. 상품 조회
    List<Product> productList = fetchProducts(products);

    // 3. 재고 검증 (ProductAvailabilityService)
    validateProductAvailability(productList, pricing.getTimeSlots(), products);

    // 4. 상품 가격 계산
    List<ProductPriceBreakdown> breakdowns =
        calculateProductBreakdowns(productList, products);

    // 5. ReservationPricing 업데이트
    ReservationPricing updated = pricing.updateProducts(breakdowns);

    // 6. 저장 (재고 락 효과)
    reservationPricingRepository.save(updated);

    return ReservationPricingResponse.from(updated);
}
```

**포인트**:
- 재고 검증 실패 시 예외 발생 (409 Conflict)
- 성공 시 ReservationPricing 업데이트 = **재고 락**
- ProductAvailabilityService가 PENDING 상태 예약을 재고 계산에 포함
- 여전히 PENDING 상태 유지

---

### 5단계: 결제 (결제 서비스)

#### 사용자 액션
최종 가격 확인 후 결제 진행

#### 결제 서비스 동작
```
POST /api/payments
Request:
{
  "reservationId": 1000,
  "amount": 95000,
  "paymentMethod": "CARD",
  "cardInfo": {...}
}

Response:
{
  "paymentId": 5000,
  "reservationId": 1000,
  "status": "COMPLETED",
  "paidAt": "2025-11-09T10:05:00"
}
```

#### 이벤트 발행
```json
{
  "topic": "payment-completed",
  "eventType": "PaymentCompletedEvent",
  "paymentId": 5000,
  "reservationId": 1000,
  "amount": 95000,
  "occurredAt": "2025-11-09T10:05:00"
}
```

---

### 6단계: 예약 확정 (가격 서비스) - 구현 필요

#### PaymentCompletedEvent 수신
가격 서비스의 EventConsumer가 이벤트 수신

#### 가격 서비스 동작
```java
@KafkaListener(topics = "payment-completed")
public void consumePaymentEvents(String message) {
    PaymentCompletedEvent event = deserialize(message);
    paymentCompletedEventHandler.handle(event);
}
```

#### 핸들러 처리
```java
public void handle(PaymentCompletedEvent event) {
    // 1. ReservationPricing 조회
    ReservationPricing pricing = reservationPricingRepository
        .findById(event.getReservationId())
        .orElseThrow(...);

    // 2. 상태 변경: PENDING → CONFIRMED
    pricing.confirm();

    // 3. 저장
    reservationPricingRepository.save(pricing);
}
```

#### 최종 상태
```json
{
  "reservationId": 1000,
  "status": "CONFIRMED",  // 확정됨
  "timeSlotBreakdown": {...},
  "productBreakdowns": [...],
  "totalPrice": 95000,
  "calculatedAt": "2025-11-09T10:01:00",
  "confirmedAt": "2025-11-09T10:05:00"
}
```

**포인트**:
- PENDING → CONFIRMED 상태 변경
- 재고는 이미 차감된 상태 (4단계에서 차감됨)
- 가격 정보는 변경되지 않음 (스냅샷)

---

### 7단계: 예약 취소 (선택적)

#### 사용자 액션
사용자가 예약 취소 요청

#### API 호출
```
POST /api/reservations/1000/cancel

Response:
{
  "reservationId": 1000,
  "status": "CANCELLED",
  "cancelledAt": "2025-11-09T10:10:00"
}
```

#### 내부 동작
```java
public ReservationPricingResponse cancelReservation(Long reservationId) {
    // 1. ReservationPricing 조회
    ReservationPricing pricing = reservationPricingRepository
        .findById(reservationId)
        .orElseThrow(...);

    // 2. 상태 변경: PENDING/CONFIRMED → CANCELLED
    pricing.cancel();

    // 3. 저장 (재고 자동 복구)
    reservationPricingRepository.save(pricing);

    return ReservationPricingResponse.from(pricing);
}
```

**포인트**:
- PENDING 또는 CONFIRMED → CANCELLED 가능
- ProductAvailabilityService가 CANCELLED 예약을 재고 계산에서 제외
- 재고 자동 복구 효과
- 가격 정보는 이력으로 보관

---

### 8단계: 예약 환불 (선택적, Issue #164)

#### 환불 시나리오
결제 완료 후 환불 요청 시 (CONFIRMED → CANCELLED)

#### 결제 서비스 동작
```
POST /api/payments/5000/refund

Response:
{
  "paymentId": 5000,
  "reservationId": 1000,
  "refundAmount": 95000,
  "refundedAt": "2025-11-09T10:20:00",
  "status": "REFUNDED"
}
```

#### 이벤트 발행
```json
{
  "topic": "reservation-events",
  "eventType": "ReservationRefund",
  "reservationId": 1000,
  "refundedAt": "2025-11-09T10:20:00",
  "reason": "USER_REQUEST"
}
```

#### ReservationRefundEvent 수신
가격 서비스의 EventConsumer가 이벤트 수신

#### 가격 서비스 동작
```java
@KafkaListener(topics = "reservation-events")
public void consumeReservationEvents(String message) {
    ReservationRefundEvent event = deserialize(message);
    reservationRefundEventHandler.handle(event);
}
```

#### 핸들러 처리 (Issue #164)
```java
public void handle(ReservationRefundEvent event) {
    // 1. ReservationPricing 조회
    ReservationPricing pricing = reservationPricingRepository
        .findById(event.getReservationId())
        .orElseThrow(...);

    // 2. 멱등성 체크: 이미 CANCELLED 상태면 무시
    if (pricing.getStatus() == ReservationStatus.CANCELLED) {
        return;
    }

    // 3. 상태 변경: CONFIRMED → CANCELLED
    pricing.cancel();

    // 4. 상품 재고 해제 (Issue #157)
    for (ProductPriceBreakdown product : pricing.getProductBreakdowns()) {
        productRepository.releaseQuantity(
            product.productId(),
            product.quantity(),
            pricing.getTimeSlots()  // ROOM/PLACE Scope용
        );
    }

    // 5. 저장
    reservationPricingRepository.save(pricing);
}
```

**재고 해제 로직** (Issue #157):
- **RESERVATION Scope**: `UPDATE products SET reserved_quantity = reserved_quantity - ? WHERE product_id = ?`
- **ROOM/PLACE Scope**: `UPDATE product_time_slot_inventory SET reserved_quantity = reserved_quantity - ? WHERE product_id = ? AND slot_time IN (?)`

**포인트**:
- CONFIRMED 상태에서만 환불 처리
- 상품 재고가 명시적으로 해제됨 (ProductRepository.releaseQuantity)
- 멱등성 보장 (중복 이벤트 처리 시 안전)
- Atomic UPDATE로 동시성 제어 (ADR_002)

---

## 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                    예약 상태 전이                              │
└─────────────────────────────────────────────────────────────┘

                    SlotReservedEvent
                           │
                           ▼
                    ┌──────────┐
                    │ PENDING  │ ◄─── 초기 상태 (시간대 가격만)
                    └────┬─────┘
                         │
                         │ updateProducts()
                         │
                    ┌────▼─────┐
                    │ PENDING  │ ◄─── 상품 추가됨 (재고 락)
                    └────┬─────┘
                         │
                         │ PaymentCompletedEvent
                         │
                    ┌────▼──────┐
                    │ CONFIRMED │ ◄─── 결제 완료 (확정)
                    └───────────┘


    PENDING/CONFIRMED 모두 cancel() 가능
                         │
                         ▼
                    ┌───────────┐
                    │ CANCELLED │ ◄─── 취소 (재고 복구)
                    └───────────┘
```

---

## 재고 관리 메커니즘

### PENDING 상태의 역할
```
PENDING = Soft Lock (소프트 락)

- 재고 차감: O (다른 예약이 사용 불가)
- 가격 확정: X (아직 결제 전)
- 취소 가능: O (재고 자동 복구)
```

### CONFIRMED 상태의 역할
```
CONFIRMED = Hard Lock (하드 락)

- 재고 차감: O (유지)
- 가격 확정: O (스냅샷 확정)
- 취소 가능: O (환불 프로세스 필요)
```

### CANCELLED 상태의 역할
```
CANCELLED = Released (해제됨)

- 재고 복구: O (자동)
- 가격 이력: O (보관)
- 상태 전이: X (최종 상태)
```

---

## 구현 필요 항목

### 1. 상품 추가 API
- `PUT /api/reservations/{reservationId}/products`
- UseCase: `updateProducts()`
- 재고 검증 및 ReservationPricing 업데이트

### 2. PaymentCompletedEvent 핸들러
- 이벤트: `PaymentCompletedEvent`
- 핸들러: `PaymentCompletedEventHandler`
- 동작: PENDING → CONFIRMED 상태 변경

---

## 실패 시나리오 및 보상 트랜잭션

### 시나리오 1: 재고 부족 (4단계)
```
문제: 상품 추가 시 재고 부족
대응: 409 Conflict 응답, 사용자에게 재고 부족 안내
보상: 없음 (아직 예약 생성 안됨)
```

### 시나리오 2: 결제 실패 (5단계)
```
문제: 결제 서비스에서 결제 실패
대응: 결제 서비스가 PaymentFailedEvent 발행
보상: PENDING 상태 유지, 15분 후 TTL로 자동 취소
```

### 시나리오 3: 시간 만료
```
문제: 슬롯 예약 후 15분 이내 결제 미완료
대응: 예약 서비스가 ReservationExpiredEvent 발행
보상: 가격 서비스가 자동 취소 처리 (재고 복구)
```

---

## 참고 문서

- [PROJECT_REQUIREMENTS.md](../../requirements/PROJECT_REQUIREMENTS.md)
- [Product Domain](../product/domain.md)
- [Product - 룸별 상품 허용 관리](../product/README.md#룸별-상품-허용-관리-epic-77)
- [Event Handling Architecture](../event-handling/architecture.md)

---

**Last Updated**: 2025-11-15
