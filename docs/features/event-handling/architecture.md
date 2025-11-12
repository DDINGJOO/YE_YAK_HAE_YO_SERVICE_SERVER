# Event Handling 아키텍처

## 전체 구조

```
┌─────────────────────────────────────────────────────────────┐
│                     Event Layer (Domain)                     │
├─────────────────────────────────────────────────────────────┤
│  Event (abstract)                                            │
│    ├── RoomCreatedEvent                                      │
│    ├── RoomUpdatedEvent                                      │
│    ├── SlotReservedEvent                                     │
│    ├── ReservationConfirmedEvent (Task #88)                 │
│    ├── ReservationCancelledEvent (Task #88)                 │
│    └── ReservationRefundEvent (Issue #164)                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Handler Layer (Application)               │
├─────────────────────────────────────────────────────────────┤
│  EventHandler<T extends Event> (interface)                   │
│    ├── RoomCreatedEventHandler                              │
│    ├── RoomUpdatedEventHandler                              │
│    ├── SlotReservedEventHandler                             │
│    ├── ReservationConfirmedEventHandler (Task #88)          │
│    ├── ReservationCancelledEventHandler (Task #88)          │
│    └── ReservationRefundEventHandler (Issue #164)           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                 Consumer Layer (Infrastructure)              │
├─────────────────────────────────────────────────────────────┤
│  EventConsumer                                               │
│    - @KafkaListener                                          │
│    - EVENT_TYPE_MAP                                          │
│    - Dynamic handler routing                                 │
└─────────────────────────────────────────────────────────────┘
```

## 계층별 역할

### 1. Event Layer (Domain)

**위치**: `adapter.in.messaging.event`

**역할**: 이벤트 데이터 구조 정의

**설계 원칙**:
- 불변 객체 (final fields)
- Jackson 역직렬화 지원 (@JsonCreator, @JsonProperty)
- 추상 클래스로 공통 속성 정의

**Event 추상 클래스**:
```java
public abstract class Event {
    private final String topic;
    private final String eventType;

    public abstract String getEventTypeName();
}
```

**구체 클래스 예시 (RoomCreatedEvent)**:
```java
public final class RoomCreatedEvent extends Event {
    private static final String EVENT_TYPE_NAME = "RoomCreated";

    private final Long placeId;
    private final Long roomId;
    private final String timeSlot;

    @JsonCreator
    public RoomCreatedEvent(
        @JsonProperty("topic") String topic,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("placeId") Long placeId,
        @JsonProperty("roomId") Long roomId,
        @JsonProperty("timeSlot") String timeSlot
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
}
```

### 2. Handler Layer (Application)

**위치**: `adapter.in.messaging.handler`

**역할**: 특정 이벤트 타입 처리 로직

**설계 원칙**:
- Generic 인터페이스로 타입 안정성
- Single Responsibility: 하나의 이벤트 타입만 처리
- Use Case 호출로 비즈니스 로직 위임

**EventHandler 인터페이스**:
```java
public interface EventHandler<T extends Event> {
    void handle(T event);
    String getSupportedEventType();
}
```

**구체 클래스 예시 (RoomCreatedEventHandler)**:
```java
@Component
public class RoomCreatedEventHandler implements EventHandler<RoomCreatedEvent> {
    private final CreatePricingPolicyUseCase createPricingPolicyUseCase;

    @Override
    public void handle(RoomCreatedEvent event) {
        // 1. 이벤트 데이터 추출
        RoomId roomId = RoomId.of(event.getRoomId());
        PlaceId placeId = PlaceId.of(event.getPlaceId());
        TimeSlot timeSlot = TimeSlot.valueOf(event.getTimeSlot());

        // 2. Use Case 호출
        createPricingPolicyUseCase.createDefaultPolicy(roomId, placeId, timeSlot);
    }

    @Override
    public String getSupportedEventType() {
        return "RoomCreated";
    }
}
```

### 3. Consumer Layer (Infrastructure)

**위치**: `adapter.in.messaging.consumer`

**역할**: Kafka 메시지 수신 및 핸들러 라우팅

**설계 원칙**:
- Strategy Pattern: 런타임에 적절한 핸들러 선택
- Dependency Injection: 모든 핸들러를 List로 주입
- 확장성: 새 핸들러 추가 시 자동 인식

