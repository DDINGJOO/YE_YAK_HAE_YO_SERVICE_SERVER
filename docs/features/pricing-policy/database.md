# PricingPolicy 데이터베이스 스키마

## ERD

```
┌─────────────────────────┐
│   pricing_policies      │
├─────────────────────────┤
│ room_id (PK)     BIGINT │
│ place_id         BIGINT │
│ time_slot        VARCHAR│
│ default_price   DECIMAL │
└─────────────────────────┘
            │
            │ 1:N
            │
            ▼
┌─────────────────────────┐
│  time_range_prices      │
├─────────────────────────┤
│ room_id (FK)     BIGINT │
│ day_of_week      VARCHAR│
│ start_time          TIME│
│ end_time            TIME│
│ price_per_slot  DECIMAL │
└─────────────────────────┘
```

## 테이블 상세

### pricing_policies

**목적**: 룸별 가격 정책 메인 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| room_id | BIGINT | PK, NOT NULL | 룸 ID (Aggregate ID) |
| place_id | BIGINT | NOT NULL | 장소 ID |
| time_slot | VARCHAR(20) | NOT NULL | 시간 단위 (HOUR, HALFHOUR) |
| default_price | DECIMAL(19,2) | NOT NULL | 기본 가격 |

**인덱스**:
- PRIMARY KEY: `room_id`
- INDEX: `idx_pricing_policies_place_id` on `place_id`

**설명**:
- room_id가 Aggregate ID이자 Primary Key
- place_id로 장소별 정책 조회 가능
- default_price는 시간대별 가격이 없을 때 적용되는 fallback 가격

### time_range_prices

**목적**: 시간대별 차등 가격 테이블 (ElementCollection)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| room_id | BIGINT | FK, NOT NULL | 룸 ID (부모 PK 참조) |
| day_of_week | VARCHAR(20) | NOT NULL | 요일 (MONDAY ~ SUNDAY) |
| start_time | TIME | NOT NULL | 시작 시간 |
| end_time | TIME | NOT NULL | 종료 시간 |
| price_per_slot | DECIMAL(19,2) | NOT NULL | 슬롯당 가격 |

**제약조건**:
- FOREIGN KEY: `room_id` REFERENCES `pricing_policies(room_id)` ON DELETE CASCADE
  - 가격 정책 삭제 시 시간대별 가격도 함께 삭제

**인덱스**:
- INDEX: `idx_time_range_prices_room_id` on `room_id`
- INDEX: `idx_time_range_prices_day_of_week` on `day_of_week`

**설명**:
- JPA @ElementCollection으로 매핑
- Composite Key 없음 (ElementCollection 특성)
- day_of_week + start_time + end_time 조합으로 시간대 식별

## JPA 매핑

### PricingPolicyEntity
```java
@Entity
@Table(name = "pricing_policies")
public class PricingPolicyEntity {
    @EmbeddedId
    private RoomIdEmbeddable roomId;

    @Embedded
    private PlaceIdEmbeddable placeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot")
    private TimeSlot timeSlot;

    @Column(name = "default_price", precision = 19, scale = 2)
    private BigDecimal defaultPrice;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "time_range_prices",
        joinColumns = @JoinColumn(name = "room_id")
    )
    private List<TimeRangePriceEmbeddable> timeRangePrices;
}
```

### RoomIdEmbeddable
```java
@Embeddable
public class RoomIdEmbeddable implements Serializable {
    @Column(name = "room_id", nullable = false)
    private Long value;
}
```

### TimeRangePriceEmbeddable
```java
@Embeddable
public class TimeRangePriceEmbeddable {
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "price_per_slot", nullable = false, precision = 19, scale = 2)
    private BigDecimal pricePerSlot;
}
```

## Flyway 마이그레이션

**파일**: `V2__create_pricing_policy_tables.sql`

### 주요 특징
1. **CASCADE 삭제**: 부모 정책 삭제 시 시간대별 가격도 자동 삭제
2. **인덱스 최적화**: place_id, room_id, day_of_week에 인덱스 생성
3. **코멘트**: 모든 테이블과 컬럼에 설명 추가
4. **타입 안정성**: VARCHAR(20)으로 ENUM 저장

### 마이그레이션 스크립트
```sql
-- 가격 정책 메인 테이블
CREATE TABLE pricing_policies (
    room_id        BIGINT         NOT NULL,
    place_id       BIGINT         NOT NULL,
    time_slot      VARCHAR(20)    NOT NULL,
    default_price  DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (room_id)
);

-- 시간대별 가격 테이블 (ElementCollection)
CREATE TABLE time_range_prices (
    room_id        BIGINT         NOT NULL,
    day_of_week    VARCHAR(20)    NOT NULL,
    start_time     TIME           NOT NULL,
    end_time       TIME           NOT NULL,
    price_per_slot DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (room_id) REFERENCES pricing_policies (room_id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_pricing_policies_place_id ON pricing_policies (place_id);
CREATE INDEX idx_time_range_prices_room_id ON time_range_prices (room_id);
CREATE INDEX idx_time_range_prices_day_of_week ON time_range_prices (day_of_week);
```

## 쿼리 예시

### 1. 룸별 가격 정책 조회
```sql
SELECT p.*, t.*
FROM pricing_policies p
LEFT JOIN time_range_prices t ON p.room_id = t.room_id
WHERE p.room_id = ?;
```

### 2. 장소별 모든 가격 정책 조회
```sql
SELECT p.*, t.*
FROM pricing_policies p
LEFT JOIN time_range_prices t ON p.room_id = t.room_id
WHERE p.place_id = ?;
```

### 3. 특정 요일의 시간대별 가격 조회
```sql
SELECT *
FROM time_range_prices
WHERE room_id = ?
  AND day_of_week = 'MONDAY';
```

## 데이터 예시

### pricing_policies
| room_id | place_id | time_slot | default_price |
|---------|----------|-----------|---------------|
| 1 | 100 | HOUR | 10000.00 |
| 2 | 100 | HALFHOUR | 5000.00 |

### time_range_prices
| room_id | day_of_week | start_time | end_time | price_per_slot |
|---------|-------------|------------|----------|----------------|
| 1 | MONDAY | 18:00:00 | 22:00:00 | 15000.00 |
| 1 | SATURDAY | 09:00:00 | 18:00:00 | 20000.00 |
| 2 | FRIDAY | 18:00:00 | 23:00:00 | 8000.00 |

## 성능 고려사항

### 인덱스 전략
1. **place_id**: 장소별 정책 조회 최적화
2. **room_id** (time_range_prices): JOIN 성능 최적화
3. **day_of_week**: 요일별 필터링 최적화

### N+1 문제 해결
- `FetchType.EAGER`로 시간대별 가격 즉시 로딩
- 한 번의 JOIN으로 모든 데이터 조회

### 트랜잭션 격리
- Aggregate 단위 트랜잭션
- 하나의 PricingPolicy는 하나의 트랜잭션에서만 수정
