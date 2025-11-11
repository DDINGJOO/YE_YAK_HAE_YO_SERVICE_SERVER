-- ============================================================================
-- Reservation Pricing Service - Initial Schema
-- Version: V1
-- Description: 예약 가격 관리 서비스 초기 스키마 생성
-- Date: 2025-11-08
-- ============================================================================

-- ============================================================================
-- 1. PricingPolicy Aggregate (가격 정책)
-- ============================================================================

-- 1.1 pricing_policies 테이블 (Aggregate Root)
CREATE TABLE pricing_policies
(
    id BIGSERIAL PRIMARY KEY,
    room_id     BIGINT         NOT NULL,
    place_id    BIGINT         NOT NULL,
    day_of_week VARCHAR(10)    NOT NULL CHECK (day_of_week IN
                                               ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY',
                                                'SUNDAY')),
    start_time  TIME           NOT NULL,
    end_time    TIME           NOT NULL,
    price       DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 도메인 규칙: 시작 시간은 종료 시간보다 이전이어야 함
    CONSTRAINT chk_time_range CHECK (start_time < end_time),

    -- 도메인 규칙: 같은 룸, 같은 요일 내에서 시간대 중복 불가
    CONSTRAINT uq_room_day_time UNIQUE (room_id, day_of_week, start_time, end_time)
);

-- 인덱스: 룸별 가격 정책 조회 최적화
CREATE INDEX idx_pricing_policies_room_id ON pricing_policies (room_id);

-- 인덱스: 플레이스별 가격 정책 조회 최적화
CREATE INDEX idx_pricing_policies_place_id ON pricing_policies (place_id);

-- 인덱스: 요일 및 시간대 기반 검색 최적화
CREATE INDEX idx_pricing_policies_day_time ON pricing_policies (day_of_week, start_time, end_time);

-- 코멘트
COMMENT
ON TABLE pricing_policies IS '시간대별 예약 가격 정책 (PricingPolicy Aggregate Root)';
COMMENT
ON COLUMN pricing_policies.room_id IS '룸 ID (외부 서비스 참조)';
COMMENT
ON COLUMN pricing_policies.place_id IS '플레이스 ID (외부 서비스 참조)';
COMMENT
ON COLUMN pricing_policies.day_of_week IS '요일 (MONDAY~SUNDAY)';
COMMENT
ON COLUMN pricing_policies.start_time IS '시작 시간';
COMMENT
ON COLUMN pricing_policies.end_time IS '종료 시간';
COMMENT
ON COLUMN pricing_policies.price IS '가격 (원)';

-- ============================================================================
-- 2. Product Aggregate (추가상품)
-- ============================================================================

-- 2.1 products 테이블 (Aggregate Root)
CREATE TABLE products
(
    id BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL,
    description    TEXT,
    price          DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    scope_type     VARCHAR(20)    NOT NULL CHECK (scope_type IN ('PLACE', 'ROOM', 'RESERVATION')),
    scope_id       BIGINT,
    total_quantity INTEGER        NOT NULL CHECK (total_quantity >= 0),
    used_quantity  INTEGER        NOT NULL DEFAULT 0 CHECK (used_quantity >= 0),
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 도메인 규칙: 사용 수량은 총 수량을 초과할 수 없음
    CONSTRAINT chk_quantity CHECK (used_quantity <= total_quantity)
);

-- 인덱스: 적용 범위별 상품 조회 최적화
CREATE INDEX idx_products_scope ON products (scope_type, scope_id);

-- 인덱스: 활성 상품 필터링 최적화
CREATE INDEX idx_products_active ON products (is_active) WHERE is_active = TRUE;

-- 코멘트
COMMENT
ON TABLE products IS '추가상품 (Product Aggregate Root)';
COMMENT
ON COLUMN products.name IS '상품명';
COMMENT
ON COLUMN products.description IS '상품 설명';
COMMENT
ON COLUMN products.price IS '상품 가격 (원)';
COMMENT
ON COLUMN products.scope_type IS '적용 범위 (PLACE/ROOM/RESERVATION)';
COMMENT
ON COLUMN products.scope_id IS '적용 범위 ID (플레이스/룸 ID 등)';
COMMENT
ON COLUMN products.total_quantity IS '총 재고 수량';
COMMENT
ON COLUMN products.used_quantity IS '사용된 수량';
COMMENT
ON COLUMN products.is_active IS '활성 상태';

-- ============================================================================
-- 3. ReservationPricing Aggregate (예약 가격 스냅샷)
-- ============================================================================

