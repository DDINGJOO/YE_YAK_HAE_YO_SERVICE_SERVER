# 이벤트 통신 규약

## 개요

본 문서는 MSA 환경에서 서비스 간 이벤트 기반 비동기 통신을 위한 Kafka 프로토콜 규약을 정의합니다.

## 통신 방식

### 메시징 플랫폼
- **플랫폼**: Apache Kafka
- **통신 패턴**: Pub/Sub (발행-구독)
- **메시지 포맷**: JSON
- **문자 인코딩**: UTF-8

### Consumer Group 설정
```yaml
spring:
  kafka:
    consumer:
      group-id: pricing-service-consumer-group
      enable-auto-commit: false  # 수동 커밋 사용
      auto-offset-reset: earliest
```

## 메시지 구조

### 기본 메시지 포맷

모든 이벤트 메시지는 다음 공통 필드를 포함해야 합니다:

```json
{
  "topic": "string",      // Kafka 토픽 이름 (필수)
  "eventType": "string",  // 이벤트 타입 (필수)
  // ... 이벤트별 추가 필드
}
```

### 필드 규칙

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| topic | String | Y | 메시지가 발행된 Kafka 토픽 이름 |
| eventType | String | Y | 이벤트 타입 식별자 (PascalCase) |

### 예시: RoomCreatedEvent
```json
{
  "topic": "room-events",
  "eventType": "RoomCreated",
  "placeId": 100,
  "roomId": 1,
  "timeSlot": "HOUR"
}
```

## Topic 명명 규칙

### 규칙
- **형식**: `{domain}-events`
- **케이스**: kebab-case
- **예시**:
  - `room-events`: 룸 관련 이벤트
  - `reservation-events`: 예약 관련 이벤트
  - `payment-events`: 결제 관련 이벤트

### Topic별 이벤트 타입

| Topic | Event Types | Producer | Consumers |
|-------|-------------|----------|-----------|
| room-events | RoomCreated, RoomUpdated | Place Service | Pricing Service |
| reservation-events | SlotReserved, ReservationConfirmed, ReservationCancelled, ReservationRefund | Reservation/Payment Service | Pricing Service |

## Event Type 명명 규칙

### 규칙
- **형식**: `{Entity}{PastTenseVerb}`
- **케이스**: PascalCase
- **시제**: 과거형 (이미 발생한 사건)
- **예시**:
  - `RoomCreated` (O)
  - `SlotReserved` (O)
  - `PaymentCompleted` (O)
  - `CreateRoom` (X - 현재형)
  - `room_created` (X - snake_case)

### 지원되는 Event Types

