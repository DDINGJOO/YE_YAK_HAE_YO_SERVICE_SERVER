# 이벤트 타입 및 스키마

## 이벤트 목록

### 1. RoomCreatedEvent

**목적**: Place 서비스에서 룸이 생성되었을 때 발행

**Producer**: Place Service

**Consumer**: Pricing Service (현재 프로젝트)

**Topic**: `room-events`

**처리 핸들러**: `RoomCreatedEventHandler`

**비즈니스 로직**: 가격 정책 자동 생성 (기본 가격 0원)

#### JSON 스키마
```json
{
  "topic": "room-events",
  "eventType": "RoomCreated",
  "placeId": 100,
  "roomId": 1,
  "timeSlot": "HOUR"
}
```

#### 필드 설명
| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| topic | String | Y | Kafka 토픽 이름 |
| eventType | String | Y | 이벤트 타입 ("RoomCreated") |
| placeId | Long | Y | 장소 ID |
| roomId | Long | Y | 룸 ID (Aggregate ID) |
| timeSlot | String | Y | 시간 단위 (HOUR, HALFHOUR) |

#### 검증 규칙
- placeId: 양수
- roomId: 양수, 중복 불가
- timeSlot: "HOUR" 또는 "HALFHOUR"만 허용

#### Java 클래스
```java
package com.teambind.springproject.adapter.in.messaging.event;

public final class RoomCreatedEvent extends Event {
    private static final String EVENT_TYPE_NAME = "RoomCreated";

    private final Long placeId;
    private final Long roomId;
    private final String timeSlot;

    @JsonCreator
    public RoomCreatedEvent(
        @JsonProperty("topic") final String topic,
        @JsonProperty("eventType") final String eventType,
        @JsonProperty("placeId") final Long placeId,
        @JsonProperty("roomId") final Long roomId,
        @JsonProperty("timeSlot") final String timeSlot
    ) {
        super(topic, eventType);
        this.placeId = placeId;
        this.roomId = roomId;
        this.timeSlot = timeSlot;
    }

    @Override
    public String getEventTypeName() {
        return EVENT_TYPE_NAME;
    }

    // getters...
}
```

#### 처리 흐름
1. EventConsumer가 메시지 수신
2. JSON → RoomCreatedEvent 역직렬화
3. RoomCreatedEventHandler로 라우팅
4. CreatePricingPolicyService 호출
5. PricingPolicy 생성 (기본 가격 0원)
6. Repository에 저장
7. Kafka acknowledge

#### 예시 시나리오
```
시나리오: 관리자가 Place 서비스에서 새 룸을 등록

1. Place Service: 룸 생성 (ID=1, PlaceID=100, TimeSlot=HOUR)
2. Place Service: RoomCreatedEvent 발행 → room-events 토픽
3. Pricing Service: 이벤트 수신
4. Pricing Service: PricingPolicy 자동 생성
   - roomId: 1
   - placeId: 100
   - timeSlot: HOUR
   - defaultPrice: 0원
   - timeRangePrices: [] (비어있음)
5. 관리자: 나중에 API로 실제 가격 설정
```

---

---

### 2. RoomUpdatedEvent

**목적**: Place 서비스에서 룸 정보가 업데이트되었을 때 발행

**Producer**: Place Service

**Consumer**: Pricing Service

**Topic**: `room-events`

**처리 핸들러**: `RoomUpdatedEventHandler`

**비즈니스 로직**: PricingPolicy 정보 업데이트

---

### 3. SlotReservedEvent

**목적**: 예약이 생성되었을 때 발행

**Producer**: Reservation Service

**Consumer**: Pricing Service

**Topic**: `slot-events`

**처리 핸들러**: `SlotReservedEventHandler`

**비즈니스 로직**: 예약 가격 계산 및 재고 소프트 락 (PENDING 상태)

**예상 스키마**:
```json
{
  "topic": "reservation-events",
  "eventType": "SlotReserved",
  "reservationId": 1001,
  "roomId": 1,
  "startDateTime": "2025-01-06T18:00:00",
  "endDateTime": "2025-01-06T20:00:00",
  "totalPrice": 30000
}
```

---

### 4. ReservationConfirmedEvent

**목적**: 예약이 결제 완료되어 확정되었을 때 발행

