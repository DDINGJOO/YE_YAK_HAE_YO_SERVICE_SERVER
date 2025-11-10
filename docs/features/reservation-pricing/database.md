# ReservationPricing 데이터베이스 스키마

## ERD

```
reservation_pricings
├── reservation_id (PK, BIGSERIAL)
├── room_id (FK, BIGINT, NOT NULL)
├── place_id (BIGINT, NOT NULL)
├── status (VARCHAR(20), NOT NULL)
├── time_slot (VARCHAR(10), NOT NULL)
├── total_price (DECIMAL(12,2), NOT NULL)
├── calculated_at (TIMESTAMP, NOT NULL)
└── expires_at (TIMESTAMP)

reservation_pricing_slots (ElementCollection)
├── reservation_id (FK, BIGINT, NOT NULL)
├── slot_time (TIMESTAMP, NOT NULL)
└── slot_price (DECIMAL(12,2), NOT NULL)
    PRIMARY KEY (reservation_id, slot_time)

reservation_pricing_products (ElementCollection)
├── reservation_id (FK, BIGINT, NOT NULL)
├── product_id (BIGINT, NOT NULL)
├── product_name (VARCHAR(255), NOT NULL)
├── quantity (INTEGER, NOT NULL)
├── unit_price (DECIMAL(12,2), NOT NULL)
├── total_price (DECIMAL(12,2), NOT NULL)
└── pricing_type (VARCHAR(30), NOT NULL)
```

## 테이블 상세

### reservation_pricings
예약 가격 스냅샷 메인 테이블

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| reservation_id | BIGSERIAL | PK | 예약 ID (자동 증가) |
| room_id | BIGINT | NOT NULL | 룸 ID |
| place_id | BIGINT | NOT NULL | 플레이스 ID (비정규화, 쿼리 최적화) |
| status | VARCHAR(20) | NOT NULL, CHECK | 예약 상태 (PENDING, CONFIRMED, CANCELLED) |
| time_slot | VARCHAR(10) | NOT NULL, CHECK | 시간 단위 (HOUR, HALFHOUR) |
| total_price | DECIMAL(12,2) | NOT NULL | 총 가격 (시간대 + 상품 합계) |
| calculated_at | TIMESTAMP | NOT NULL | 가격 계산 시각 |
| expires_at | TIMESTAMP | NULL | PENDING 만료 시각 (타임아웃 관리) |

**제약조건**:
```sql
CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
CONSTRAINT chk_time_slot CHECK (time_slot IN ('HOUR', 'HALFHOUR'))
```

### reservation_pricing_slots
시간대별 가격 내역 테이블 (@ElementCollection)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| reservation_id | BIGINT | FK, NOT NULL | 예약 ID |
| slot_time | TIMESTAMP | NOT NULL | 시간 슬롯 시작 시각 |
| slot_price | DECIMAL(12,2) | NOT NULL | 해당 슬롯의 가격 |

**Primary Key**: (reservation_id, slot_time)

**Foreign Key**:
```sql
CONSTRAINT fk_reservation_pricing_slots
    FOREIGN KEY (reservation_id)
    REFERENCES reservation_pricings (reservation_id)
    ON DELETE CASCADE
```

### reservation_pricing_products
상품별 가격 내역 테이블 (@ElementCollection)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| reservation_id | BIGINT | FK, NOT NULL | 예약 ID |
| product_id | BIGINT | NOT NULL | 상품 ID (스냅샷) |
| product_name | VARCHAR(255) | NOT NULL | 상품명 (스냅샷) |
| quantity | INTEGER | NOT NULL | 수량 |
| unit_price | DECIMAL(12,2) | NOT NULL | 단가 (스냅샷) |
| total_price | DECIMAL(12,2) | NOT NULL | 총 가격 (단가 × 수량) |
| pricing_type | VARCHAR(30) | NOT NULL, CHECK | 가격 책정 방식 |

**제약조건**:
```sql
CONSTRAINT chk_pricing_type CHECK (
    pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK')
)
```

**Foreign Key**:
```sql
CONSTRAINT fk_reservation_pricing_products
    FOREIGN KEY (reservation_id)
    REFERENCES reservation_pricings (reservation_id)
    ON DELETE CASCADE
```

## 인덱스 전략

