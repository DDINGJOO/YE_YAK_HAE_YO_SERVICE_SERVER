# Product 데이터베이스 스키마

## ERD

```
┌─────────────────────────────────┐
│         products                │
├─────────────────────────────────┤
│ product_id (PK)    BIGSERIAL    │
│ scope              VARCHAR(20)  │
│ place_id           BIGINT       │ (nullable)
│ room_id            BIGINT       │ (nullable)
│ name               VARCHAR(255) │
│ pricing_type       VARCHAR(50)  │
│ initial_price      DECIMAL(19,2)│
│ additional_price   DECIMAL(19,2)│ (nullable)
│ total_quantity     INTEGER      │
└─────────────────────────────────┘
```

## 테이블 상세

### products

**목적**: 추가상품 정보 메인 테이블

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| product_id | BIGSERIAL | PK, AUTO_INCREMENT | 상품 ID (Aggregate ID) |
| scope | VARCHAR(20) | NOT NULL | 상품 적용 범위 (PLACE, ROOM, RESERVATION) |
| place_id | BIGINT | nullable | 플레이스 ID (PLACE, ROOM 범위 시 필수) |
| room_id | BIGINT | nullable | 룸 ID (ROOM 범위 시 필수) |
| name | VARCHAR(255) | NOT NULL | 상품명 |
| pricing_type | VARCHAR(50) | NOT NULL | 가격 책정 방식 |
| initial_price | DECIMAL(19,2) | NOT NULL | 초기 가격 또는 단가 |
| additional_price | DECIMAL(19,2) | nullable | 추가 가격 (INITIAL_PLUS_ADDITIONAL만) |
| total_quantity | INTEGER | NOT NULL, DEFAULT 0 | 총 재고 수량 |

**제약조건**:

1. **Scope 값 제약**:
```sql
CONSTRAINT chk_scope CHECK (scope IN ('PLACE', 'ROOM', 'RESERVATION'))
```

2. **PricingType 값 제약**:
```sql
CONSTRAINT chk_pricing_type CHECK (
  pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK')
)
```

3. **Scope별 ID 필수 여부 제약**:
```sql
CONSTRAINT chk_place_scope CHECK (
  (scope = 'PLACE' AND place_id IS NOT NULL AND room_id IS NULL) OR
  (scope = 'ROOM' AND place_id IS NOT NULL AND room_id IS NOT NULL) OR
  (scope = 'RESERVATION' AND place_id IS NULL AND room_id IS NULL)
)
```

4. **PricingType별 AdditionalPrice 필수 여부 제약**:
```sql
CONSTRAINT chk_pricing_strategy CHECK (
  (pricing_type = 'INITIAL_PLUS_ADDITIONAL' AND additional_price IS NOT NULL) OR
  (pricing_type IN ('ONE_TIME', 'SIMPLE_STOCK') AND additional_price IS NULL)
)
```

**인덱스**:
- PRIMARY KEY: `product_id`
- INDEX: `idx_products_scope` on `scope`
- INDEX: `idx_products_place_id` on `place_id` WHERE `place_id IS NOT NULL` (Partial Index)
- INDEX: `idx_products_room_id` on `room_id` WHERE `room_id IS NOT NULL` (Partial Index)
- INDEX: `idx_products_scope_place_id` on `(scope, place_id)` WHERE `place_id IS NOT NULL` (Composite Index)

**설명**:
- product_id는 Auto-increment로 자동 생성
- place_id와 room_id는 scope에 따라 nullable
- DB 제약조건으로 도메인 불변식을 강제
- Partial Index로 NULL 값 제외하여 인덱스 효율 향상
- Composite Index로 "특정 플레이스의 PLACE 범위 상품" 같은 쿼리 최적화

## JPA 매핑

### ProductEntity

