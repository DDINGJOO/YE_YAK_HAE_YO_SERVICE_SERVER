# Product 도메인 모델

## Aggregate: Product

### 구조
```
Product (Aggregate Root)
├── ProductId (Aggregate ID)
├── ProductScope (PLACE | ROOM | RESERVATION)
├── PlaceId (nullable, Scope에 따라 필수/null)
├── RoomId (nullable, Scope에 따라 필수/null)
├── String name
├── PricingStrategy
│   ├── PricingType
│   ├── Money initialPrice
│   └── Money additionalPrice (nullable, Type에 따라)
└── int totalQuantity
```

### 주요 책임
1. **상품 관리**
   - Scope별 상품 생성 (PLACE/ROOM/RESERVATION)
   - 상품명 변경
   - 가격 전략 변경
   - 재고 수량 변경

2. **가격 계산**
   - PricingStrategy를 통한 단가 계산
   - 수량별 총액 계산
   - ProductPriceBreakdown 생성 (가격 스냅샷)

3. **불변식 유지**
   - Scope별 PlaceId/RoomId 필수 여부 검증
   - PricingType별 additionalPrice 필수 여부 검증
   - 음수 가격 및 수량 방지
   - 상품명 공백 방지

## Value Objects

### PricingStrategy
**역할**: 상품의 가격 책정 방식 관리

**불변식**:
- PricingType에 따른 additionalPrice 필수 여부
  - INITIAL_PLUS_ADDITIONAL: additionalPrice 필수
  - ONE_TIME, SIMPLE_STOCK: additionalPrice는 null
- 모든 가격은 0 이상
- 불변 객체

**Factory Methods**:
- `initialPlusAdditional(Money initialPrice, Money additionalPrice)`: 초기+추가 방식
- `oneTime(Money price)`: 1회 대여 방식
- `simpleStock(Money unitPrice)`: 단순 재고 방식

**주요 메서드**:
- `calculate(int quantity)`: 수량에 따른 **단가** 계산
  - INITIAL_PLUS_ADDITIONAL: quantity=1이면 initialPrice, 그 외 additionalPrice 반환
  - ONE_TIME: 항상 initialPrice 반환 (수량 무관)
  - SIMPLE_STOCK: 항상 initialPrice 반환 (개당 단가)

**가격 계산 로직**:
```
INITIAL_PLUS_ADDITIONAL:
- 1개: initialPrice
- 2개 이상: additionalPrice (추가분의 단가)
- 총액은 Product.calculatePrice()에서 계산

ONE_TIME:
- 모든 수량: initialPrice
- 총액 = initialPrice × quantity (모든 개수에 동일 단가)

SIMPLE_STOCK:
- 모든 수량: initialPrice (개당 단가)
- 총액 = initialPrice × quantity
```

### ProductPriceBreakdown (Record)
**역할**: 계산된 상품 가격 정보 스냅샷

**구성**:
- `ProductId productId`: 상품 ID
- `String productName`: 상품명
- `int quantity`: 수량
- `Money unitPrice`: 단가 (해당 수량에서의 단가)
- `Money totalPrice`: 총액
- `PricingType pricingType`: 가격 책정 타입

**불변식**:
- `unitPrice × quantity = totalPrice` (가격 일관성 보장)
- 불변 객체 (Record)

**검증 로직**:
```java
public ProductPriceBreakdown {
  final Money expectedTotalPrice = unitPrice.multiply(quantity);
  if (!totalPrice.equals(expectedTotalPrice)) {
    throw new IllegalArgumentException("Price consistency error");
  }
}
```

### ProductScope (Enum)
**역할**: 상품 적용 범위 정의

**값**:
- `PLACE`: 플레이스 전체에서 사용 가능
- `ROOM`: 특정 룸에서만 사용 가능
- `RESERVATION`: 모든 예약에서 사용 가능

**Helper 메서드**:
- `requiresTimeSlots()`: 시간 슬롯 필요 여부
  - PLACE, ROOM: true
  - RESERVATION: false

## Enums

### PricingType
**역할**: 가격 책정 방식 정의

**값**:
- `INITIAL_PLUS_ADDITIONAL`: 초기 + 추가 방식
  - 첫 개는 초기 가격, 추가 개는 추가 가격
  - 예: 빔 프로젝터 (첫 대여 10,000원, 추가 5,000원)

- `ONE_TIME`: 1회 대여 방식
  - 수량 무관하게 1회 가격만 적용
  - 예: 회의실 청소 서비스 (15,000원)

- `SIMPLE_STOCK`: 단순 재고 방식
  - 개당 일정한 단가
  - 예: 음료수 (개당 2,000원)

## 비즈니스 규칙

### 1. Scope별 ID 검증
**PLACE**:
- PlaceId 필수
- RoomId는 null

**ROOM**:
- PlaceId 필수
- RoomId 필수

**RESERVATION**:
- PlaceId는 null
- RoomId는 null