### reservation_pricings 테이블
```sql
-- 기본 조회
CREATE INDEX idx_reservation_pricings_room_id
    ON reservation_pricings (room_id);

CREATE INDEX idx_reservation_pricings_place_id
    ON reservation_pricings (place_id);

-- 상태별 조회
CREATE INDEX idx_reservation_pricings_status
    ON reservation_pricings (status);

-- 시간 기반 조회 (타임아웃 처리)
CREATE INDEX idx_reservation_pricings_calculated_at
    ON reservation_pricings (calculated_at);

-- 복합 인덱스 (재고 관리용)
CREATE INDEX idx_reservation_pricings_room_status
    ON reservation_pricings (room_id, status);

CREATE INDEX idx_reservation_pricings_place_status
    ON reservation_pricings (place_id, status);
```

**사용 사례**:
- `idx_reservation_pricings_room_id`: 특정 룸의 예약 조회
- `idx_reservation_pricings_place_id`: 특정 플레이스의 예약 조회
- `idx_reservation_pricings_status`: 상태별 예약 조회 (PENDING 타임아웃)
- `idx_reservation_pricings_room_status`: ROOM Scope 재고 관리
- `idx_reservation_pricings_place_status`: PLACE Scope 재고 관리

### reservation_pricing_slots 테이블
```sql
-- 시간 슬롯 기반 조회
CREATE INDEX idx_reservation_pricing_slots_time
    ON reservation_pricing_slots (slot_time);

-- 복합 인덱스 (시간대별 재고 조회 최적화)
CREATE INDEX idx_reservation_pricing_slots_reservation_time
    ON reservation_pricing_slots (reservation_id, slot_time);
```

**사용 사례**:
- `idx_reservation_pricing_slots_time`: 특정 시간대 예약 조회
- `idx_reservation_pricing_slots_reservation_time`: 예약별 시간 슬롯 조회 최적화

### reservation_pricing_products 테이블
```sql
-- 상품 기반 조회
CREATE INDEX idx_reservation_pricing_products_product_id
    ON reservation_pricing_products (product_id);
```

**사용 사례**:
- `idx_reservation_pricing_products_product_id`: 특정 상품을 포함한 예약 조회

## JPA 매핑

### ReservationPricingEntity
```java
@Entity
@Table(name = "reservation_pricings")
public class ReservationPricingEntity {

    @Id
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false, length = 10)
    private TimeSlot timeSlot;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "reservation_pricing_slots",
        joinColumns = @JoinColumn(name = "reservation_id")
    )
    @MapKeyColumn(name = "slot_time")
    @Column(name = "slot_price", precision = 12, scale = 2)
    private Map<LocalDateTime, BigDecimal> slotPrices = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "reservation_pricing_products",
        joinColumns = @JoinColumn(name = "reservation_id")
    )
    private List<ProductPriceBreakdownEmbeddable> productBreakdowns = new ArrayList<>();
}
```

### ProductPriceBreakdownEmbeddable
```java
@Embeddable
public class ProductPriceBreakdownEmbeddable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 30)
    private PricingType pricingType;
}
```

## Flyway Migration

### V4__create_reservation_pricing_tables.sql
**목적**: 초기 테이블 생성

```sql
-- 메인 테이블
CREATE TABLE reservation_pricings (
    reservation_id BIGSERIAL PRIMARY KEY,
    room_id        BIGINT         NOT NULL,
    place_id       BIGINT         NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    time_slot      VARCHAR(10)    NOT NULL,
    total_price    DECIMAL(12, 2) NOT NULL,
    calculated_at  TIMESTAMP      NOT NULL,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_time_slot CHECK (time_slot IN ('HOUR', 'HALFHOUR'))
);

-- 시간대별 가격 테이블
CREATE TABLE reservation_pricing_slots (
    reservation_id BIGINT         NOT NULL,
    slot_time      TIMESTAMP      NOT NULL,
    slot_price     DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (reservation_id, slot_time),
    CONSTRAINT fk_reservation_pricing_slots
        FOREIGN KEY (reservation_id)
        REFERENCES reservation_pricings (reservation_id)
        ON DELETE CASCADE
);

-- 상품별 가격 테이블
CREATE TABLE reservation_pricing_products (
    reservation_id BIGINT         NOT NULL,
    product_id     BIGINT         NOT NULL,
    product_name   VARCHAR(255)   NOT NULL,
    quantity       INTEGER        NOT NULL,
    unit_price     DECIMAL(12, 2) NOT NULL,
    total_price    DECIMAL(12, 2) NOT NULL,
    pricing_type   VARCHAR(30)    NOT NULL,
    CONSTRAINT fk_reservation_pricing_products
        FOREIGN KEY (reservation_id)
        REFERENCES reservation_pricings (reservation_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_pricing_type CHECK (
        pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK')
    )
);

-- 인덱스 생성
CREATE INDEX idx_reservation_pricings_room_id ON reservation_pricings (room_id);
CREATE INDEX idx_reservation_pricings_place_id ON reservation_pricings (place_id);
CREATE INDEX idx_reservation_pricings_status ON reservation_pricings (status);
CREATE INDEX idx_reservation_pricings_calculated_at ON reservation_pricings (calculated_at);
CREATE INDEX idx_reservation_pricings_room_status ON reservation_pricings (room_id, status);
CREATE INDEX idx_reservation_pricings_place_status ON reservation_pricings (place_id, status);

CREATE INDEX idx_reservation_pricing_slots_time ON reservation_pricing_slots (slot_time);
CREATE INDEX idx_reservation_pricing_slots_reservation_time
    ON reservation_pricing_slots (reservation_id, slot_time);

CREATE INDEX idx_reservation_pricing_products_product_id
    ON reservation_pricing_products (product_id);
```

