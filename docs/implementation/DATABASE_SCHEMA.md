# Database Schema Documentation

예약 가격 관리 서비스의 데이터베이스 스키마 문서입니다.

---

## ID 생성 전략

본 프로젝트는 **Snowflake ID Generator**를 사용하여 분산 환경에서 고유한 ID를 생성합니다.

### Snowflake ID 구조

```
[Sign Bit 1bit] [Timestamp 41bits] [Node ID 10bits] [Sequence 12bits]
```

- **Timestamp (41 bits)**: Custom Epoch (2024-01-01T00:00:00Z) 기준 밀리초
- **Node ID (10 bits)**: 노드 식별자 (최대 1024개 노드 지원)
- **Sequence (12 bits)**: 밀리초당 순차 번호 (밀리초당 최대 4096개 ID 생성)

### 주요 특징

- **고유성 보장**: 분산 시스템에서 충돌 없이 고유 ID 생성
- **시간 기반 정렬**: ID 자체로 생성 순서 파악 가능
- **높은 처리량**: 단일 노드에서 초당 최대 400만개 ID 생성 가능
- **64-bit Long**: Java Long 타입으로 표현 가능

### 적용 대상

- `products.product_id`: BIGINT (Snowflake ID)
- `reservation_pricings.reservation_id`: BIGINT (Snowflake ID)

### 구현

```java
@Entity
public class ProductEntity {
  @Id
  @GeneratedValue(generator = "snowflake-id")
  @GenericGenerator(
      name = "snowflake-id",
      type = SnowflakeIdGenerator.class
  )
  private Long id;
}
```

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PricingPolicy Aggregate                         │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐
│   pricing_policies       │
├──────────────────────────┤
│ PK  id                   │ BIGSERIAL
│     room_id              │ BIGINT (외부 서비스 참조)
│     place_id             │ BIGINT (외부 서비스 참조)
│     day_of_week          │ VARCHAR(10) [MONDAY~SUNDAY]
│     start_time           │ TIME
│     end_time             │ TIME
│     price                │ DECIMAL(10,2)
│     created_at           │ TIMESTAMP
│     updated_at           │ TIMESTAMP
└──────────────────────────┘

Constraints:
- CHECK: start_time < end_time
- CHECK: price >= 0
- UNIQUE: (room_id, day_of_week, start_time, end_time)

Indexes:
- idx_pricing_policies_room_id (room_id)
- idx_pricing_policies_place_id (place_id)
- idx_pricing_policies_day_time (day_of_week, start_time, end_time)

---

┌─────────────────────────────────────────────────────────────────────────┐
│                           Product Aggregate                             │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐
│       products           │
├──────────────────────────┤
│ PK  product_id           │ BIGSERIAL
│     scope                │ VARCHAR(20) [PLACE/ROOM/RESERVATION]
│     place_id             │ BIGINT (nullable)
│     room_id              │ BIGINT (nullable)
│     name                 │ VARCHAR(255)
│     pricing_type         │ VARCHAR(50) [INITIAL_PLUS_ADDITIONAL/ONE_TIME/SIMPLE_STOCK]
│     initial_price        │ DECIMAL(19,2)
│     additional_price     │ DECIMAL(19,2) (nullable)
│     total_quantity       │ INTEGER DEFAULT 0
│     reserved_quantity    │ INTEGER DEFAULT 0 (V9)
└──────────────────────────┘

Constraints:
- CHECK: scope IN ('PLACE', 'ROOM', 'RESERVATION')
- CHECK: pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK')
- CHECK: (scope = 'PLACE' AND place_id IS NOT NULL AND room_id IS NULL) OR
         (scope = 'ROOM' AND place_id IS NOT NULL AND room_id IS NOT NULL) OR
         (scope = 'RESERVATION' AND place_id IS NULL AND room_id IS NULL)
- CHECK: (pricing_type = 'INITIAL_PLUS_ADDITIONAL' AND additional_price IS NOT NULL) OR
         (pricing_type IN ('ONE_TIME', 'SIMPLE_STOCK') AND additional_price IS NULL)
