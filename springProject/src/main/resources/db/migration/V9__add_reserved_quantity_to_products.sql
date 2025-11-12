-- Task #142: 원자적 재고 예약을 위한 reserved_quantity 컬럼 추가

ALTER TABLE products
    ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0;

-- 제약조건: reserved_quantity는 음수일 수 없음
ALTER TABLE products
    ADD CONSTRAINT chk_reserved_quantity_non_negative CHECK (reserved_quantity >= 0);

-- 제약조건: reserved_quantity는 total_quantity를 초과할 수 없음
ALTER TABLE products
    ADD CONSTRAINT chk_reserved_quantity_not_exceed_total CHECK (reserved_quantity <= total_quantity);

-- 코멘트 추가
COMMENT ON COLUMN products.reserved_quantity IS '예약된 수량 (동시성 제어용)';