### V7__add_expires_at_to_reservation_pricings.sql
**목적**: PENDING 타임아웃 관리를 위한 컬럼 추가

```sql
ALTER TABLE reservation_pricings
ADD COLUMN expires_at TIMESTAMP;

COMMENT ON COLUMN reservation_pricings.expires_at IS
    'PENDING 상태의 예약 만료 시각. 타임아웃 관리에 사용.';
```

## 재고 관리 쿼리

### PLACE Scope 재고 조회
```java
@Query("""
    SELECT rp FROM ReservationPricingEntity rp
    WHERE rp.placeId = :placeId
      AND rp.status IN :statuses
      AND EXISTS (
          SELECT 1 FROM rp.slotPrices slot
          WHERE KEY(slot) >= :startTime
            AND KEY(slot) < :endTime
      )
    """)
List<ReservationPricingEntity> findByPlaceIdAndTimeRange(
    @Param("placeId") Long placeId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime,
    @Param("statuses") List<ReservationStatus> statuses
);
```

### ROOM Scope 재고 조회
```java
@Query("""
    SELECT rp FROM ReservationPricingEntity rp
    WHERE rp.roomId = :roomId
      AND rp.status IN :statuses
      AND EXISTS (
          SELECT 1 FROM rp.slotPrices slot
          WHERE KEY(slot) >= :startTime
            AND KEY(slot) < :endTime
      )
    """)
List<ReservationPricingEntity> findByRoomIdAndTimeRange(
    @Param("roomId") Long roomId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime,
    @Param("statuses") List<ReservationStatus> statuses
);
```

### PENDING 타임아웃 조회
```java
@Query("""
    SELECT rp FROM ReservationPricingEntity rp
    WHERE rp.status = 'PENDING'
      AND rp.expiresAt < :now
    """)
List<ReservationPricingEntity> findExpiredPending(
    @Param("now") LocalDateTime now
);
```

## 성능 최적화 전략

### 1. ElementCollection EAGER Fetch
**선택 이유**:
- 시간대별 가격과 상품별 가격은 항상 함께 조회됨
- N+1 문제 방지
- 단일 JOIN으로 모든 데이터 로드

**Trade-off**:
- 메모리 사용량 증가
- 예약당 슬롯/상품 수가 많지 않아 문제되지 않음

### 2. PlaceId 비정규화
**목적**: PLACE Scope 재고 조회 성능 향상

**효과**:
- RoomId → PlaceId 조인 없이 직접 조회
- 인덱스 `idx_reservation_pricings_place_status` 활용

**Trade-off**:
- 저장 공간 증가 (BIGINT 8 bytes per row)
- 일관성 관리 필요 (Room의 PlaceId 변경 시)

### 3. 복합 인덱스
**전략**: (room_id, status), (place_id, status)

**사용 사례**:
- 재고 조회: "특정 룸/플레이스의 PENDING + CONFIRMED 예약"
- 커버링 인덱스 효과 (인덱스만으로 필터링 완료)

### 4. ON DELETE CASCADE
**목적**: 자식 테이블 자동 삭제

**효과**:
- 애플리케이션 코드 단순화
- 트랜잭션 일관성 보장

## 데이터 예시