- CHECK: (reserved_quantity <= total_quantity) (V9)

Indexes:
- idx_products_scope (scope)
- idx_products_place_id (place_id) WHERE place_id IS NOT NULL
- idx_products_room_id (room_id) WHERE room_id IS NOT NULL
- idx_products_scope_place_id (scope, place_id) WHERE place_id IS NOT NULL

┌─────────────────────────────────┐
│ product_time_slot_inventory     │ (V10)
├─────────────────────────────────┤
│ PK  product_id                  │ BIGINT
│ PK  inventory_date              │ DATE
│ PK  slot_time                   │ TIME
│     total_quantity              │ INTEGER DEFAULT 0
│     reserved_quantity           │ INTEGER DEFAULT 0
│     created_at                  │ TIMESTAMP
│     updated_at                  │ TIMESTAMP
└─────────────────────────────────┘

Constraints:
- CHECK: (reserved_quantity <= total_quantity)
- FK: product_id → products.product_id ON DELETE CASCADE

Indexes:
- idx_product_time_slot_inventory_product_date (product_id, inventory_date)
- idx_product_time_slot_inventory_date_slot (inventory_date, slot_time)

Partitioning:
- Monthly partitioning by inventory_date
- Partition template: product_time_slot_inventory_YYYYMM

Note:
- ROOM/PLACE scope 상품의 시간대별 재고 관리 (V10 추가)
- RESERVATION scope 상품은 이 테이블 미사용 (products 테이블의 재고만 사용)
- 월별 파티셔닝으로 대용량 데이터 효율적 관리

┌──────────────────────────┐
│ room_allowed_products    │
├──────────────────────────┤
│ PK  id                   │ BIGSERIAL
│ FK  product_id           │ BIGINT → products.product_id
│     room_id              │ BIGINT (외부 서비스 참조)
│     created_at           │ TIMESTAMP
└──────────────────────────┘

Constraints:
- UNIQUE: (room_id, product_id)
- FK: product_id → products.product_id ON DELETE CASCADE

Indexes:
- idx_room_allowed_products_room_id (room_id)

Note: PLACE 범위 상품의 룸별 사용 가능 여부를 화이트리스트 방식으로 관리 (V6 추가)

---

┌─────────────────────────────────────────────────────────────────────────┐
│                    ReservationPricing Aggregate                         │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐         ┌──────────────────────────────┐
│ reservation_pricings     │ 1     * │ reservation_pricing_slots    │
├──────────────────────────┤◄────────├──────────────────────────────┤
│ PK  reservation_id       │         │ FK  reservation_id           │
│     room_id              │         │     slot_time                │
│     place_id             │         │     slot_price               │
│     status               │         └──────────────────────────────┘
│     time_slot            │
│     total_price          │         ┌──────────────────────────────┐
│     calculated_at        │ 1     * │ reservation_pricing_products │
│     expires_at           │◄────────├──────────────────────────────┤
└──────────────────────────┘         │ FK  reservation_id           │
                                     │     product_id               │
                                     │     product_name             │
                                     │     quantity                 │
                                     │     unit_price               │
                                     │     total_price              │
                                     │     pricing_type             │
                                     └──────────────────────────────┘

reservation_pricings:
- Immutable snapshot of pricing at reservation time
- CHECK: status IN ('PENDING', 'CONFIRMED', 'CANCELLED')
- CHECK: time_slot IN ('HOUR', 'HALFHOUR')
- expires_at: PENDING 예약 자동 만료 시각 (V7 추가)

reservation_pricing_slots:
- Stores time-slot prices (ElementCollection)
- PK: (reservation_id, slot_time)
- ON DELETE CASCADE

reservation_pricing_products:
- Stores product prices (ElementCollection)
- Snapshot of product info at reservation time
- ON DELETE CASCADE

