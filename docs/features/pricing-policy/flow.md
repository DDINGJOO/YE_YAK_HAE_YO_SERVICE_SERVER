# PricingPolicy 플로우 및 시퀀스

## 1. 가격 정책 자동 생성 플로우 (Issue #9)

### 시퀀스 다이어그램
```
Place Service          Kafka              EventConsumer       RoomCreatedEventHandler    CreatePricingPolicyService    PricingPolicyRepository
     |                   |                      |                       |                            |                              |
     |-- publish ------->|                      |                       |                            |                              |
     |  RoomCreatedEvent |                      |                       |                            |                              |
     |                   |                      |                       |                            |                              |
     |                   |-- consume ---------->|                       |                            |                              |
     |                   |                      |                       |                            |                              |
     |                   |                      |-- handle(event) ----->|                            |                              |
     |                   |                      |                       |                            |                              |
     |                   |                      |                       |-- createDefaultPolicy ---->|                            |
     |                   |                      |                       |    (roomId, placeId,       |                            |
     |                   |                      |                       |     timeSlot)              |                            |
     |                   |                      |                       |                            |                            |
     |                   |                      |                       |                            |-- existsById(roomId) ----->|
     |                   |                      |                       |                            |<-- false ------------------|
     |                   |                      |                       |                            |                            |
     |                   |                      |                       |                            |-- save(policy) ----------->|
     |                   |                      |                       |                            |    (defaultPrice: 0원)     |
     |                   |                      |                       |                            |<-- savedPolicy ------------|
     |                   |                      |                       |<-- policy ------------------|                            |
     |                   |                      |<-- acknowledged -------|                            |                            |
     |                   |                      |                       |                            |                            |
```

### 상세 플로우

#### 1단계: 이벤트 발행
- Place 서비스에서 룸 생성 시 RoomCreatedEvent 발행
- Event 내용: `{ topic, eventType, placeId, roomId, timeSlot }`

#### 2단계: 이벤트 수신 및 역직렬화
- EventConsumer가 Kafka에서 메시지 수신
- JsonUtil로 JSON → RoomCreatedEvent 객체 변환
- EVENT_TYPE_MAP에서 이벤트 타입 확인 (`"RoomCreated"`)

#### 3단계: 핸들러 라우팅
- `getSupportedEventType() == "RoomCreated"`인 핸들러 검색
- RoomCreatedEventHandler 선택

#### 4단계: 가격 정책 생성
- CreatePricingPolicyService.createDefaultPolicy() 호출
- 중복 확인 (`existsById`)
- PricingPolicy.create() 로 **기본 가격 0원**으로 정책 생성
- Repository에 저장

#### 5단계: 완료
- Kafka 메시지 acknowledge
- 이후 관리자가 API로 실제 가격 설정 가능

## 2. 가격 계산 플로우

### 시퀀스 다이어그램
```
Client              Application          PricingPolicy         TimeRangePrices       Repository
  |                      |                      |                      |                  |
  |-- 예약 요청 -------->|                      |                      |                  |
  |  (roomId, start,     |                      |                      |                  |
  |   end)               |                      |                      |                  |
  |                      |                      |                      |                  |
  |                      |-- findById(roomId) -------------------------------->|
  |                      |<-- policy ---------------------------------------------|
  |                      |                      |                      |                  |
  |                      |-- calculatePriceBreakdown(start, end) -->|                  |
  |                      |                      |                      |                  |
  |                      |                      |-- 슬롯 분할 -------->|                  |
  |                      |                      |  (start ~ end를      |                  |
  |                      |                      |   TimeSlot 단위로)   |                  |
  |                      |                      |                      |                  |
  |                      |                      |-- for each slot ---->|                  |
  |                      |                      |                      |                  |
  |                      |                      |   findPriceForSlot -->|                  |
  |                      |                      |   (dayOfWeek, time)   |                  |
  |                      |                      |<-- price or empty ----|                  |
  |                      |                      |                      |                  |
  |                      |                      |   if empty:          |                  |
  |                      |                      |   use defaultPrice   |                  |
  |                      |                      |                      |                  |
  |                      |<-- PriceBreakdown ---|                      |                  |
  |                      |    (slotPrices,      |                      |                  |
  |                      |     totalPrice)      |                      |                  |
  |<-- 예약 가격 정보 --|                      |                      |                  |
  |                      |                      |                      |                  |
```

### 상세 플로우

#### 1단계: 가격 정책 조회
- Repository에서 roomId로 PricingPolicy 조회

#### 2단계: 시간 슬롯 분할
- 예약 시작 시각부터 종료 시각까지 TimeSlot 단위로 분할
- 예: 18:00~20:00, HOUR → [18:00, 19:00]

#### 3단계: 슬롯별 가격 결정
각 슬롯마다:
1. 요일과 시작 시각 확인
2. TimeRangePrices.findPriceForSlot() 호출
3. 매칭되는 시간대별 가격 있으면 사용
4. 없으면 defaultPrice 사용

#### 4단계: 가격 집계
- 모든 슬롯 가격 합산 → totalPrice
- SlotPrice 목록, 총 가격, 슬롯 개수로 PriceBreakdown 생성