**EventConsumer**:
```java
@Component
public class EventConsumer {
    // 이벤트 타입 → 클래스 매핑
    private static final Map<String, Class<? extends Event>> EVENT_TYPE_MAP = new HashMap<>();

    static {
        EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
        // 새 이벤트 타입 추가 시 여기에 등록
    }

    private final JsonUtil jsonUtil;
    private final ObjectMapper objectMapper;
    private final List<EventHandler<?>> handlers; // 모든 핸들러 주입

    @KafkaListener(topics = "room-events", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment acknowledgment) {
        // 1. eventType 추출
        JsonNode rootNode = objectMapper.readTree(message);
        String eventType = rootNode.get("eventType").asText();

        // 2. 이벤트 클래스 찾기
        Class<? extends Event> eventClass = EVENT_TYPE_MAP.get(eventType);

        // 3. JSON → Event 객체 변환
        Event event = jsonUtil.fromJson(message, eventClass);

        // 4. 적절한 핸들러 찾기
        EventHandler<Event> handler = findHandler(eventType);

        // 5. 핸들러로 처리
        handler.handle(event);

        // 6. 수동 커밋
        acknowledgment.acknowledge();
    }

    private EventHandler<Event> findHandler(String eventType) {
        return handlers.stream()
            .filter(h -> h.getSupportedEventType().equals(eventType))
            .findFirst()
            .orElse(null);
    }
}
```

## 핵심 설계 패턴

### 1. Strategy Pattern
- EventHandler 인터페이스가 Strategy
- 각 구체 핸들러가 ConcreteStrategy
- EventConsumer가 Context (런타임에 Strategy 선택)

### 2. Template Method Pattern
- Event 추상 클래스가 Template
- `getEventTypeName()`이 hook method
- 구체 클래스가 구현

### 3. Dependency Injection
- Spring의 `List<EventHandler<?>>` 자동 주입
- 새 핸들러 추가 시 자동으로 목록에 포함

## 확장 시나리오

### 새 이벤트 타입 추가

**1단계**: Event 클래스 작성
```java
public final class SlotReservedEvent extends Event {
    private final Long reservationId;
    private final Long roomId;
    // ...
}
```

**2단계**: EventHandler 구현
```java
@Component
public class SlotReservedEventHandler implements EventHandler<SlotReservedEvent> {
    @Override
    public void handle(SlotReservedEvent event) {
        // 처리 로직
    }

    @Override
    public String getSupportedEventType() {
        return "SlotReserved";
    }
}
```

**3단계**: EVENT_TYPE_MAP에 등록
```java
static {
    EVENT_TYPE_MAP.put("RoomCreated", RoomCreatedEvent.class);
    EVENT_TYPE_MAP.put("SlotReserved", SlotReservedEvent.class); // 추가
}
```

**완료!** 기존 코드 수정 없이 새 이벤트 처리 가능

## 에러 처리 전략

### 현재 구현
```java
catch (Exception e) {
    logger.error("Failed to process message: {}", message, e);
    // TODO: DLQ(Dead Letter Queue)로 전송 또는 재처리 로직 구현
    acknowledgment.acknowledge(); // 실패해도 일단 acknowledge (무한 재시도 방지)
}
```

### 향후 개선 방향 (TODO)
1. **DLQ (Dead Letter Queue)**
   - 실패한 메시지를 별도 토픽으로 전송
   - 나중에 수동 재처리 또는 분석

2. **재시도 전략**
   - 일시적 오류: 지수 백오프로 재시도
   - 영구적 오류: DLQ로 이동

3. **모니터링**
   - 실패율 모니터링
   - 알람 설정

## 테스트 전략

### Unit Tests
- **EventConsumer**: Mock 핸들러로 라우팅 로직 테스트
- **EventHandler**: Mock Use Case로 처리 로직 테스트
- **Event**: 역직렬화 테스트

### Integration Tests (Future)
- Embedded Kafka 사용
- 실제 메시지 발행 → 수신 → 처리 검증

## 성능 고려사항

### 1. 수동 Acknowledge
- 처리 완료 후에만 커밋
- At-least-once 보장

### 2. 동시성
- Kafka Consumer는 단일 스레드
- 필요 시 `@KafkaListener` concurrency 설정 가능

### 3. 역직렬화
- JsonUtil 인터페이스 사용
- 구현체를 Jackson에서 다른 라이브러리로 교체 가능

---

**Last Updated**: 2025-11-12