Indexes:
- idx_reservation_pricings_room_id (room_id)
- idx_reservation_pricings_place_id (place_id)
- idx_reservation_pricings_status (status)
- idx_reservation_pricings_calculated_at (calculated_at)
- idx_reservation_pricings_room_status (room_id, status)
- idx_reservation_pricings_place_status (place_id, status)
- idx_reservation_pricings_status_expires_at (status, expires_at) WHERE status = 'PENDING' (V7 추가)
- idx_reservation_pricing_slots_time (slot_time)
- idx_reservation_pricing_slots_reservation_time (reservation_id, slot_time)
- idx_reservation_pricing_products_product_id (product_id)

---

┌─────────────────────────────────────────────────────────────────────────┐
│                          Infrastructure Tables                          │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐
│       shedlock           │
├──────────────────────────┤
│ PK  name                 │ VARCHAR(64)
│     lock_until           │ TIMESTAMP
│     locked_at            │ TIMESTAMP
│     locked_by            │ VARCHAR(255)
└──────────────────────────┘

Note: 분산 스케줄링 잠금 관리 테이블 (V8 추가)
- ShedLock 라이브러리가 사용하는 테이블
- 여러 서비스 인스턴스 환경에서 스케줄 작업 중복 실행 방지
```

---

## 테이블 상세 설명

### 1. pricing_policies (가격 정책)

**목적**: 룸별 시간대별 가격 정책을 저장합니다.

**도메인 규칙**:
- 같은 룸, 같은 요일 내에서 시간대가 중복될 수 없습니다
- 시작 시간은 종료 시간보다 이전이어야 합니다
- 가격은 0원 이상이어야 합니다

**주요 컬럼**:
- `room_id`: 외부 서비스(룸 관리 서비스)의 룸 ID 참조
- `place_id`: 외부 서비스(플레이스 관리 서비스)의 플레이스 ID 참조
- `day_of_week`: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
- `start_time`, `end_time`: 시간대 범위
- `price`: 해당 시간대 가격

**인덱싱 전략**:
- 룸별 조회가 가장 빈번하므로 `room_id`에 인덱스
- 요일 및 시간대 기반 검색을 위한 복합 인덱스

---

### 2. products (추가상품)

**목적**: 추가상품 정보 및 재고를 관리합니다.

**도메인 규칙**:
- scope와 place_id/room_id의 조합이 일관성을 유지해야 합니다
  - PLACE: place_id NOT NULL, room_id NULL
  - ROOM: place_id NOT NULL, room_id NOT NULL
  - RESERVATION: place_id NULL, room_id NULL
- pricing_type에 따라 additional_price 필수 여부가 결정됩니다
  - INITIAL_PLUS_ADDITIONAL: additional_price 필수
  - ONE_TIME, SIMPLE_STOCK: additional_price NULL

**주요 컬럼**:
- `scope`: 적용 범위 타입 (PLACE, ROOM, RESERVATION)
  - PLACE: 플레이스 전체에 적용
  - ROOM: 특정 룸에만 적용
  - RESERVATION: 예약별로 적용 (시간 독립적)
- `place_id`: 플레이스 ID (PLACE, ROOM scope일 때 사용)
- `room_id`: 룸 ID (ROOM scope일 때만 사용)
- `pricing_type`: 가격 책정 전략
  - INITIAL_PLUS_ADDITIONAL: 초기 가격 + 추가 가격 (1개 초과 시)
  - ONE_TIME: 1회 대여 가격 (수량 무관)
  - SIMPLE_STOCK: 단순 재고 기반 가격 (단가 × 수량)
- `initial_price`: 초기 가격 또는 단가
- `additional_price`: 추가 가격 (INITIAL_PLUS_ADDITIONAL만 사용)
- `total_quantity`: 총 재고 수량
- `reserved_quantity`: 현재 예약된 수량 (V9에서 추가)

**재고 관리 및 동시성 제어**:

재고는 **Atomic UPDATE 방식**으로 동시성을 제어합니다 (Issue #138, ADR_002):

1. **재고 계산**:
   - 가용 수량 = `total_quantity - reserved_quantity`
   - PENDING/CONFIRMED 상태의 예약만 `reserved_quantity`에 반영

2. **원자적 재고 예약** (Concurrency Control):
```sql
UPDATE products
SET reserved_quantity = reserved_quantity + ?
WHERE product_id = ?
  AND (total_quantity - reserved_quantity) >= ?