**Producer**: Payment Service

**Consumer**: Pricing Service

**Topic**: `reservation-events`

**처리 핸들러**: `ReservationConfirmedEventHandler`

**비즈니스 로직**: PENDING → CONFIRMED 상태 전환, 재고 하드 락

**예상 스키마**:
```json
{
  "topic": "reservation-events",
  "eventType": "ReservationConfirmed",
  "reservationId": 1001,
  "confirmedAt": "2025-01-06T18:05:00"
}
```

---

### 5. ReservationCancelledEvent

**목적**: 예약이 취소되었을 때 발행

**Producer**: Reservation Service

**Consumer**: Pricing Service

**Topic**: `reservation-events`

**처리 핸들러**: `ReservationCancelledEventHandler`

**비즈니스 로직**: 예약 상태 CANCELLED로 변경, 재고 해제

**예상 스키마**:
```json
{
  "topic": "reservation-events",
  "eventType": "SlotCancelled",
  "reservationId": 1001,
  "roomId": 1,
  "cancelledAt": "2025-01-05T10:30:00",
  "reason": "USER_REQUEST"
}
```

---

### 6. ReservationRefundEvent

**목적**: 예약 환불이 처리되었을 때 발행

**Producer**: Payment Service

**Consumer**: Pricing Service

**Topic**: `reservation-events`

**처리 핸들러**: `ReservationRefundEventHandler`

**비즈니스 로직**: CONFIRMED → CANCELLED 상태 전환, 재고 해제

**예상 스키마**:
```json
{
  "topic": "reservation-events",
  "eventType": "ReservationRefund",
  "reservationId": 1001,
  "refundedAt": "2025-01-07T14:30:00",
  "reason": "USER_REQUEST"
}
```

---

## 이벤트 설계 가이드라인

### 1. 네이밍 규칙
- PascalCase 사용
- 과거형 동사 사용 (Created, Reserved, Cancelled)
- 명확하고 구체적인 이름

### 2. 필드 규칙
- 불변 객체 (final fields)
- Jackson 어노테이션 명시
- null 불가 (모든 필드 필수)

### 3. 버전 관리
- 이벤트 스키마 변경 시 새 버전 생성
- 예: `RoomCreatedEventV2`
- 하위 호환성 유지

### 4. 문서화
- 모든 필드에 Javadoc 추가
- JSON 스키마 예시 제공
- 비즈니스 의미 명확히 기술

### 5. 테스트
- 역직렬화 테스트 필수
- 유효하지 않은 JSON 처리 테스트
- 필드 누락 시 동작 테스트

---

## EVENT_TYPE_MAP 관리

### 현재 매핑
```java
private static final Map<String, Class<? extends Event>> EVENT_TYPE_MAP = new HashMap<>();

static {
    EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
    EVENT_TYPE_MAP.put("RoomUpdated", RoomUpdatedEvent.class);
    EVENT_TYPE_MAP.put("SlotReserved", SlotReservedEvent.class);
    EVENT_TYPE_MAP.put("ReservationConfirmed", ReservationConfirmedEvent.class);
    EVENT_TYPE_MAP.put("ReservationCancelled", ReservationCancelledEvent.class);
    EVENT_TYPE_MAP.put("ReservationRefund", ReservationRefundEvent.class);
}
```

### 추가 방법
새 이벤트 타입 추가 시:
```java
static {
    EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
    EVENT_TYPE_MAP.put("SlotReserved", SlotReservedEvent.class); // 추가
    EVENT_TYPE_MAP.put("SlotCancelled", SlotCancelledEvent.class); // 추가
}
```

### 향후 개선 (TODO)
- 리플렉션 기반 자동 스캔
- @EventType 어노테이션 활용
- Spring Component Scan 통합

---

## 참고 자료

### Jackson 역직렬화
- `@JsonCreator`: 불변 객체 생성자 지정
- `@JsonProperty`: 필드 매핑 지정
- 필드명과 JSON 키가 일치하면 생략 가능 (하지만 명시 권장)

### Kafka Topic 규칙
- kebab-case 사용: `room-events`, `reservation-events`
- 서비스별 또는 도메인별로 토픽 분리
- 이벤트 타입은 메시지 내 `eventType` 필드로 구분

---

**Last Updated**: 2025-11-15
