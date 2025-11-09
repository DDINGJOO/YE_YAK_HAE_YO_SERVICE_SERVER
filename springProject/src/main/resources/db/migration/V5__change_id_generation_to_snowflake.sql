-- ID 생성 전략 변경: BIGSERIAL -> BIGINT (Snowflake ID Generator 사용)

-- products 테이블: product_id의 SERIAL 제거
ALTER TABLE products ALTER COLUMN product_id DROP DEFAULT;
DROP SEQUENCE IF EXISTS products_product_id_seq;

-- reservation_pricings 테이블: reservation_id의 SERIAL 제거
ALTER TABLE reservation_pricings ALTER COLUMN reservation_id DROP DEFAULT;
DROP SEQUENCE IF EXISTS reservation_pricings_reservation_id_seq;

-- 코멘트 업데이트
COMMENT ON COLUMN products.product_id IS '상품 ID (PK, Snowflake ID)';
COMMENT ON COLUMN reservation_pricings.reservation_id IS '예약 ID (PK, Snowflake ID)';