```
- WHERE 절에서 재고 검증과 차감을 원자적으로 수행
- DB Row Lock으로 동시 요청 순차 처리
- UPDATE 성공 시 재고 예약 성공, 실패 시 재고 부족

3. **재고 롤백** (여러 상품 예약 실패 시):
```sql
UPDATE products
SET reserved_quantity = reserved_quantity - ?
WHERE product_id = ?
```
- 부분 예약 실패 시 이전 예약 롤백
- @Transactional로 전체 롤백 보장

4. **재고 해제** (예약 취소/환불 시, Issue #157, #164):
```sql
-- RESERVATION Scope 상품
UPDATE products
SET reserved_quantity = reserved_quantity - ?
WHERE product_id = ?

-- ROOM/PLACE Scope 상품 (시간대별)
UPDATE product_time_slot_inventory
SET reserved_quantity = reserved_quantity - ?
WHERE product_id = ? AND inventory_date = ? AND slot_time IN (?)
```
- ReservationRefundEventHandler가 명시적으로 재고 해제 (Issue #164)
- ProductRepository.releaseQuantity() 호출로 원자적 재고 복원
- RESERVATION Scope: products 테이블 업데이트
- ROOM/PLACE Scope: product_time_slot_inventory 테이블 업데이트
- @Transactional로 일관성 보장

**장점**:
- 오버부킹 완전 방지 (DB 원자성 보장)
- 락 경합 없음 (높은 처리량: 50 TPS)
- 데드락 위험 없음

---

### 3. reservation_pricings (예약 가격 스냅샷)

**목적**: 예약 시점의 가격 정보를 불변 스냅샷으로 저장합니다.

**도메인 규칙**:
- 예약 가격은 생성 후 변경 불가 (Immutable)
- 총 가격 = 모든 시간대 가격 합계 + 모든 추가상품 가격의 합
- 모든 금액은 0원 이상

**주요 컬럼**:
- `reservation_id`: 예약 ID (PK, Auto Increment)
- `room_id`: 룸 ID
- `place_id`: 플레이스 ID (쿼리 효율성을 위한 비정규화)
- `status`: 예약 상태
  - PENDING: 대기 중
  - CONFIRMED: 확정됨
  - CANCELLED: 취소됨
- `time_slot`: 시간 단위 (HOUR: 1시간, HALFHOUR: 30분)
- `total_price`: 총 가격 (시간대 가격 합계 + 상품 가격 합계)
- `calculated_at`: 가격 계산 시각 (생성 시각)
- `expires_at`: PENDING 예약 자동 만료 시각 (V7에서 추가)
  - PENDING 상태 예약이 이 시각을 초과하면 자동 취소됨
  - 기본 타임아웃: 10분

**특징**:
- `updated_at` 컬럼 없음 (불변 객체)
- 예약 취소 시에도 데이터 유지 (이력 관리)
- status만 CANCELLED로 변경 가능
- 스케줄러가 주기적으로 만료된 PENDING 예약을 자동 취소

---

### 4. room_allowed_products (룸별 허용 상품)

**목적**: PLACE 범위 상품의 룸별 사용 가능 여부를 화이트리스트 방식으로 관리합니다 (V6에서 추가).

**도메인 규칙**:
- 하나의 룸에 동일한 상품을 중복 등록할 수 없습니다
- PLACE 범위 상품만 등록 가능합니다
- 상품이 삭제되면 매핑도 함께 삭제됩니다 (CASCADE)

**주요 컬럼**:
- `id`: 매핑 ID (PK, Auto Increment)
- `room_id`: 룸 ID (외부 서비스 참조)
- `product_id`: 허용된 PLACE 상품 ID (FK: products.product_id)
- `created_at`: 생성 일시

**사용 시나리오**:
- PLACE 상품은 기본적으로 플레이스 전체에 적용되지만
- 특정 룸에서만 사용 가능하도록 제한하려면 이 테이블에 매핑을 추가
- 매핑이 없으면 해당 룸에서는 해당 PLACE 상품 사용 불가

---

### 5. reservation_pricing_slots (예약 시간대별 가격)

**목적**: 예약에 포함된 시간대별 가격을 스냅샷으로 저장합니다.

**주요 컬럼**:
- `reservation_id`: 예약 ID (FK)
- `slot_time`: 시간 슬롯 (예: 2025-01-15 10:00:00)
- `slot_price`: 해당 슬롯의 가격

**특징**:
- ElementCollection 매핑 (JPA)
- 복합 기본키 (reservation_id, slot_time)
- 예약 삭제 시 CASCADE로 함께 삭제
- 스냅샷이므로 pricing_policies 테이블과 FK 관계 없음

---

### 6. reservation_pricing_products (예약 상품별 가격)

**목적**: 예약에 포함된 추가상품 정보를 스냅샷으로 저장합니다.

**주요 컬럼**:
- `reservation_id`: 예약 ID (FK)
- `product_id`: 상품 ID (스냅샷, 참조용)
- `product_name`: 상품명 스냅샷 (상품 이름 변경되어도 유지)
- `quantity`: 수량
- `unit_price`: 단가 (스냅샷)
- `total_price`: 총 가격 (가격 전략에 따라 계산된 값)
- `pricing_type`: 가격 책정 방식 (스냅샷)

**특징**:
- ElementCollection 매핑 (JPA)
- 스냅샷이므로 products 테이블과 FK 관계 없음
- `ON DELETE CASCADE`: 예약 가격 삭제 시 함께 삭제

---

### 7. shedlock (분산 스케줄링 잠금)

**목적**: 여러 서비스 인스턴스 환경에서 스케줄 작업의 중복 실행을 방지합니다 (V8에서 추가).

**사용 라이브러리**: ShedLock (net.javacrumbs.shedlock)

**도메인 규칙**:
- 하나의 스케줄 작업은 하나의 인스턴스에서만 실행되어야 합니다
- 잠금은 특정 시간이 지나면 자동으로 해제됩니다
- 작업명은 고유해야 합니다

**주요 컬럼**:
- `name`: 스케줄 작업명 (PK)
- `lock_until`: 잠금 유지 시각 (이 시각까지 다른 인스턴스가 작업 실행 불가)
- `locked_at`: 잠금 획득 시각
- `locked_by`: 잠금을 획득한 인스턴스 식별자 (hostname)

**사용 시나리오**:
- **만료 예약 자동 취소**: PENDING 상태인 예약 중 expires_at를 초과한 건을 주기적으로 취소
- **재고 동기화**: 주기적으로 재고 상태를 검증하고 동기화
- **통계 집계**: 일일/주간 통계를 특정 시간에 집계

**예시**:
```java
@Scheduled(cron = "*/1 * * * * *")
@SchedulerLock(
    name = "cancelExpiredReservations",
    lockAtMostFor = "10m",
    lockAtLeastFor = "1m"
)
public void cancelExpiredReservations() {
    // PENDING이면서 만료된 예약 자동 취소
}
```

---

### 8. product_time_slot_inventory (시간대별 상품 재고)

**목적**: ROOM/PLACE scope 상품의 시간대별 재고를 관리합니다 (V10에서 추가).

**도메인 규칙**:
- ROOM/PLACE scope 상품만 이 테이블 사용
- RESERVATION scope 상품은 products 테이블의 재고만 사용
- 재고 소진 방지를 위해 reserved_quantity <= total_quantity 제약
- 상품이 삭제되면 재고 데이터도 함께 삭제 (CASCADE)

**주요 컬럼**:
- `product_id`: 상품 ID (FK: products.product_id)
- `inventory_date`: 재고 날짜 (예: 2025-01-15)
- `slot_time`: 시간 슬롯 (예: 10:00:00)
- `total_quantity`: 해당 시간대의 총 재고 수량
- `reserved_quantity`: 해당 시간대의 예약된 수량
- `created_at`: 생성 일시
- `updated_at`: 수정 일시

**파티셔닝 전략**:
- **월별 파티셔닝**: `inventory_date` 기준
- 파티션 이름 형식: `product_time_slot_inventory_YYYYMM`
- 예: `product_time_slot_inventory_202501` (2025년 1월)
- 대용량 데이터 효율적 관리 및 조회 성능 최적화

**사용 시나리오**:
- ROOM scope 상품: 특정 룸, 특정 날짜, 특정 시간의 재고 관리
  - 예: "2025-01-15 10:00~12:00에 룸 A의 빔 프로젝터 2대 예약 가능"
- PLACE scope 상품: 플레이스 전체, 특정 날짜, 특정 시간의 재고 관리
  - 예: "2025-01-15 14:00~16:00에 플레이스의 화이트보드 5개 예약 가능"

**동시성 제어**:
- products 테이블과 동일하게 Atomic UPDATE 방식 사용
```sql
UPDATE product_time_slot_inventory
SET reserved_quantity = reserved_quantity + ?
WHERE product_id = ? AND inventory_date = ? AND slot_time = ?
  AND (total_quantity - reserved_quantity) >= ?