```java
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private ProductScope scope;

    @Column(name = "place_id")
    private Long placeId;  // nullable

    @Column(name = "room_id")
    private Long roomId;   // nullable

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Embedded
    private PricingStrategyEmbeddable pricingStrategy;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    // constructors, getters, fromDomain, toDomain methods
}
```

**특징**:
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Auto-increment 사용
- `placeId`, `roomId`는 Long 타입으로 nullable 허용
- `PricingStrategyEmbeddable`로 가격 전략 매핑

### PricingStrategyEmbeddable

```java
@Embeddable
public class PricingStrategyEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 50)
    private PricingType pricingType;

    @Column(name = "initial_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialPrice;

    @Column(name = "additional_price", precision = 19, scale = 2)
    private BigDecimal additionalPrice;  // nullable

    public static PricingStrategyEmbeddable fromDomain(PricingStrategy strategy) {
        return new PricingStrategyEmbeddable(
            strategy.getPricingType(),
            strategy.getInitialPrice().getAmount(),
            strategy.getAdditionalPrice() != null
                ? strategy.getAdditionalPrice().getAmount()
                : null
        );
    }

    public PricingStrategy toDomain() {
        return switch (pricingType) {
            case INITIAL_PLUS_ADDITIONAL -> PricingStrategy.initialPlusAdditional(
                Money.of(initialPrice),
                Money.of(additionalPrice)
            );
            case ONE_TIME -> PricingStrategy.oneTime(
                Money.of(initialPrice)
            );
            case SIMPLE_STOCK -> PricingStrategy.simpleStock(
                Money.of(initialPrice)
            );
        };
    }
}
```

**특징**:
- `@Embeddable`로 PricingStrategy Value Object 매핑
- `fromDomain()`, `toDomain()` 메서드로 변환 처리
- Switch expression으로 PricingType별 Factory Method 호출

### ProductIdEmbeddable

```java
@Embeddable
public class ProductIdEmbeddable implements Serializable {

    @Column(name = "product_id")
    private Long value;

    public ProductId toDomain() {
        return ProductId.of(value);
    }

    public static ProductIdEmbeddable fromDomain(ProductId productId) {
        return new ProductIdEmbeddable(productId.getValue());
    }
}
```

**특징**:
- ProductId Value Object를 JPA 엔티티에 매핑
- `Serializable` 구현 (Embeddable ID 요구사항)

## Repository 구현

### ProductJpaRepository

```java
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * PlaceId로 해당 플레이스의 모든 상품을 조회합니다.
     * PLACE 범위 상품과 ROOM 범위 상품을 모두 포함합니다.
     */
    List<ProductEntity> findByPlaceId(Long placeId);

    /**
     * RoomId로 해당 룸의 상품을 조회합니다.
     * ROOM 범위 상품만 반환합니다.
     */
    List<ProductEntity> findByRoomId(Long roomId);

    /**
     * ProductScope로 상품을 조회합니다.
     */
    List<ProductEntity> findByScope(ProductScope scope);
}
```

**쿼리 예시**:

```sql
-- findByPlaceId(100L)
SELECT * FROM products
WHERE place_id = 100;
-- PLACE 범위 상품 + ROOM 범위 상품 모두 조회

-- findByRoomId(200L)
SELECT * FROM products
WHERE room_id = 200;
-- ROOM 범위 상품만 조회

-- findByScope(ProductScope.RESERVATION)
SELECT * FROM products
WHERE scope = 'RESERVATION';
```

### ProductRepositoryAdapter

```java
@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Optional<Product> findById(final ProductId productId) {
        return jpaRepository.findById(productId.getValue())
            .map(ProductEntity::toDomain);
    }

    @Override
    public List<Product> findByPlaceId(final PlaceId placeId) {
        return jpaRepository.findByPlaceId(placeId.getValue())
            .stream()
            .map(ProductEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Product save(final Product product) {
        final ProductEntity entity = ProductEntity.fromDomain(product);
        final ProductEntity savedEntity = jpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    // 기타 메서드들...
}
```