| Event Type | 설명 | 문서 |
|------------|------|------|
| RoomCreated | 룸 생성 완료 | [events.md](./events.md#1-roomcreatedevent) |
| RoomUpdated | 룸 정보 업데이트 완료 | [events.md](./events.md#2-roomupdatedevent) |
| SlotReserved | 예약 생성 완료 | [events.md](./events.md#3-slotreservedevent) |
| ReservationConfirmed | 예약 확정 완료 (결제 완료) | [events.md](./events.md#4-reservationconfirmedevent) |
| ReservationCancelled | 예약 취소 완료 | [events.md](./events.md#5-reservationcancelledevent) |
| ReservationRefund | 예약 환불 완료 | [events.md](./events.md#6-reservationrefundevent) |

## 직렬화/역직렬화

### Producer Side (발행자)

**직렬화**: Java 객체 → JSON String

```java
// Jackson ObjectMapper 사용
String jsonMessage = objectMapper.writeValueAsString(event);
kafkaTemplate.send(topic, jsonMessage);
```

### Consumer Side (구독자)

**역직렬화**: JSON String → Java 객체

```java
// 1. eventType 추출
JsonNode rootNode = objectMapper.readTree(message);
String eventType = rootNode.get("eventType").asText();

// 2. 이벤트 클래스 매핑
Class<? extends Event> eventClass = EVENT_TYPE_MAP.get(eventType);

// 3. 역직렬화
Event event = jsonUtil.fromJson(message, eventClass);
```

### Jackson 어노테이션 규칙

```java
public final class RoomCreatedEvent extends Event {
    @JsonCreator
    public RoomCreatedEvent(
        @JsonProperty("topic") String topic,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("placeId") Long placeId,
        @JsonProperty("roomId") Long roomId,
        @JsonProperty("timeSlot") String timeSlot
    ) {
        // ...
    }
}
```

- `@JsonCreator`: 역직렬화용 생성자 지정
- `@JsonProperty`: JSON 필드명 명시

## 메시지 처리 보장

### Delivery Semantics

**At-least-once 보장**
- Consumer는 수동 커밋(Manual Acknowledge) 사용
- 처리 성공 후에만 `acknowledgment.acknowledge()` 호출
- 실패 시 메시지 재처리 가능

### 처리 플로우

```
1. 메시지 수신 (Kafka Consumer)
2. 역직렬화 (JSON → Event 객체)
3. 핸들러 라우팅 (eventType 기반)
4. 비즈니스 로직 실행
5. acknowledge() 호출 (성공 시)
```

### 코드 예시

```java
@KafkaListener(topics = "room-events", groupId = "pricing-service-consumer-group")
public void consume(String message, Acknowledgment acknowledgment) {
    try {
        // 1~4: 메시지 처리
        processMessage(message);

        // 5: 성공 시 커밋
        acknowledgment.acknowledge();
    } catch (Exception e) {
        logger.error("Failed to process message: {}", message, e);
        // 실패 처리 (현재는 acknowledge하여 무한 재시도 방지)
        acknowledgment.acknowledge();
    }
}
```

## 에러 처리

### 현재 구현

**전략**: 로그 기록 후 acknowledge
- 실패한 메시지도 커밋하여 무한 재시도 방지
- 로그를 통한 사후 분석

```java
catch (Exception e) {
    logger.error("Failed to process message: {}", message, e);
    acknowledgment.acknowledge();
}
```

### 향후 개선 계획

#### 1. Dead Letter Queue (DLQ)
실패한 메시지를 별도 토픽으로 전송

```yaml
# 예정
room-events-dlq
reservation-events-dlq
```

#### 2. 재시도 전략
- **일시적 오류** (네트워크, DB 연결): 지수 백오프로 3회 재시도
- **영구적 오류** (잘못된 데이터): 즉시 DLQ로 전송

#### 3. 모니터링 및 알림
- 실패율 임계값 초과 시 알림
- DLQ 메시지 수 모니터링

## 메시지 순서 보장

### Kafka Partition 전략

**동일 Entity의 순서 보장 필요 시**:
- Partition Key로 Entity ID 사용
- 예: `roomId`를 key로 설정하면 동일 룸의 이벤트는 순서 보장

```java
// Producer 설정 예시
kafkaTemplate.send(
    "room-events",           // topic
    String.valueOf(roomId),  // key (partition key)
    jsonMessage              // value
);
```

### 주의사항
- 순서가 중요하지 않은 경우 key 없이 발행하여 부하 분산
- 현재 구현에서는 key를 사용하지 않음 (순서 보장 불필요)

## 성능 및 확장성

### Consumer 동시성

**현재 설정**: 단일 스레드 처리

**확장 방법**:
```java
@KafkaListener(
    topics = "room-events",
    groupId = "pricing-service-consumer-group",
    concurrency = "3"  // 3개의 Consumer 스레드
)
```

### Kafka Consumer 설정 권장사항

| 설정 | 권장값 | 이유 |
|------|--------|------|
| max.poll.records | 10-50 | 한 번에 처리할 메시지 수 제한 |
| max.poll.interval.ms | 300000 (5분) | 처리 시간이 긴 경우 타임아웃 방지 |
| session.timeout.ms | 30000 (30초) | Consumer 상태 확인 주기 |

## 이벤트 버전 관리

### 스키마 변경 정책

**하위 호환성 유지 원칙**:
1. **필드 추가**: 기존 Consumer가 무시하도록 설계
2. **필드 삭제**: 새 버전 이벤트 타입 생성
3. **필드 타입 변경**: 새 버전 이벤트 타입 생성

### 버전 명명 규칙

```java
// v1
public class RoomCreatedEvent extends Event { ... }

// v2 (스키마 변경 시)
public class RoomCreatedEventV2 extends Event { ... }

// EVENT_TYPE_MAP 등록
EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
EVENT_TYPE_MAP.put("RoomCreatedV2", RoomCreatedEventV2.class);
```

## 보안 및 인증

### 현재 구현
- 내부 네트워크 통신으로 별도 인증 없음

### 향후 고려사항
- **SSL/TLS**: 메시지 암호화
- **SASL**: Kafka 인증
- **ACL**: Topic별 접근 제어

## 모니터링 포인트

### 추적해야 할 메트릭

1. **Lag**: Consumer가 얼마나 뒤처져 있는가
2. **처리 속도**: 초당 처리 메시지 수
3. **실패율**: 전체 메시지 중 실패한 비율
4. **처리 시간**: 메시지당 평균 처리 시간

### 로깅 규칙

```java
// 수신 로그
logger.info("Received message: topic={}, eventType={}", topic, eventType);

// 처리 완료 로그
logger.info("Successfully processed event: eventType={}, aggregateId={}", eventType, id);

// 실패 로그
logger.error("Failed to process message: {}", message, exception);
```

## 테스트 가이드

### Unit Test
```java
@Test
void testRoomCreatedEventDeserialization() {
    String json = "{\"topic\":\"room-events\",\"eventType\":\"RoomCreated\",...}";
    RoomCreatedEvent event = jsonUtil.fromJson(json, RoomCreatedEvent.class);
    assertThat(event.getRoomId()).isEqualTo(1L);
}
```

### Integration Test
```java
// Embedded Kafka 사용 (향후 구현 예정)
@SpringBootTest
@EmbeddedKafka(topics = "room-events")
class EventConsumerIntegrationTest {
    // 실제 메시지 발행 → 수신 → 처리 검증
}
```

## 참고 문서

- [이벤트 타입 및 스키마](./events.md)
- [Event Handling 아키텍처](./architecture.md)
- [Event Handling 기능 개요](./README.md)

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2025-01-09 | 초기 문서 작성 |

---

**Last Updated**: 2025-11-15
