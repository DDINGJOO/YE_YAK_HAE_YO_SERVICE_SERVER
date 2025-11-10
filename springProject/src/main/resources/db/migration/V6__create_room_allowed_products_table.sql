-- 룸별 허용 상품 테이블 생성 (Epic #77, Task #80)

-- 룸별 PLACE 상품 허용 목록 관리 테이블
CREATE TABLE room_allowed_products
(
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT    NOT NULL,
    product_id BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_room_product UNIQUE (room_id, product_id),
    CONSTRAINT fk_room_allowed_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
);

-- 인덱스 생성 (룸 ID 기반 조회 최적화)
CREATE INDEX idx_room_allowed_products_room_id ON room_allowed_products (room_id);

-- 코멘트 추가
COMMENT ON TABLE room_allowed_products IS '룸별 허용 상품 매핑 테이블 (화이트리스트 방식)';
COMMENT ON COLUMN room_allowed_products.id IS '매핑 ID (PK, Auto Increment)';
COMMENT ON COLUMN room_allowed_products.room_id IS '룸 ID (Place 서비스 참조)';
COMMENT ON COLUMN room_allowed_products.product_id IS '허용된 PLACE 상품 ID (FK: products.product_id)';
COMMENT ON COLUMN room_allowed_products.created_at IS '생성 일시';