-- 3.1 reservation_pricings 테이블 (Aggregate Root)
CREATE TABLE reservation_pricings
(
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT         NOT NULL UNIQUE,
    room_id        BIGINT         NOT NULL,
    place_id       BIGINT         NOT NULL,
    base_price     DECIMAL(10, 2) NOT NULL CHECK (base_price >= 0),
    total_price    DECIMAL(10, 2) NOT NULL CHECK (total_price >= 0),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 도메인 규칙: 예약 가격은 생성 후 변경 불가 (Immutable)
    -- updated_at 컬럼 없음

    CONSTRAINT uq_reservation_id UNIQUE (reservation_id)
);

-- 인덱스: 예약 ID로 빠른 조회
CREATE INDEX idx_reservation_pricings_reservation_id ON reservation_pricings (reservation_id);

-- 인덱스: 룸별 가격 조회 최적화
CREATE INDEX idx_reservation_pricings_room_id ON reservation_pricings (room_id);

-- 코멘트
COMMENT
ON TABLE reservation_pricings IS '예약 가격 스냅샷 (ReservationPricing Aggregate Root)';
COMMENT
ON COLUMN reservation_pricings.reservation_id IS '예약 ID (외부 서비스 참조, Unique)';
COMMENT
ON COLUMN reservation_pricings.room_id IS '룸 ID';
COMMENT
ON COLUMN reservation_pricings.place_id IS '플레이스 ID';
COMMENT
ON COLUMN reservation_pricings.base_price IS '기본 가격 (룸 가격)';
COMMENT
ON COLUMN reservation_pricings.total_price IS '총 가격 (기본 가격 + 추가상품)';

-- 3.2 reservation_pricing_items 테이블 (Entity)
CREATE TABLE reservation_pricing_items
(
    id BIGSERIAL PRIMARY KEY,
    reservation_pricing_id BIGINT         NOT NULL REFERENCES reservation_pricings (id) ON DELETE CASCADE,
    product_id             BIGINT         NOT NULL,
    product_name           VARCHAR(100)   NOT NULL,
    product_price          DECIMAL(10, 2) NOT NULL CHECK (product_price >= 0),
    quantity               INTEGER        NOT NULL CHECK (quantity > 0),
    item_total_price       DECIMAL(10, 2) NOT NULL CHECK (item_total_price >= 0),
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP

    -- Immutable: updated_at 없음
);

-- 인덱스: 예약 가격별 항목 조회 최적화
CREATE INDEX idx_pricing_items_reservation_pricing_id ON reservation_pricing_items (reservation_pricing_id);

-- 코멘트
COMMENT
ON TABLE reservation_pricing_items IS '예약 가격 항목 (추가상품 스냅샷, Entity)';
COMMENT
ON COLUMN reservation_pricing_items.reservation_pricing_id IS '예약 가격 ID (FK)';
COMMENT
ON COLUMN reservation_pricing_items.product_id IS '상품 ID (스냅샷 시점)';
COMMENT
ON COLUMN reservation_pricing_items.product_name IS '상품명 (스냅샷)';
COMMENT
ON COLUMN reservation_pricing_items.product_price IS '상품 단가 (스냅샷)';
COMMENT
ON COLUMN reservation_pricing_items.quantity IS '수량';
COMMENT
ON COLUMN reservation_pricing_items.item_total_price IS '항목 총 가격 (단가 x 수량)';

-- ============================================================================
-- 4. 샘플 데이터 (개발/테스트용)
-- ============================================================================

-- PricingPolicy 샘플 데이터
INSERT INTO pricing_policies (room_id, place_id, day_of_week, start_time, end_time, price)
VALUES (1, 1, 'MONDAY', '09:00', '12:00', 50000.00),
       (1, 1, 'MONDAY', '12:00', '18:00', 80000.00),
       (1, 1, 'SATURDAY', '09:00', '12:00', 70000.00),
       (1, 1, 'SATURDAY', '12:00', '18:00', 100000.00);

-- Product 샘플 데이터
INSERT INTO products (name, description, price, scope_type, scope_id, total_quantity, used_quantity, is_active)
VALUES ('빔 프로젝터', '회의용 빔 프로젝터 대여', 30000.00, 'PLACE', 1, 5, 0, TRUE),
       ('화이트보드', '대형 화이트보드 (마커 포함)', 10000.00, 'ROOM', 1, 3, 0, TRUE),
       ('케이터링 세트', '회의용 간식 세트 (10인분)', 50000.00, 'RESERVATION', NULL, 100, 0, TRUE);

-- ============================================================================
-- 5. 트리거 함수 (updated_at 자동 갱신)
-- ============================================================================

-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- pricing_policies 테이블 트리거
CREATE TRIGGER trigger_pricing_policies_updated_at
    BEFORE UPDATE
    ON pricing_policies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- products 테이블 트리거
CREATE TRIGGER trigger_products_updated_at
    BEFORE UPDATE
    ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- End of V1__init_schema.sql
-- ============================================================================
