-- Add total_quantity column to product_time_slot_inventory
-- This column tracks the total available quantity for a product at a specific time slot

ALTER TABLE product_time_slot_inventory
    ADD COLUMN total_quantity INTEGER NOT NULL DEFAULT 0;

-- Add comment to explain the column
COMMENT ON COLUMN product_time_slot_inventory.total_quantity IS
    '해당 시간대의 총 재고 수량 (전체 가용량)';