```

**예시**:
```sql
-- 2025년 1월 15일 10:00 시간대의 상품 ID 1번 재고 관리
INSERT INTO product_time_slot_inventory
  (product_id, inventory_date, slot_time, total_quantity, reserved_quantity)
VALUES
  (1, '2025-01-15', '10:00:00', 5, 0);

-- 2개 예약
UPDATE product_time_slot_inventory
SET reserved_quantity = reserved_quantity + 2
WHERE product_id = 1
  AND inventory_date = '2025-01-15'
  AND slot_time = '10:00:00'
  AND (total_quantity - reserved_quantity) >= 2;
```

---

## 도메인 규칙 (Business Rules)

### PricingPolicy
```sql
-- 규칙 1: 시간대 중복 불가
CONSTRAINT uq_room_day_time UNIQUE (room_id, day_of_week, start_time, end_time)

-- 규칙 2: 시작 시간 < 종료 시간
CONSTRAINT chk_time_range CHECK (start_time < end_time)

-- 규칙 3: 가격 >= 0
CONSTRAINT CHECK (price >= 0)
```

### Product
```sql
-- 규칙 1: scope 타입에 따른 place_id/room_id 제약
CONSTRAINT chk_place_scope CHECK (
    (scope = 'PLACE' AND place_id IS NOT NULL AND room_id IS NULL) OR
    (scope = 'ROOM' AND place_id IS NOT NULL AND room_id IS NOT NULL) OR
    (scope = 'RESERVATION' AND place_id IS NULL AND room_id IS NULL)
)

