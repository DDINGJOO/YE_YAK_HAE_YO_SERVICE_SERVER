# Database Schema Documentation

예약 가격 관리 서비스의 데이터베이스 스키마 문서입니다.

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
│ PK  id                   │ BIGSERIAL
│     name                 │ VARCHAR(100)
│     description          │ TEXT
│     price                │ DECIMAL(10,2)
│     scope_type           │ VARCHAR(20) [PLACE/ROOM/RESERVATION]
│     scope_id             │ BIGINT
│     total_quantity       │ INTEGER
│     used_quantity        │ INTEGER
│     is_active            │ BOOLEAN
│     created_at           │ TIMESTAMP
│     updated_at           │ TIMESTAMP
└──────────────────────────┘

Constraints:
- CHECK: price >= 0
- CHECK: total_quantity >= 0
- CHECK: used_quantity >= 0
- CHECK: used_quantity <= total_quantity
- CHECK: scope_type IN ('PLACE', 'ROOM', 'RESERVATION')

Indexes:
- idx_products_scope (scope_type, scope_id)
- idx_products_active (is_active) WHERE is_active = TRUE

---

┌─────────────────────────────────────────────────────────────────────────┐
│                    ReservationPricing Aggregate                         │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐         ┌──────────────────────────────┐
│ reservation_pricings     │ 1     * │ reservation_pricing_items    │
├──────────────────────────┤◄────────├──────────────────────────────┤
│ PK  id                   │         │ PK  id                       │
│ UQ  reservation_id       │         │ FK  reservation_pricing_id   │
│     room_id              │         │     product_id               │
│     place_id             │         │     product_name             │
│     base_price           │         │     product_price            │
│     total_price          │         │     quantity                 │
│     created_at           │         │     item_total_price         │
└──────────────────────────┘         │     created_at               │
                                     └──────────────────────────────┘

reservation_pricings:
- Immutable (No updated_at)
- CHECK: base_price >= 0
- CHECK: total_price >= 0
- UNIQUE: reservation_id

reservation_pricing_items:
- Immutable (No updated_at)
- CHECK: product_price >= 0
- CHECK: quantity > 0
- CHECK: item_total_price >= 0
- ON DELETE CASCADE

Indexes:
- idx_reservation_pricings_reservation_id (reservation_id)
- idx_reservation_pricings_room_id (room_id)
- idx_pricing_items_reservation_pricing_id (reservation_pricing_id)
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
- 사용 수량은 총 수량을 초과할 수 없습니다
- 가격은 0원 이상이어야 합니다

**주요 컬럼**:
- `scope_type`: 적용 범위 타입 (PLACE, ROOM, RESERVATION)
  - PLACE: 플레이스 전체에 적용
  - ROOM: 특정 룸에만 적용
  - RESERVATION: 예약별로 적용
- `scope_id`: 적용 범위 ID (scope_type이 PLACE/ROOM일 때)
- `total_quantity`: 총 재고 수량
- `used_quantity`: 현재 사용 중인 수량
- `is_active`: 활성 상태 (삭제 대신 비활성화)

**재고 관리**:
```sql
-- 재고 차감
UPDATE products
SET used_quantity = used_quantity + :quantity
WHERE id = :product_id
  AND (total_quantity - used_quantity) >= :quantity;

-- 재고 복구
UPDATE products
SET used_quantity = used_quantity - :quantity
WHERE id = :product_id;
```

---

### 3. reservation_pricings (예약 가격 스냅샷)

**목적**: 예약 시점의 가격 정보를 불변 스냅샷으로 저장합니다.

**도메인 규칙**:
- 예약 가격은 생성 후 변경 불가 (Immutable)
- 총 가격 = 기본 가격 + 모든 추가상품 가격의 합
- 모든 금액은 0원 이상

**주요 컬럼**:
- `reservation_id`: 외부 서비스(예약 서비스)의 예약 ID (Unique)
- `base_price`: 룸 기본 가격 (예약 시점의 가격 정책 기준)
- `total_price`: 총 가격 (base_price + 추가상품 합계)

**특징**:
- `updated_at` 컬럼 없음 (불변 객체)
- 예약 취소 시에도 데이터 유지 (이력 관리)

---

### 4. reservation_pricing_items (예약 가격 항목)

