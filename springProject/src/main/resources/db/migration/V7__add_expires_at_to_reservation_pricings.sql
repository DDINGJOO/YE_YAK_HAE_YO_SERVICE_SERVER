-- Add expires_at column to reservation_pricings table
-- This column stores the expiration time for PENDING reservations
-- When current time exceeds this value, the reservation should be automatically cancelled

ALTER TABLE reservation_pricings
    ADD COLUMN expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '10 minutes');

-- Remove default after adding the column (default is only for existing rows)
ALTER TABLE reservation_pricings
    ALTER COLUMN expires_at DROP DEFAULT;

-- Add index for efficient querying of expired reservations
CREATE INDEX idx_reservation_pricings_status_expires_at
    ON reservation_pricings (status, expires_at) WHERE status = 'PENDING';