-- 규칙 2: pricing_type에 따른 additional_price 제약
CONSTRAINT chk_pricing_strategy CHECK (
    (pricing_type = 'INITIAL_PLUS_ADDITIONAL' AND additional_price IS NOT NULL) OR
    (pricing_type IN ('ONE_TIME', 'SIMPLE_STOCK') AND additional_price IS NULL)
)

-- 규칙 3: scope 값 제한
CONSTRAINT chk_scope CHECK (scope IN ('PLACE', 'ROOM', 'RESERVATION'))

-- 규칙 4: pricing_type 값 제한
CONSTRAINT chk_pricing_type CHECK (pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK'))
```

### ReservationPricing
```sql
-- 규칙 1: status 값 제한
CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))

-- 규칙 2: time_slot 값 제한
CONSTRAINT chk_time_slot CHECK (time_slot IN ('HOUR', 'HALFHOUR'))
```

---

## 인덱싱 전략

### 조회 성능 최적화

**pricing_policies**:
```sql
-- 룸별 가격 정책 조회 (가장 빈번)
CREATE INDEX idx_pricing_policies_room_id ON pricing_policies(room_id);

-- 플레이스별 가격 정책 조회
CREATE INDEX idx_pricing_policies_place_id ON pricing_policies(place_id);

-- 요일 및 시간대 기반 검색
CREATE INDEX idx_pricing_policies_day_time ON pricing_policies(day_of_week, start_time, end_time);
```

**products**:
```sql
-- scope별 상품 조회
CREATE INDEX idx_products_scope ON products(scope);

