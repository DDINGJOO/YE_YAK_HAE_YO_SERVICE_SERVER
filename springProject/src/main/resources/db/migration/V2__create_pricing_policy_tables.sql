-- PricingPolicy Aggregate 테이블 재생성
-- V1에서 생성한 pricing_policies 테이블을 새로운 구조로 변경

-- 기존 테이블 삭제 (CASCADE로 관련 제약조건도 함께 삭제)
DROP TABLE IF EXISTS pricing_policies CASCADE;

-- 가격 정책 메인 테이블 (새로운 구조)
CREATE TABLE pricing_policies
(
    room_id       BIGINT         NOT NULL,
    place_id      BIGINT         NOT NULL,
    time_slot     VARCHAR(20)    NOT NULL,
    default_price DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (room_id)
);

-- 시간대별 가격 테이블 (ElementCollection)
CREATE TABLE time_range_prices
(
    room_id        BIGINT         NOT NULL,
    day_of_week    VARCHAR(20)    NOT NULL,
    start_time     TIME           NOT NULL,
    end_time       TIME           NOT NULL,
    price_per_slot DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (room_id) REFERENCES pricing_policies (room_id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_pricing_policies_place_id ON pricing_policies (place_id);
CREATE INDEX idx_time_range_prices_room_id ON time_range_prices (room_id);
CREATE INDEX idx_time_range_prices_day_of_week ON time_range_prices (day_of_week);

-- 코멘트 추가
COMMENT
ON TABLE pricing_policies IS '룸별 가격 정책 테이블';
COMMENT
ON COLUMN pricing_policies.room_id IS '룸 ID (PK, Aggregate ID)';
COMMENT
ON COLUMN pricing_policies.place_id IS '장소 ID';
COMMENT
ON COLUMN pricing_policies.time_slot IS '시간 단위 (HOUR, HALFHOUR)';
COMMENT
ON COLUMN pricing_policies.default_price IS '기본 가격 (시간대별 가격이 없을 때 적용)';

COMMENT
ON TABLE time_range_prices IS '시간대별 차등 가격 테이블';
COMMENT
ON COLUMN time_range_prices.room_id IS '룸 ID (FK)';
COMMENT
ON COLUMN time_range_prices.day_of_week IS '요일 (MONDAY ~ SUNDAY)';
COMMENT
ON COLUMN time_range_prices.start_time IS '시작 시간';
COMMENT
ON COLUMN time_range_prices.end_time IS '종료 시간';
COMMENT
ON COLUMN time_range_prices.price_per_slot IS '슬롯당 가격';