**특징**:
- Domain ↔ Entity 변환 처리
- ProductRepository Port 구현
- Hexagonal Architecture의 Adapter 패턴

## Flyway 마이그레이션

### V3__create_products_table.sql

```sql
-- Product Aggregate 테이블 생성

-- 추가상품 메인 테이블
CREATE TABLE products
(
    product_id       BIGSERIAL PRIMARY KEY,
    scope            VARCHAR(20)    NOT NULL,
    place_id         BIGINT,
    room_id          BIGINT,
    name             VARCHAR(255)   NOT NULL,
    pricing_type     VARCHAR(50)    NOT NULL,
    initial_price    DECIMAL(19, 2) NOT NULL,
    additional_price DECIMAL(19, 2),
    total_quantity   INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT chk_scope CHECK (scope IN ('PLACE', 'ROOM', 'RESERVATION')),
    CONSTRAINT chk_pricing_type CHECK (pricing_type IN ('INITIAL_PLUS_ADDITIONAL', 'ONE_TIME', 'SIMPLE_STOCK')),
    CONSTRAINT chk_place_scope CHECK (
        (scope = 'PLACE' AND place_id IS NOT NULL AND room_id IS NULL) OR
        (scope = 'ROOM' AND place_id IS NOT NULL AND room_id IS NOT NULL) OR
        (scope = 'RESERVATION' AND place_id IS NULL AND room_id IS NULL)
    ),
    CONSTRAINT chk_pricing_strategy CHECK (
        (pricing_type = 'INITIAL_PLUS_ADDITIONAL' AND additional_price IS NOT NULL) OR
        (pricing_type IN ('ONE_TIME', 'SIMPLE_STOCK') AND additional_price IS NULL)
    )
);

-- 인덱스 생성
CREATE INDEX idx_products_scope ON products (scope);
CREATE INDEX idx_products_place_id ON products (place_id) WHERE place_id IS NOT NULL;
CREATE INDEX idx_products_room_id ON products (room_id) WHERE room_id IS NOT NULL;
CREATE INDEX idx_products_scope_place_id ON products (scope, place_id) WHERE place_id IS NOT NULL;

-- 코멘트 추가
COMMENT ON TABLE products IS '추가상품 테이블';
COMMENT ON COLUMN products.product_id IS '상품 ID (PK, Auto Increment)';
COMMENT ON COLUMN products.scope IS '상품 적용 범위 (PLACE: 플레이스 전체, ROOM: 특정 룸, RESERVATION: 예약 단위)';
COMMENT ON COLUMN products.place_id IS '플레이스 ID (PLACE, ROOM 범위 시 필수)';
COMMENT ON COLUMN products.room_id IS '룸 ID (ROOM 범위 시 필수)';
COMMENT ON COLUMN products.name IS '상품명';
COMMENT ON COLUMN products.pricing_type IS '가격 책정 방식 (INITIAL_PLUS_ADDITIONAL: 초기+추가, ONE_TIME: 1회 대여, SIMPLE_STOCK: 단순 재고)';
COMMENT ON COLUMN products.initial_price IS '초기 가격 또는 단가';
COMMENT ON COLUMN products.additional_price IS '추가 가격 (INITIAL_PLUS_ADDITIONAL 타입만 사용)';
COMMENT ON COLUMN products.total_quantity IS '총 재고 수량';
```

**마이그레이션 특징**:
- CHECK 제약조건으로 도메인 불변식 강제
- Partial Index로 성능 최적화
- 상세한 코멘트로 스키마 문서화

## 쿼리 예시

### 1. PlaceId로 모든 상품 조회
```sql
-- PLACE 범위 상품 + ROOM 범위 상품
SELECT * FROM products
WHERE place_id = 100
ORDER BY scope, product_id;
```

**결과**:
```
product_id | scope | place_id | room_id | name
-----------|-------|----------|---------|------
1          | PLACE | 100      | null    | 공용 빔 프로젝터
2          | ROOM  | 100      | 201     | 룸 A 화이트보드
3          | ROOM  | 100      | 202     | 룸 B 화이트보드
```