-- 플레이스별 상품 조회 (Partial Index)
CREATE INDEX idx_products_place_id ON products(place_id) WHERE place_id IS NOT NULL;

-- 룸별 상품 조회 (Partial Index)
CREATE INDEX idx_products_room_id ON products(room_id) WHERE room_id IS NOT NULL;

-- scope + place_id 복합 조회
CREATE INDEX idx_products_scope_place_id ON products(scope, place_id) WHERE place_id IS NOT NULL;
```

**reservation_pricings**:
```sql
-- 예약 ID로 빠른 조회
CREATE INDEX idx_reservation_pricings_reservation_id ON reservation_pricings(reservation_id);

-- 룸별 예약 가격 조회
CREATE INDEX idx_reservation_pricings_room_id ON reservation_pricings(room_id);

-- 플레이스별 예약 가격 조회
CREATE INDEX idx_reservation_pricings_place_id ON reservation_pricings(place_id);

-- 상태별 조회
CREATE INDEX idx_reservation_pricings_status ON reservation_pricings(status);

-- 생성 시각 기준 조회
CREATE INDEX idx_reservation_pricings_calculated_at ON reservation_pricings(calculated_at);

-- 룸 + 상태 복합 조회
CREATE INDEX idx_reservation_pricings_room_status ON reservation_pricings(room_id, status);

-- 플레이스 + 상태 복합 조회
CREATE INDEX idx_reservation_pricings_place_status ON reservation_pricings(place_id, status);

-- 만료 예약 조회 (V7 추가)
CREATE INDEX idx_reservation_pricings_status_expires_at ON reservation_pricings(status, expires_at)
WHERE status = 'PENDING';
```

**room_allowed_products**:
```sql
-- 룸별 허용 상품 조회
CREATE INDEX idx_room_allowed_products_room_id ON room_allowed_products(room_id);
```

**reservation_pricing_slots**:
```sql
-- 시간 슬롯 기준 조회
CREATE INDEX idx_reservation_pricing_slots_time ON reservation_pricing_slots(slot_time);

-- 예약 + 시간 복합 조회
CREATE INDEX idx_reservation_pricing_slots_reservation_time
    ON reservation_pricing_slots(reservation_id, slot_time);
```

**reservation_pricing_products**:
```sql
-- 상품 ID 기준 조회
CREATE INDEX idx_reservation_pricing_products_product_id
    ON reservation_pricing_products(product_id);
```

---

## 샘플 데이터

### PricingPolicy 샘플
```sql
INSERT INTO pricing_policies (room_id, place_id, day_of_week, start_time, end_time, price) VALUES
(1, 100, 'MONDAY', '09:00', '12:00', 50000.00),
(1, 100, 'MONDAY', '12:00', '18:00', 80000.00),
(1, 100, 'SATURDAY', '09:00', '12:00', 70000.00),
(1, 100, 'SATURDAY', '12:00', '18:00', 100000.00);
```

### Product 샘플
```sql
INSERT INTO products (scope, place_id, room_id, name, pricing_type, initial_price, additional_price, total_quantity) VALUES
-- PLACE scope: 플레이스 전체에서 사용 가능
('PLACE', 100, NULL, '빔 프로젝터', 'SIMPLE_STOCK', 30000.00, NULL, 5),

