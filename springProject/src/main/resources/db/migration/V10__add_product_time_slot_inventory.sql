-- Task #157: ROOM/PLACE Scope 시간대별 재고 관리 테이블 추가
-- 동시성 문제(Phantom Read) 해결을 위한 원자적 UPDATE 기반 재고 관리

-- 1. 시간대별 재고 관리 테이블 생성 (파티셔닝 적용)
CREATE TABLE product_time_slot_inventory (
    product_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    time_slot TIMESTAMP NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, room_id, time_slot)
) PARTITION BY RANGE (time_slot);

-- 2. Constraint 추가
ALTER TABLE product_time_slot_inventory
    ADD CONSTRAINT chk_time_slot_reserved_non_negative
    CHECK (reserved_quantity >= 0);

-- 3. 인덱스 추가
-- PLACE Scope 성능 향상용 (rooms 테이블과 JOIN 시 사용)
CREATE INDEX idx_product_time_slot_inventory_product_time
    ON product_time_slot_inventory(product_id, time_slot);

-- 과거 데이터 정리용
CREATE INDEX idx_product_time_slot_inventory_time_slot
    ON product_time_slot_inventory(time_slot);

-- 4. 월별 파티션 생성 (2025년 1월 ~ 3월)
CREATE TABLE product_time_slot_inventory_2025_01
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-01-01 00:00:00') TO ('2025-02-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_02
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-02-01 00:00:00') TO ('2025-03-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_03
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-03-01 00:00:00') TO ('2025-04-01 00:00:00');

-- 5. 코멘트 추가
COMMENT ON TABLE product_time_slot_inventory IS
    'ROOM/PLACE Scope 상품의 시간대별 예약 수량 관리 테이블 (동시성 제어용)';

COMMENT ON COLUMN product_time_slot_inventory.product_id IS
    '상품 ID';

COMMENT ON COLUMN product_time_slot_inventory.room_id IS
    '예약한 룸 ID (PLACE Scope인 경우 rooms 테이블과 JOIN하여 place_id 획득)';

COMMENT ON COLUMN product_time_slot_inventory.time_slot IS
    '시간대 (1시간 단위, 예: 2025-01-15 10:00:00)';

COMMENT ON COLUMN product_time_slot_inventory.reserved_quantity IS
    '해당 시간대의 예약된 수량';

-- 6. Foreign Key 추가 (참조 무결성 보장)
ALTER TABLE product_time_slot_inventory
    ADD CONSTRAINT fk_time_slot_inventory_product
    FOREIGN KEY (product_id)
    REFERENCES products(product_id)
    ON DELETE CASCADE;

-- 주의: rooms 테이블이 없으면 이 FK는 생략
-- ALTER TABLE product_time_slot_inventory
--     ADD CONSTRAINT fk_time_slot_inventory_room
--     FOREIGN KEY (room_id)
--     REFERENCES rooms(room_id)
--     ON DELETE CASCADE;