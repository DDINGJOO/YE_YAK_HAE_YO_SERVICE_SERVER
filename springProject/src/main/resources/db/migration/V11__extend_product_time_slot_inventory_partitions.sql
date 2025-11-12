-- Hotfix: Extend product_time_slot_inventory partitions to prevent service failure
-- CRITICAL: Without these partitions, the service will fail on 2025-04-01 with:
-- "ERROR: no partition of relation "product_time_slot_inventory" found for row"

-- Create monthly partitions for 2025-04 through 2025-12
CREATE TABLE product_time_slot_inventory_2025_04
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-04-01 00:00:00') TO ('2025-05-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_05
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-05-01 00:00:00') TO ('2025-06-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_06
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-06-01 00:00:00') TO ('2025-07-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_07
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-07-01 00:00:00') TO ('2025-08-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_08
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-08-01 00:00:00') TO ('2025-09-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_09
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-09-01 00:00:00') TO ('2025-10-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_10
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-10-01 00:00:00') TO ('2025-11-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_11
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-11-01 00:00:00') TO ('2025-12-01 00:00:00');

CREATE TABLE product_time_slot_inventory_2025_12
    PARTITION OF product_time_slot_inventory
    FOR VALUES FROM ('2025-12-01 00:00:00') TO ('2026-01-01 00:00:00');

-- TODO: Implement automated partition management
-- Option 1: Use pg_partman extension for automatic partition creation
-- Option 2: Create a scheduled job (cron) to create partitions 3 months ahead
-- Option 3: Application-level partition creation on startup