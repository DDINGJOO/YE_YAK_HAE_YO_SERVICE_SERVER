-- Additional tables not managed by JPA
-- ROOM/PLACE Scope time-slot inventory table with partitioning
CREATE TABLE IF NOT EXISTS product_time_slot_inventory (
    product_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    time_slot TIMESTAMP NOT NULL,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, room_id, time_slot),
    CONSTRAINT chk_time_slot_reserved_non_negative CHECK (reserved_quantity >= 0)
) PARTITION BY RANGE (time_slot);

-- ShedLock table for distributed task locking
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Create monthly partitions for 2025
CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_01
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-01-01 00:00:00') TO ('2025-02-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_02
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-02-01 00:00:00') TO ('2025-03-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_03
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-03-01 00:00:00') TO ('2025-04-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_04
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-04-01 00:00:00') TO ('2025-05-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_05
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-05-01 00:00:00') TO ('2025-06-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_06
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-06-01 00:00:00') TO ('2025-07-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_07
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-07-01 00:00:00') TO ('2025-08-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_08
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-08-01 00:00:00') TO ('2025-09-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_09
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-09-01 00:00:00') TO ('2025-10-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_10
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-10-01 00:00:00') TO ('2025-11-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_11
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-11-01 00:00:00') TO ('2025-12-01 00:00:00');

CREATE TABLE IF NOT EXISTS product_time_slot_inventory_2025_12
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-12-01 00:00:00') TO ('2026-01-01 00:00:00');

-- Indexes
CREATE INDEX IF NOT EXISTS idx_product_time_slot_inventory_product_time
    ON product_time_slot_inventory(product_id, time_slot);

CREATE INDEX IF NOT EXISTS idx_product_time_slot_inventory_time_slot
    ON product_time_slot_inventory(time_slot);