**목적**: 예약에 포함된 추가상품 정보를 스냅샷으로 저장합니다.

**주요 컬럼**:
- `product_id`: 상품 ID (참조용, FK 아님)
- `product_name`: 상품명 스냅샷 (상품 이름 변경되어도 유지)
- `product_price`: 상품 단가 스냅샷
- `quantity`: 수량
- `item_total_price`: 항목 총 가격 (product_price × quantity)

**특징**:
- 스냅샷이므로 products 테이블과 FK 관계 없음
- `ON DELETE CASCADE`: 예약 가격 삭제 시 함께 삭제

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
-- 규칙 1: 사용 수량 <= 총 수량
CONSTRAINT chk_quantity CHECK (used_quantity <= total_quantity)

-- 규칙 2: 수량 >= 0
CONSTRAINT CHECK (total_quantity >= 0)
CONSTRAINT CHECK (used_quantity >= 0)

-- 규칙 3: 가격 >= 0
CONSTRAINT CHECK (price >= 0)
```

### ReservationPricing
```sql
-- 규칙 1: 예약 ID 유일성
CONSTRAINT uq_reservation_id UNIQUE (reservation_id)

-- 규칙 2: 금액 >= 0
CONSTRAINT CHECK (base_price >= 0)
CONSTRAINT CHECK (total_price >= 0)
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
-- 적용 범위별 상품 조회
CREATE INDEX idx_products_scope ON products(scope_type, scope_id);

-- 활성 상품 필터링 (Partial Index)
CREATE INDEX idx_products_active ON products(is_active) WHERE is_active = TRUE;
```

**reservation_pricings**:
```sql
-- 예약 ID로 빠른 조회
CREATE INDEX idx_reservation_pricings_reservation_id ON reservation_pricings(reservation_id);

-- 룸별 가격 조회
CREATE INDEX idx_reservation_pricings_room_id ON reservation_pricings(room_id);
```

**reservation_pricing_items**:
```sql
-- 예약 가격별 항목 조회
CREATE INDEX idx_pricing_items_reservation_pricing_id ON reservation_pricing_items(reservation_pricing_id);
```

---

## 트리거

### updated_at 자동 갱신

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- pricing_policies 테이블
CREATE TRIGGER trigger_pricing_policies_updated_at
BEFORE UPDATE ON pricing_policies
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- products 테이블
CREATE TRIGGER trigger_products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

**주의**: `reservation_pricings` 및 `reservation_pricing_items`는 불변 객체이므로 트리거 없음

---

## 샘플 데이터

### PricingPolicy 샘플
```sql
INSERT INTO pricing_policies (room_id, place_id, day_of_week, start_time, end_time, price) VALUES
(1, 1, 'MONDAY', '09:00', '12:00', 50000.00),
(1, 1, 'MONDAY', '12:00', '18:00', 80000.00),
(1, 1, 'SATURDAY', '09:00', '12:00', 70000.00),
(1, 1, 'SATURDAY', '12:00', '18:00', 100000.00);
```

### Product 샘플
```sql
INSERT INTO products (name, description, price, scope_type, scope_id, total_quantity, used_quantity, is_active) VALUES
('빔 프로젝터', '회의용 빔 프로젝터 대여', 30000.00, 'PLACE', 1, 5, 0, TRUE),
('화이트보드', '대형 화이트보드 (마커 포함)', 10000.00, 'ROOM', 1, 3, 0, TRUE),
('케이터링 세트', '회의용 간식 세트 (10인분)', 50000.00, 'RESERVATION', NULL, 100, 0, TRUE);
```

---

## 마이그레이션 히스토리

| Version | Description | Date |
|---------|-------------|------|
| V1 | 초기 스키마 생성 (PricingPolicy, Product, ReservationPricing Aggregates) | 2025-11-08 |

---

## 향후 고려사항

### 성능 최적화
- 파티셔닝: `reservation_pricings` 테이블이 커질 경우 날짜 기반 파티셔닝 고려
- 아카이빙: 오래된 예약 가격 데이터 아카이빙 전략

### 기능 확장
- 가격 정책 변경 이력 관리 (Temporal Table)
- 프로모션/할인 코드 지원
- 동적 가격 정책 (Dynamic Pricing)

---

**Last Updated**: 2025-11-08