#### 5단계: 반환
- PriceBreakdown 객체 반환
- 클라이언트는 이를 바탕으로 예약 금액 표시

## 3. 가격 정책 수정 플로우 (Issue #10)

### 3.1 기본 가격 수정

**엔드포인트**: `PUT /api/pricing-policies/{roomId}/default-price`

#### 시퀀스 다이어그램
```
Client       Controller      UpdateUseCase      PricingPolicy     Repository
  |               |                 |                 |                 |
  |-- PUT ------->|                 |                 |                 |
  |   {price}     |                 |                 |                 |
  |               |-- update ------>|                 |                 |
  |               |  (roomId,       |                 |                 |
  |               |   Money)        |                 |                 |
  |               |                 |                 |                 |
  |               |                 |-- findById -------------------->|
  |               |                 |<-- policy ----------------------|
  |               |                 |                 |                 |
  |               |                 |-- updateDefaultPrice() -->|      |
  |               |                 |                 |         |      |
  |               |                 |                 |<--------|      |
  |               |                 |                 |                 |
  |               |                 |-- save(policy) ---------------->|
  |               |                 |<-- saved ------------------------|
  |               |                 |                 |                 |
  |               |<-- policy ------|                 |                 |
  |               |                 |                 |                 |
  |<-- 200 OK ----|                 |                 |                 |
  |   Response    |                 |                 |                 |
  |               |                 |                 |                 |
```

### 3.2 시간대별 가격 재설정

**엔드포인트**: `PUT /api/pricing-policies/{roomId}/time-range-prices`

#### 시퀀스 다이어그램
```
Client       Controller      UpdateUseCase      PricingPolicy     Repository
  |               |                 |                 |                 |
  |-- PUT ------->|                 |                 |                 |
  |   {prices[]}  |                 |                 |                 |
  |               |                 |                 |                 |
  |               |-- DTO to Domain -->                |                 |
  |               |  TimeRangePrice[]                 |                 |
  |               |                 |                 |                 |
  |               |-- update ------>|                 |                 |
  |               |  (roomId,       |                 |                 |
  |               |   prices)       |                 |                 |
  |               |                 |                 |                 |
  |               |                 |-- findById -------------------->|
  |               |                 |<-- policy ----------------------|
  |               |                 |                 |                 |
  |               |                 |-- resetPrices() ---------->|      |
  |               |                 |  (new TimeRangePrices)    |      |
  |               |                 |<--------------------------|      |
  |               |                 |                 |                 |
  |               |                 |-- save(policy) ---------------->|
  |               |                 |<-- saved ------------------------|
  |               |                 |                 |                 |
  |               |<-- policy ------|                 |                 |
  |               |                 |                 |                 |
  |<-- 200 OK ----|                 |                 |                 |
  |   Response    |                 |                 |                 |
  |               |                 |                 |                 |
```

### 3.3 가격 정책 복사

**엔드포인트**: `POST /api/pricing-policies/{targetRoomId}/copy`

#### 시퀀스 다이어그램
```
Client       Controller      CopyUseCase     PricingPolicy     Repository
  |               |                |                |                |
  |-- POST ------>|                |                |                |
  | {sourceRoomId}|                |                |                |
  |               |                |                |                |
  |               |-- copy ------->|                |                |
  |               | (target, src)  |                |                |
  |               |                |                |                |
  |               |                |-- findById(source) ----------->|
  |               |                |<-- sourcePolicy ---------------|
  |               |                |                |                |
  |               |                |-- findById(target) ----------->|
  |               |                |<-- targetPolicy ---------------|
  |               |                |                |                |
  |               |                |-- validate placeId -->|         |
  |               |                |  (same place check)   |         |
  |               |                |                       |         |
  |               |                |   if different:       |         |
  |               |                |   throw Exception     |         |
  |               |                |                       |         |
  |               |                |-- copy prices ------->|         |
  |               |                | updateDefaultPrice()  |         |
  |               |                | resetPrices()         |         |
  |               |                |<----------------------|         |
  |               |                |                |                |
  |               |                |-- save(target) --------------->|
  |               |                |<-- saved ----------------------|
  |               |                |                |                |
  |               |<-- policy -----|                |                |
  |               |                |                |                |
  |<-- 200 OK ----|                |                |                |
  |   Response    |                |                |                |
  |               |                |                |                |
```

#### 복사 제약사항
- 같은 `placeId`를 가진 룸 간에만 복사 가능
- 다른 플레이스 간 복사 시도 시 `CannotCopyDifferentPlaceException` 발생 (400 Bad Request)

## 플로우 특징

### 장점
1. **이벤트 기반 자동화**: 룸 생성 즉시 가격 정책 자동 생성
2. **느슨한 결합**: Place 서비스와 Pricing 서비스 분리
3. **확장성**: 새로운 이벤트 타입 추가 용이
4. **정확한 가격 계산**: 슬롯별 세밀한 가격 적용

### 주의사항
1. **이벤트 순서**: RoomCreatedEvent가 먼저 도착해야 함
2. **중복 방지**: existsById로 중복 생성 방지
3. **재시도 전략**: TODO - DLQ 구현 필요
4. **트랜잭션**: 가격 정책 생성은 @Transactional 보장

---

**Last Updated**: 2025-11-15
