-- Product Aggregate 테이블 재생성
-- V1에서 생성한 products 테이블을 새로운 구조로 변경

-- 기존 테이블 삭제 (CASCADE로 관련 제약조건도 함께 삭제)
DROP TABLE IF EXISTS products CASCADE;

-- 추가상품 메인 테이블 (새로운 구조)
CREATE TABLE products
(
    product_id BIGSERIAL PRIMARY KEY,
    scope            VARCHAR(20)    NOT NULL,
    place_id         BIGINT,
    room_id          BIGINT,
    name             VARCHAR(255)   NOT NULL,
    pricing_type     VARCHAR(50)    NOT NULL,
    initial_price    DECIMAL(19, 2) NOT NULL,
    additional_price DECIMAL(19, 2),
    total_quantity   INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT chk_scope CHECK (scope IN ('PLACE', 'ROOM', 'RESERVATION')),
    CONSTRAINT chk_pricing_type CHECK (pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK')),
    CONSTRAINT chk_place_scope CHECK (
        (scope = 'PLACE' AND place_id IS NOT NULL AND room_id IS NULL) OR
        (scope = 'ROOM' AND place_id IS NOT NULL AND room_id IS NOT NULL) OR
        (scope = 'RESERVATION' AND place_id IS NULL AND room_id IS NULL)
        ),
    CONSTRAINT chk_pricing_strategy CHECK (
        (pricing_type = 'INITIAL_PLUS_ADDITIONAL' AND additional_price IS NOT NULL) OR
        (pricing_type IN ('ONE_TIME', 'SIMPLE_STOCK') AND additional_price IS NULL)
        )
);

-- 인덱스 생성
CREATE INDEX idx_products_scope ON products (scope);
CREATE INDEX idx_products_place_id ON products (place_id) WHERE place_id IS NOT NULL;
CREATE INDEX idx_products_room_id ON products (room_id) WHERE room_id IS NOT NULL;
CREATE INDEX idx_products_scope_place_id ON products (scope, place_id) WHERE place_id IS NOT NULL;

-- 코멘트 추가
COMMENT
ON TABLE products IS '추가상품 테이블';
COMMENT
ON COLUMN products.product_id IS '상품 ID (PK, Auto Increment)';
COMMENT
ON COLUMN products.scope IS '상품 적용 범위 (PLACE: 플레이스 전체, ROOM: 특정 룸, RESERVATION: 예약 단위)';
COMMENT
ON COLUMN products.place_id IS '플레이스 ID (PLACE, ROOM 범위 시 필수)';
COMMENT
ON COLUMN products.room_id IS '룸 ID (ROOM 범위 시 필수)';
COMMENT
ON COLUMN products.name IS '상품명';
COMMENT
ON COLUMN products.pricing_type IS '가격 책정 방식 (INITIAL_PLUS_ADDITIONAL: 초기+추가, ONE_TIME: 1회 대여, SIMPLE_STOCK: 단순 재고)';
COMMENT
ON COLUMN products.initial_price IS '초기 가격 또는 단가';
COMMENT
ON COLUMN products.additional_price IS '추가 가격 (INITIAL_PLUS_ADDITIONAL 타입만 사용)';
COMMENT
ON COLUMN products.total_quantity IS '총 재고 수량';