**검증 코드**:
```java
private void validateScopeIds(PlaceId placeId, RoomId roomId, ProductScope scope) {
  switch (scope) {
    case PLACE -> {
      if (placeId == null) throw new IllegalArgumentException("PlaceId required for PLACE scope");
      if (roomId != null) throw new IllegalArgumentException("RoomId must be null for PLACE scope");
    }
    case ROOM -> {
      if (placeId == null) throw new IllegalArgumentException("PlaceId required for ROOM scope");
      if (roomId == null) throw new IllegalArgumentException("RoomId required for ROOM scope");
    }
    case RESERVATION -> {
      if (placeId != null) throw new IllegalArgumentException("PlaceId must be null for RESERVATION scope");
      if (roomId != null) throw new IllegalArgumentException("RoomId must be null for RESERVATION scope");
    }
  }
}
```

### 2. PricingType별 AdditionalPrice 검증
**INITIAL_PLUS_ADDITIONAL**:
- initialPrice 필수
- additionalPrice 필수

**ONE_TIME, SIMPLE_STOCK**:
- initialPrice 필수
- additionalPrice는 null

### 3. 가격 계산 로직
**INITIAL_PLUS_ADDITIONAL**:
```
수량 1: unitPrice = initialPrice, totalPrice = initialPrice
수량 2: unitPrice = additionalPrice, totalPrice = initialPrice + additionalPrice
수량 3: unitPrice = additionalPrice, totalPrice = initialPrice + 2 * additionalPrice
...
수량 n: unitPrice = additionalPrice, totalPrice = initialPrice + (n-1) * additionalPrice
```

**ONE_TIME**:
```
모든 수량: unitPrice = initialPrice, totalPrice = initialPrice * quantity
```

**SIMPLE_STOCK**:
```
모든 수량: unitPrice = initialPrice, totalPrice = initialPrice * quantity
```

### 4. 재고 관리
- `totalQuantity`는 0 이상이어야 함
- 재고 차감 로직은 별도 UseCase에서 처리 (추후 구현)
- Product는 재고 수량 변경 기능만 제공

## Factory Methods

### Product 생성
```java
// PLACE 범위 상품
Product placeProduct = Product.createPlaceScoped(
    ProductId.of(null),  // Auto-generated
    PlaceId.of(100L),
    "공용 빔 프로젝터",
    PricingStrategy.initialPlusAdditional(Money.of(10000), Money.of(5000)),
    5  // totalQuantity
);

// ROOM 범위 상품
Product roomProduct = Product.createRoomScoped(
    ProductId.of(null),
    PlaceId.of(100L),
    RoomId.of(200L),
    "룸 전용 화이트보드",
    PricingStrategy.simpleStock(Money.of(3000)),
    10
);

// RESERVATION 범위 상품
Product reservationProduct = Product.createReservationScoped(
    ProductId.of(null),
    "음료수",
    PricingStrategy.simpleStock(Money.of(2000)),
    100
);
```

## 예시

### 1. INITIAL_PLUS_ADDITIONAL 타입
```java
Product beamProjector = Product.createPlaceScoped(
    ProductId.of(null),
    PlaceId.of(100L),
    "빔 프로젝터",
    PricingStrategy.initialPlusAdditional(Money.of(10000), Money.of(5000)),
    5
);

// 1개 주문
ProductPriceBreakdown breakdown1 = beamProjector.calculatePrice(1);
// unitPrice: 10000원 (초기 가격)
// totalPrice: 10000원

// 2개 주문
ProductPriceBreakdown breakdown2 = beamProjector.calculatePrice(2);
// unitPrice: 5000원 (추가 가격)
// totalPrice: 15000원 (10000 + 5000)

// 3개 주문
ProductPriceBreakdown breakdown3 = beamProjector.calculatePrice(3);
// unitPrice: 5000원 (추가 가격)
// totalPrice: 20000원 (10000 + 5000 + 5000)
```

### 2. ONE_TIME 타입
```java
Product cleaningService = Product.createRoomScoped(
    ProductId.of(null),
    PlaceId.of(100L),
    RoomId.of(200L),
    "회의실 청소",
    PricingStrategy.oneTime(Money.of(15000)),
    999  // 무제한 (수량 의미 없음)
);

// 1개 주문
ProductPriceBreakdown breakdown1 = cleaningService.calculatePrice(1);
// unitPrice: 15000원
// totalPrice: 15000원

// 5개 주문 (실제로는 의미 없음)
ProductPriceBreakdown breakdown5 = cleaningService.calculatePrice(5);
// unitPrice: 15000원
// totalPrice: 75000원 (15000 × 5)
```

### 3. SIMPLE_STOCK 타입
```java
Product beverage = Product.createReservationScoped(
    ProductId.of(null),
    "음료수",
    PricingStrategy.simpleStock(Money.of(2000)),
    100
);

// 5개 주문
ProductPriceBreakdown breakdown = beverage.calculatePrice(5);
// unitPrice: 2000원 (개당 단가)
// totalPrice: 10000원 (2000 × 5)
```