-- ROOM scope: 특정 룸에서만 사용 가능
('ROOM', 100, 1, '화이트보드', 'ONE_TIME', 10000.00, NULL, 3),

-- RESERVATION scope: 시간 독립적
('RESERVATION', NULL, NULL, '케이터링 세트', 'INITIAL_PLUS_ADDITIONAL', 50000.00, 30000.00, 100);
```

### ReservationPricing 샘플
```sql
-- 예약 메인 정보
INSERT INTO reservation_pricings (reservation_id, room_id, place_id, status, time_slot, total_price, calculated_at) VALUES
(1, 1, 100, 'CONFIRMED', 'HOUR', 130000.00, '2025-01-10 14:30:00');

-- 시간대별 가격 (2시간 예약)
INSERT INTO reservation_pricing_slots (reservation_id, slot_time, slot_price) VALUES
(1, '2025-01-15 10:00:00', 50000.00),
(1, '2025-01-15 11:00:00', 50000.00);

-- 상품별 가격 (빔 프로젝터 1개)
INSERT INTO reservation_pricing_products (reservation_id, product_id, product_name, quantity, unit_price, total_price, pricing_type) VALUES
(1, 1, '빔 프로젝터', 1, 30000.00, 30000.00, 'SIMPLE_STOCK');
```

---

## 마이그레이션 히스토리

| Version | Description | Date |
|---------|-------------|------|
| V1 | 초기 스키마 생성 | 2025-11-08 |
| V2 | PricingPolicy Aggregate 추가 | 2025-11-08 |
| V3 | Product Aggregate 추가 | 2025-11-08 |
| V4 | ReservationPricing Aggregate 추가 | 2025-11-08 |
| V5 | ID 생성 전략 변경 (BIGSERIAL -> Snowflake ID) | 2025-11-09 |
| V6 | room_allowed_products 테이블 추가 (룸별 허용 상품 화이트리스트) | 2025-11-09 |
| V7 | reservation_pricings에 expires_at 컬럼 추가 (PENDING 예약 자동 만료) | 2025-11-09 |
| V8 | shedlock 테이블 추가 (분산 스케줄링 잠금) | 2025-11-09 |
| V9 | products에 reserved_quantity 컬럼 추가 (재고 동시성 제어) | 2025-11-12 |
| V10 | product_time_slot_inventory 테이블 추가 (시간대별 재고 관리, 월별 파티셔닝, 2025년 1-3월 파티션 생성) | 2025-11-12 |
| V11 | product_time_slot_inventory 파티션 확장 (2025년 12월까지) | 2025-11-13 |
| V12 | pg_partman 설치 및 자동 파티션 관리 설정 (3개월 선행 생성, 12개월 후 삭제) | 2025-11-13 |
| V13 | inventory_compensation_queue 테이블 추가 (재고 보상 트랜잭션 큐) | 2025-11-14 |

---

## 향후 고려사항

### 성능 최적화
- **파티셔닝**: `reservation_pricings` 테이블이 커질 경우 날짜 기반 파티셔닝 고려
- **아카이빙**: 오래된 예약 가격 데이터 아카이빙 전략
- **Materialized View**: 통계 조회를 위한 집계 뷰

### 기능 확장
- **가격 정책 변경 이력 관리**: Temporal Table 또는 별도 히스토리 테이블
- **프로모션/할인 코드 지원**: 할인 정책 테이블 추가
- **동적 가격 정책**: Dynamic Pricing 알고리즘 적용

### 데이터 무결성
- **분산 트랜잭션**: Saga 패턴 고려 (마이크로서비스 환경)

---

**Last Updated**: 2025-11-15

**주요 변경사항 (2025-11-15):**
- V11~V13 마이그레이션 이력 추가
- pg_partman 자동 파티션 관리 설명 추가
- 재고 보상 트랜잭션 큐 테이블 추가