### reservation_pricings
| reservation_id | room_id | place_id | status | time_slot | total_price | calculated_at | expires_at |
|----------------|---------|----------|--------|-----------|-------------|---------------|------------|
| 1 | 10 | 100 | PENDING | HOUR | 30000.00 | 2025-01-15 10:00:00 | 2025-01-15 10:10:00 |
| 2 | 10 | 100 | CONFIRMED | HOUR | 25000.00 | 2025-01-15 11:00:00 | 2025-01-15 11:10:00 |
| 3 | 11 | 100 | CANCELLED | HALFHOUR | 15000.00 | 2025-01-15 12:00:00 | 2025-01-15 12:10:00 |

### reservation_pricing_slots
| reservation_id | slot_time | slot_price |
|----------------|-----------|------------|
| 1 | 2025-01-15 10:00:00 | 10000.00 |
| 1 | 2025-01-15 11:00:00 | 10000.00 |
| 1 | 2025-01-15 12:00:00 | 10000.00 |
| 2 | 2025-01-15 14:00:00 | 15000.00 |
| 2 | 2025-01-15 15:00:00 | 10000.00 |

### reservation_pricing_products
| reservation_id | product_id | product_name | quantity | unit_price | total_price | pricing_type |
|----------------|------------|--------------|----------|------------|-------------|--------------|
| 1 | 1 | 빔프로젝터 | 1 | 10000.00 | 10000.00 | ONE_TIME |
| 1 | 2 | 아메리카노 | 3 | 2000.00 | 6000.00 | SIMPLE_STOCK |
| 2 | 3 | 노트북 | 2 | 10000.00 | 15000.00 | INITIAL_PLUS_ADDITIONAL |

## 설계 결정 사항

### 1. BIGSERIAL vs Snowflake ID
**선택**: BIGSERIAL (Auto Increment)

**근거**:
- 단순성: PostgreSQL 내장 기능
- 순차성: 인덱스 성능 최적화
- 충돌 방지: DB 레벨에서 보장

**Trade-off**:
- 분산 환경에서 ID 범위 분할 필요 시 Snowflake 고려

### 2. ElementCollection vs OneToMany
**선택**: @ElementCollection

**근거**:
- 시간대/상품 가격은 예약의 일부 (독립적 엔티티 아님)
- 별도 Entity 불필요
- Cascade 자동 처리

**Trade-off**:
- 개별 업데이트 불가 (전체 재저장)
- 예약 가격은 스냅샷이므로 개별 업데이트 불필요

### 3. EAGER Fetch
**선택**: FetchType.EAGER

**근거**:
- 예약 조회 시 항상 모든 정보 필요
- N+1 문제 방지
- 단일 쿼리로 완전한 객체 로드

**Trade-off**:
- 메모리 사용량 증가 (미미함)

### 4. ON DELETE CASCADE
**선택**: 모든 FK에 CASCADE 설정

**근거**:
- 예약 삭제 시 관련 데이터 자동 삭제
- 데이터 일관성 보장
- 애플리케이션 코드 단순화

## 모니터링 및 유지보수

### 인덱스 사용 통계 확인
```sql
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename LIKE 'reservation_pricing%'
ORDER BY idx_scan DESC;
```

### 테이블 크기 확인
```sql
SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_total_relation_size(relid) - pg_relation_size(relid)) AS indexes_size
FROM pg_catalog.pg_statio_user_tables
WHERE relname LIKE 'reservation_pricing%'
ORDER BY pg_total_relation_size(relid) DESC;
```

### 슬로우 쿼리 분석
```sql
EXPLAIN ANALYZE
SELECT rp.*
FROM reservation_pricings rp
WHERE rp.place_id = 100
  AND rp.status IN ('PENDING', 'CONFIRMED')
  AND EXISTS (
      SELECT 1
      FROM reservation_pricing_slots s
      WHERE s.reservation_id = rp.reservation_id
        AND s.slot_time >= '2025-01-15 10:00:00'
        AND s.slot_time < '2025-01-15 14:00:00'
  );
```

## 데이터 보관 정책

### 아카이빙 전략
- **기준**: calculated_at 기준 1년 이상 경과한 CANCELLED 예약
- **방법**: 별도 아카이브 테이블로 이동
- **주기**: 월 1회 배치 작업

### 백업 전략
- **주기**: 일 1회 전체 백업
- **보관**: 30일
- **복구 테스트**: 월 1회

## 참고 자료
- PostgreSQL Documentation: [SERIAL Types](https://www.postgresql.org/docs/16/datatype-numeric.html#DATATYPE-SERIAL)
- JPA Specification: [@ElementCollection](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)