-- V12: Setup pg_partman for automatic partition management
-- This migration configures automatic partition creation for product_time_slot_inventory table

-- 1. Enable pg_partman extension
CREATE EXTENSION IF NOT EXISTS pg_partman;

-- 2. Configure pg_partman for product_time_slot_inventory table
-- This will automatically create partitions 3 months in advance
SELECT partman.create_parent(
    p_parent_table := 'public.product_time_slot_inventory',
    p_control := 'time_slot',
    p_type := 'native',
    p_interval := '1 month',
    p_premake := 3,  -- Create partitions 3 months in advance
    p_start_partition := '2026-01-01 00:00:00'  -- Start from 2026-01 (we already have up to 2025-12)
);

-- 3. Configure partition retention (optional: keep partitions for 12 months)
UPDATE partman.part_config
SET retention = '12 months',
    retention_keep_table = false,  -- Drop old partitions (not just detach)
    infinite_time_partitions = true  -- Continue creating partitions indefinitely
WHERE parent_table = 'public.product_time_slot_inventory';

-- 4. Run initial partition creation
-- This will create partitions for the next 3 months immediately
SELECT partman.run_maintenance('public.product_time_slot_inventory');

-- 5. Setup automatic maintenance (run daily at 3 AM)
-- Note: This requires pg_cron extension or external cron job
-- For pg_cron approach:
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- SELECT cron.schedule('partman-maintenance', '0 3 * * *', $$SELECT partman.run_maintenance_proc()$$);

-- Alternative: Document manual approach
COMMENT ON TABLE product_time_slot_inventory IS
    'Automatic partition management via pg_partman.
     IMPORTANT: Setup cron job to run: SELECT partman.run_maintenance_proc(); daily.
     See: https://github.com/pgpartman/pg_partman#scheduling';