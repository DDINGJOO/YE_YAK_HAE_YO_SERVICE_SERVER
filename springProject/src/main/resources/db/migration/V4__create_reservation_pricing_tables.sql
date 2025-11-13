-- ReservationPricing Aggregate 테이블 재생성
-- V1에서 생성한 reservation_pricings 관련 테이블을 새로운 구조로 변경

-- 기존 테이블 삭제 (CASCADE로 관련 제약조건도 함께 삭제)
DROP TABLE IF EXISTS reservation_pricing_items CASCADE;
DROP TABLE IF EXISTS reservation_pricings CASCADE;

-- 예약 가격 스냅샷 메인 테이블 (새로운 구조)
CREATE TABLE reservation_pricings
(
    reservation_id BIGSERIAL PRIMARY KEY,
    room_id       BIGINT         NOT NULL,
    place_id      BIGINT         NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    time_slot     VARCHAR(10)    NOT NULL,
    total_price   DECIMAL(12, 2) NOT NULL,
    calculated_at TIMESTAMP      NOT NULL,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_time_slot CHECK (time_slot IN ('HOUR', 'HALFHOUR'))
);

-- 시간대별 가격 내역 테이블 (@ElementCollection for Map<LocalDateTime, BigDecimal>)
CREATE TABLE reservation_pricing_slots
(
    reservation_id BIGINT         NOT NULL,
    slot_time      TIMESTAMP      NOT NULL,
    slot_price     DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (reservation_id, slot_time),
    CONSTRAINT fk_reservation_pricing_slots FOREIGN KEY (reservation_id)
        REFERENCES reservation_pricings (reservation_id) ON DELETE CASCADE
);

-- 상품별 가격 내역 테이블 (@ElementCollection for List<ProductPriceBreakdownEmbeddable>)
CREATE TABLE reservation_pricing_products
(
    reservation_id BIGINT         NOT NULL,
    product_id     BIGINT         NOT NULL,
    product_name   VARCHAR(255)   NOT NULL,
    quantity       INTEGER        NOT NULL,
    unit_price     DECIMAL(12, 2) NOT NULL,
    total_price    DECIMAL(12, 2) NOT NULL,
    pricing_type   VARCHAR(30)    NOT NULL,
    CONSTRAINT fk_reservation_pricing_products FOREIGN KEY (reservation_id)
        REFERENCES reservation_pricings (reservation_id) ON DELETE CASCADE,
    CONSTRAINT chk_pricing_type CHECK (pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK'))
);

-- 인덱스 생성
CREATE INDEX idx_reservation_pricings_room_id ON reservation_pricings (room_id);
CREATE INDEX idx_reservation_pricings_place_id ON reservation_pricings (place_id);
CREATE INDEX idx_reservation_pricings_status ON reservation_pricings (status);
CREATE INDEX idx_reservation_pricings_calculated_at ON reservation_pricings (calculated_at);
CREATE INDEX idx_reservation_pricings_room_status ON reservation_pricings (room_id, status);
CREATE INDEX idx_reservation_pricings_place_status ON reservation_pricings (place_id, status);

CREATE INDEX idx_reservation_pricing_slots_time ON reservation_pricing_slots (slot_time);
CREATE INDEX idx_reservation_pricing_slots_reservation_time ON reservation_pricing_slots (reservation_id, slot_time);

CREATE INDEX idx_reservation_pricing_products_product_id ON reservation_pricing_products (product_id);

-- 코멘트 추가
COMMENT
ON TABLE reservation_pricings IS '예약 가격 스냅샷 테이블 - 예약 시점의 가격 정보를 불변 스냅샷으로 저장';
COMMENT
ON COLUMN reservation_pricings.reservation_id IS '예약 ID (PK, Auto Increment)';
COMMENT
ON COLUMN reservation_pricings.room_id IS '룸 ID';
COMMENT
ON COLUMN reservation_pricings.place_id IS '플레이스 ID (쿼리 효율성을 위한 비정규화)';
COMMENT
ON COLUMN reservation_pricings.status IS '예약 상태 (PENDING: 대기, CONFIRMED: 확정, CANCELLED: 취소)';
COMMENT
ON COLUMN reservation_pricings.time_slot IS '시간 단위 (HOUR: 1시간, HALFHOUR: 30분)';
COMMENT
ON COLUMN reservation_pricings.total_price IS '총 가격 (시간대 가격 + 상품 가격 합계)';
COMMENT
ON COLUMN reservation_pricings.calculated_at IS '가격 계산 시각';

COMMENT
ON TABLE reservation_pricing_slots IS '예약의 시간대별 가격 내역';
COMMENT
ON COLUMN reservation_pricing_slots.reservation_id IS '예약 ID (FK)';
COMMENT
ON COLUMN reservation_pricing_slots.slot_time IS '시간 슬롯 (예: 2025-01-15 10:00:00)';
COMMENT
ON COLUMN reservation_pricing_slots.slot_price IS '해당 슬롯의 가격';

COMMENT
ON TABLE reservation_pricing_products IS '예약에 포함된 상품별 가격 내역';
COMMENT
ON COLUMN reservation_pricing_products.reservation_id IS '예약 ID (FK)';
COMMENT
ON COLUMN reservation_pricing_products.product_id IS '상품 ID (스냅샷)';
COMMENT
ON COLUMN reservation_pricing_products.product_name IS '상품명 (스냅샷)';
COMMENT
ON COLUMN reservation_pricing_products.quantity IS '수량';
COMMENT
ON COLUMN reservation_pricing_products.unit_price IS '단가 (스냅샷)';
COMMENT
ON COLUMN reservation_pricing_products.total_price IS '총 가격 (단가 × 수량)';
COMMENT
ON COLUMN reservation_pricing_products.pricing_type IS '가격 책정 방식 (스냅샷)';