### 4. 상품 정보 변경
```java
Product product = Product.createReservationScoped(
    ProductId.of(null),
    "원래 이름",
    PricingStrategy.simpleStock(Money.of(1000)),
    10
);

// 이름 변경
product.updateName("새로운 이름");

// 수량 변경
product.updateTotalQuantity(20);

// 가격 전략 변경
product.updatePricingStrategy(
    PricingStrategy.oneTime(Money.of(5000))
);
```

## Use Cases

### 구현 완료 (Issue #14)

#### 1. RegisterProductUseCase 
**구현**: `RegisterProductService`
- 상품 생성 (PLACE/ROOM/RESERVATION Scope별)
- Scope별 ID 검증
- PricingStrategy 검증
- Repository 저장
- ProductResponse 반환

**REST API**: `POST /api/products`

#### 2. GetProductUseCase 
**구현**: `GetProductService`
- 상품 조회 (ID, PlaceId, RoomId, Scope별)
- 전체 상품 목록 조회
- ProductResponse 변환

**REST API**:
- `GET /api/products/{productId}`
- `GET /api/products?scope=PLACE`
- `GET /api/products?placeId=100`
- `GET /api/products?roomId=200`

#### 3. UpdateProductUseCase 
**구현**: `UpdateProductService`
- 상품 정보 수정
- 이름, 가격 전략, 수량 변경
- Repository 저장

**REST API**: `PUT /api/products/{productId}`

#### 4. DeleteProductUseCase 
**구현**: `DeleteProductService`
- 상품 삭제
- 존재 여부 검증
- Repository 삭제

**REST API**: `DELETE /api/products/{productId}`

### 향후 구현 예정

#### 5. ManageProductStockUseCase
- 재고 차감 (예약 시)
- 재고 복원 (예약 취소 시)
- 재고 부족 검증
- 동시성 제어

#### 6. CalculateProductPriceUseCase
- 상품별 가격 계산
- 예약에 포함된 모든 상품 총액 계산
- ReservationPricing과 통합

## 도메인 이벤트 (예정)

### ProductCreated
- 상품이 생성되었을 때
- 재고 관리 시스템에 알림

### ProductStockChanged
- 재고 수량이 변경되었을 때
- 재고 부족 알림 트리거

### ProductPriceChanged
- 가격 전략이 변경되었을 때
- 가격 이력 기록

## 테스트 전략

### Unit Tests
**ProductTest** (67 tests):
- Scope별 생성 테스트 (PLACE, ROOM, RESERVATION)
- ID 검증 테스트 (null, Scope별 필수 여부)
- 이름 검증 테스트 (null, 빈 문자열, 공백)
- 가격 전략 검증 테스트
- 수량 검증 테스트 (음수, 0, 양수)
- 가격 계산 테스트 (3가지 PricingType별)
- 업데이트 메서드 테스트

**PricingStrategyTest**:
- Factory Method 테스트
- PricingType별 검증 테스트
- calculate() 로직 테스트

**ProductPriceBreakdownTest**:
- 가격 일관성 검증 테스트
- Record 불변성 테스트

### Integration Tests
**ProductRepositoryAdapterTest** (13 tests):
- CRUD 동작 테스트 (저장, 조회, 수정, 삭제)
- Scope별 조회 테스트 (findByPlaceId, findByRoomId, findByScope)
- PricingStrategy 저장/조회 테스트 (3가지 타입별)
- JPA 매핑 검증 테스트

**Test Coverage**: Domain Layer 100%, Repository Layer 100%

## 설계 원칙

### SOLID 원칙
**SRP (Single Responsibility Principle)**:
- Product: 상품 정보 관리
- PricingStrategy: 가격 계산 로직
- ProductPriceBreakdown: 가격 스냅샷

**OCP (Open-Closed Principle)**:
- PricingType 추가 시 PricingStrategy만 수정
- ProductScope 추가 시 Product 검증 로직만 수정

**LSP (Liskov Substitution Principle)**:
- PricingStrategy는 모든 타입에서 일관된 인터페이스 제공

**ISP (Interface Segregation Principle)**:
- ProductRepository는 필요한 메서드만 정의

**DIP (Dependency Inversion Principle)**:
- Product는 Repository 인터페이스(Port)에만 의존
- 구현체(Adapter)는 인프라 계층에 위치

### DDD Patterns
**Aggregate**:
- Product가 Aggregate Root
- ProductId로 식별
- 경계 내 일관성 보장

**Value Object**:
- PricingStrategy: 불변 객체, 교체 가능
- ProductPriceBreakdown: 불변 스냅샷

**Factory Method**:
- Product.createXXXScoped(): Scope별 생성
- PricingStrategy.XXX(): Type별 생성

**Repository**:
- Aggregate 단위로 저장/조회
- Domain 계층에는 Port만 존재