### 2. RoomId로 상품 조회
```sql
-- ROOM 범위 상품만
SELECT * FROM products
WHERE room_id = 201;
```

**결과**:
```
product_id | scope | place_id | room_id | name
-----------|-------|----------|---------|------
2          | ROOM  | 100      | 201     | 룸 A 화이트보드
```

### 3. Scope로 상품 조회
```sql
-- RESERVATION 범위 상품만
SELECT * FROM products
WHERE scope = 'RESERVATION';
```

**결과**:
```
product_id | scope       | place_id | room_id | name
-----------|-------------|----------|---------|------
10         | RESERVATION | null     | null    | 음료수
11         | RESERVATION | null     | null    | 간식
```

### 4. 복합 쿼리 (PlaceId + Scope)
```sql
-- 특정 플레이스의 PLACE 범위 상품만
SELECT * FROM products
WHERE place_id = 100 AND scope = 'PLACE';
```

**인덱스 사용**:
- `idx_products_scope_place_id` 복합 인덱스 사용

## 성능 최적화

### 1. Partial Index
```sql
CREATE INDEX idx_products_place_id ON products (place_id)
WHERE place_id IS NOT NULL;
```
**효과**:
- NULL 값 제외하여 인덱스 크기 감소
- RESERVATION 범위 상품 제외 (place_id가 null)
- 인덱스 스캔 속도 향상

### 2. Composite Index
```sql
CREATE INDEX idx_products_scope_place_id ON products (scope, place_id)
WHERE place_id IS NOT NULL;
```
**효과**:
- "특정 플레이스의 PLACE 범위 상품" 쿼리 최적화
- Scope와 PlaceId를 함께 조회하는 경우 성능 향상

### 3. JPA Fetch Strategy
```java
// ProductRepositoryAdapter에서
@Override
public List<Product> findByPlaceId(final PlaceId placeId) {
    return jpaRepository.findByPlaceId(placeId.getValue())
        .stream()
        .map(ProductEntity::toDomain)
        .collect(Collectors.toList());
}
```
**효과**:
- 단일 쿼리로 모든 필드 조회 (N+1 문제 없음)
- Embeddable은 자동으로 EAGER 로딩

## 데이터 무결성

### 1. 도메인 제약조건
- Scope별 ID 필수 여부: DB CHECK 제약조건으로 강제
- PricingType별 AdditionalPrice: DB CHECK 제약조건으로 강제

### 2. 애플리케이션 레벨 검증
- Product Aggregate에서 중복 검증
- JPA Entity에서 추가 검증 (fromDomain 시)

### 3. 트랜잭션 보장
- Repository 레벨에서 @Transactional 처리
- Aggregate 단위로 저장/조회

## 테스트

### Repository Integration Test
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ProductRepositoryAdapter.class)
class ProductRepositoryAdapterTest {
    // 13개 테스트 케이스
    // CRUD, Scope별 조회, PricingStrategy 저장/조회
}
```

**테스트 커버리지**:
- CRUD 동작: 5개 테스트
- Scope별 조회: 3개 테스트
- PricingStrategy 저장/조회: 4개 테스트
- 총 13개 테스트, 모두 통과

## 향후 확장

### 1. 재고 이력 테이블 (예정)
```sql
CREATE TABLE product_stock_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    change_quantity INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
```

### 2. 가격 이력 테이블 (예정)
```sql
CREATE TABLE product_price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    pricing_type VARCHAR(50) NOT NULL,
    initial_price DECIMAL(19, 2) NOT NULL,
    additional_price DECIMAL(19, 2),
    changed_at TIMESTAMP NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
```

### 3. 상품-예약 연결 테이블 (예정)
```sql
CREATE TABLE reservation_products (
    reservation_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    total_price DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (reservation_id, product_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
```
