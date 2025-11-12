-- Additional tables not managed by JPA
CREATE TABLE IF NOT EXISTS product_time_slot_inventory (
    product_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    time_slot TIMESTAMP NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, room_id, time_slot),
    CONSTRAINT chk_time_slot_reserved_non_negative CHECK (reserved_quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_product_time_slot_inventory_product_time
    ON product_time_slot_inventory(product_id, time_slot);

CREATE INDEX IF NOT EXISTS idx_product_time_slot_inventory_time_slot
    ON product_time_slot_inventory(time_slot);
