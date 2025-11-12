# 추가상품 관리 API

## 개요

플레이스, 룸, 예약 범위별 추가상품을 관리하는 REST API입니다. 상품 등록, 조회, 수정, 삭제 및 다양한 필터링 조회를 제공합니다.

## Base URL

```
/api/products
```

## 엔드포인트

### 1. 상품 등록

새로운 상품을 등록합니다.

**Request**
```http
POST /api/products
Content-Type: application/json

{
  "scope": "PLACE",
  "placeId": 100,
  "roomId": null,
  "name": "공용 빔 프로젝터",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 10000,
    "additionalPrice": null
  },
  "totalQuantity": 5
}
```

**Request Body**
- `scope` (String, required): 상품 범위 (`PLACE`, `ROOM`, `RESERVATION`)
- `placeId` (Long, conditional): 플레이스 ID (PLACE, ROOM Scope에서 필수)
- `roomId` (Long, conditional): 룸 ID (ROOM Scope에서 필수)
- `name` (String, required): 상품명
- `pricingStrategy` (Object, required): 가격 전략
  - `pricingType` (String, required): 가격 책정 방식 (`INITIAL_PLUS_ADDITIONAL`, `ONE_TIME`, `SIMPLE_STOCK`)
  - `initialPrice` (BigDecimal, required): 초기 가격 (0 이상)
  - `additionalPrice` (BigDecimal, conditional): 추가 가격 (INITIAL_PLUS_ADDITIONAL에서 필수)
- `totalQuantity` (Integer, required): 총 재고 수량 (0 이상)

**Response (201 Created)**
```json
{
  "productId": 1,
  "scope": "PLACE",
  "placeId": 100,
  "roomId": null,
  "name": "공용 빔 프로젝터",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 10000,
    "additionalPrice": null
  },
  "totalQuantity": 5
}
```

**Error Responses**
- `400 Bad Request`: Validation 실패
```json
{
  "timestamp": "2025-11-09T12:34:56",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "입력값 검증에 실패했습니다.",
  "path": "/api/products",
  "fieldErrors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Product name is required"
    }
  ]
}
```

**Scope별 등록 예시**

**PLACE Scope (플레이스 전체)**
```json
{
  "scope": "PLACE",
  "placeId": 100,
  "roomId": null,
  "name": "공용 빔 프로젝터",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 10000,
    "additionalPrice": null
  },
  "totalQuantity": 5
}
```

**ROOM Scope (특정 룸)**
```json
{
  "scope": "ROOM",
  "placeId": 100,
  "roomId": 200,
  "name": "룸 전용 화이트보드",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 15000,
    "additionalPrice": null
  },
  "totalQuantity": 3
}
```

**RESERVATION Scope (예약 단위)**
```json
{
  "scope": "RESERVATION",
  "placeId": null,
  "roomId": null,
  "name": "음료수",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 2000,
    "additionalPrice": null
  },
  "totalQuantity": 100
}
```

**PricingType별 예시**

**INITIAL_PLUS_ADDITIONAL (초기 + 추가)**
```json
{
  "pricingType": "INITIAL_PLUS_ADDITIONAL",
  "initialPrice": 10000,
  "additionalPrice": 5000
}
```
- 첫 개: 10,000원
- 추가 개당: 5,000원
- 2개 주문: 10,000 + 5,000 = 15,000원

**ONE_TIME (1회 대여)**
```json
{
  "pricingType": "ONE_TIME",
  "initialPrice": 15000,
  "additionalPrice": null
}
```
- 수량 무관: 15,000원

**SIMPLE_STOCK (단순 재고)**
```json
{
  "pricingType": "SIMPLE_STOCK",
  "initialPrice": 2000,
  "additionalPrice": null
}
```
- 개당: 2,000원

---

### 2. 상품 조회 (ID)

상품 ID로 상품 정보를 조회합니다.

**Request**
```http
GET /api/products/{productId}
```

**Path Parameters**
- `productId` (Long, required): 상품 ID

**Response (200 OK)**
```json
{
  "productId": 1,
  "scope": "PLACE",
  "placeId": 100,
  "roomId": null,
  "name": "공용 빔 프로젝터",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 10000,
    "additionalPrice": null
  },
  "totalQuantity": 5
}
```

**Error Responses**
- `400 Bad Request`: 음수 ID 입력
```json
{
  "timestamp": "2025-11-09T12:34:56",
  "status": 400,
  "code": "CONSTRAINT_VIOLATION",
  "message": "getProduct.productId: Product ID must be positive",
  "path": "/api/products/-1"
}
```

- `404 Not Found`: 상품을 찾을 수 없음
```json
{
  "timestamp": "2025-11-09T12:34:56",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Product not found with id: 999",
  "path": "/api/products/999"
}
```

---

### 3. 상품 목록 조회 (필터링)

상품 목록을 조회합니다. 필터링 옵션을 통해 특정 조건의 상품만 조회할 수 있습니다.

**Request**
```http
GET /api/products?scope=PLACE&placeId=100&roomId=200
```

**Query Parameters** (모두 선택)
- `scope` (String, optional): 범위 필터링 (`PLACE`, `ROOM`, `RESERVATION`)
- `placeId` (Long, optional): 플레이스 ID 필터링
- `roomId` (Long, optional): 룸 ID 필터링

**필터링 우선순위**
```
roomId > placeId > scope > all
```
- `roomId`가 있으면 특정 룸의 상품만 조회 (ROOM Scope만)
- `placeId`만 있으면 해당 플레이스의 상품 조회 (PLACE + ROOM Scope)
- `scope`만 있으면 해당 범위의 상품 조회
- 모두 없으면 전체 상품 조회

**Response (200 OK)**
```json
[
  {
    "productId": 1,
    "scope": "PLACE",
    "placeId": 100,
    "roomId": null,
    "name": "공용 빔 프로젝터",
    "pricingStrategy": {
      "pricingType": "SIMPLE_STOCK",
      "initialPrice": 10000,
      "additionalPrice": null
    },
    "totalQuantity": 5
  },
  {
    "productId": 2,
    "scope": "RESERVATION",
    "placeId": null,
    "roomId": null,
    "name": "음료수",
    "pricingStrategy": {
      "pricingType": "SIMPLE_STOCK",
      "initialPrice": 2000,
      "additionalPrice": null
    },
    "totalQuantity": 100
  }
]
```

**필터링 예시**

**1. 전체 상품 조회**
```http
GET /api/products
```

**2. PLACE Scope만 조회**
```http
GET /api/products?scope=PLACE
```

**3. 특정 플레이스의 상품 조회 (PLACE + ROOM)**
```http
GET /api/products?placeId=100
```

**4. 특정 룸의 상품만 조회 (ROOM)**
```http
GET /api/products?roomId=200
```

**5. RESERVATION Scope만 조회**
```http
GET /api/products?scope=RESERVATION
```

---

### 4. 상품 수정

상품 정보를 수정합니다. Scope와 ID는 변경할 수 없으며, 이름, 가격 전략, 수량만 수정 가능합니다.

**Request**
```http
PUT /api/products/{productId}
Content-Type: application/json

{
  "name": "수정된 상품명",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 8000,
    "additionalPrice": null
  },
  "totalQuantity": 15
}
```

**Path Parameters**
- `productId` (Long, required): 상품 ID

**Request Body**
- `name` (String, required): 새로운 상품명
- `pricingStrategy` (Object, required): 새로운 가격 전략
- `totalQuantity` (Integer, required): 새로운 총 수량 (0 이상)

**Response (200 OK)**
```json
{
  "productId": 1,
  "scope": "PLACE",
  "placeId": 100,
  "roomId": null,
  "name": "수정된 상품명",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 8000,
    "additionalPrice": null
  },
  "totalQuantity": 15
}
```

**Error Responses**
- `400 Bad Request`: Validation 실패
- `404 Not Found`: 상품을 찾을 수 없음

---

### 5. 상품 삭제

상품을 삭제합니다.

**Request**
```http
DELETE /api/products/{productId}
```

**Path Parameters**
- `productId` (Long, required): 상품 ID

**Response (204 No Content)**
```
(No body)
```

**Error Responses**
- `400 Bad Request`: 음수 ID 입력
- `404 Not Found`: 상품을 찾을 수 없음

---

## Validation 규칙

### Scope별 필수 필드
- **PLACE**: `placeId` 필수, `roomId`는 null
- **ROOM**: `placeId`와 `roomId` 모두 필수
- **RESERVATION**: `placeId`와 `roomId` 모두 null

### PricingType별 필수 필드
- **INITIAL_PLUS_ADDITIONAL**: `initialPrice`와 `additionalPrice` 모두 필수
- **ONE_TIME**: `initialPrice`만 필수, `additionalPrice`는 null
- **SIMPLE_STOCK**: `initialPrice`만 필수, `additionalPrice`는 null

### 공통 Validation
- `name`: 공백 불가, null 불가
- `initialPrice`: 0 이상
- `additionalPrice`: 0 이상 (필수인 경우)
- `totalQuantity`: 0 이상

---

## 사용 시나리오

### 시나리오 1: 플레이스에 공용 빔 프로젝터 추가

```bash
# 1. 상품 등록
POST /api/products
{
  "scope": "PLACE",
  "placeId": 100,
  "roomId": null,
  "name": "공용 빔 프로젝터",
  "pricingStrategy": {
    "pricingType": "INITIAL_PLUS_ADDITIONAL",
    "initialPrice": 10000,
    "additionalPrice": 5000
  },
  "totalQuantity": 5
}

# 2. 등록된 상품 조회
GET /api/products/1

# 3. 플레이스의 모든 상품 조회
GET /api/products?placeId=100
```

### 시나리오 2: 룸 전용 장비 관리

```bash
# 1. 룸 전용 화이트보드 추가
POST /api/products
{
  "scope": "ROOM",
  "placeId": 100,
  "roomId": 200,
  "name": "룸 전용 화이트보드",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 15000,
    "additionalPrice": null
  },
  "totalQuantity": 3
}

# 2. 특정 룸의 상품만 조회
GET /api/products?roomId=200
```

### 시나리오 3: 예약 시 추가 가능한 음료 관리

```bash
# 1. 음료 상품 등록
POST /api/products
{
  "scope": "RESERVATION",
  "placeId": null,
  "roomId": null,
  "name": "음료수",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 2000,
    "additionalPrice": null
  },
  "totalQuantity": 100
}

# 2. 재고 부족으로 수량 증가
PUT /api/products/3
{
  "name": "음료수",
  "pricingStrategy": {
    "pricingType": "SIMPLE_STOCK",
    "initialPrice": 2000,
    "additionalPrice": null
  },
  "totalQuantity": 200
}

# 3. 모든 RESERVATION 상품 조회
GET /api/products?scope=RESERVATION
```

### 시나리오 4: 가격 전략 변경

```bash
# 1. SIMPLE_STOCK에서 ONE_TIME으로 변경
PUT /api/products/1
{
  "name": "회의실 청소 서비스",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 20000,
    "additionalPrice": null
  },
  "totalQuantity": 999
}
```

---

## 아키텍처

### Hexagonal Architecture

```
Adapter In (Web)          Application Layer         Domain Layer
┌──────────────────┐     ┌──────────────────┐     ┌──────────────┐
│ ProductController│────▶│ Use Case Ports   │     │   Product    │
│                  │     │ ┌──────────────┐ │     │ (Aggregate)  │
│ - POST /products │     │ │ Register     │ │     │              │
│ - GET  /products │     │ │ Get          │◀┼────▶│ - Scope      │
│ - PUT  /products │     │ │ Update       │ │     │ - Strategy   │
│ - DELETE         │     │ │ Delete       │ │     │ - Quantity   │
└──────────────────┘     │ └──────────────┘ │     └──────────────┘
                         │                  │
                         │ Services         │
                         │ ┌──────────────┐ │
                         │ │ Register     │ │
                         │ │ Get          │ │
                         │ │ Update       │ │
                         │ │ Delete       │ │
                         │ └──────────────┘ │
                         └──────────────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Repository Port  │
                         │ (Output Port)    │
                         └──────────────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
Adapter Out (Persistence)│ Repository       │
                         │ Adapter          │
                         │ (JPA)            │
                         └──────────────────┘
```

### Layer별 책임

**Controller (Adapter In)**
- HTTP 요청/응답 처리
- DTO 검증 (@Valid)
- Use Case 호출
- HTTP Status 반환

**Application Service**
- Use Case 구현
- Transaction 관리 (@Transactional)
- DTO ↔ Domain 변환
- Business Logic 조율

**Domain**
- 비즈니스 로직
- 불변식 유지
- Scope/PricingType 검증
- 가격 계산

**Repository**
- 영속성 처리
- Domain ↔ JPA Entity 변환
- 조회 최적화

---

## 테스트

### API 통합 테스트 (16 tests)

**POST /api/products (5 tests)**
- PLACE Scope 등록 성공
- ROOM Scope 등록 성공
- RESERVATION Scope 등록 성공
- 필수 필드 누락 시 400 반환
- 음수 수량 시 400 반환

**GET /api/products/{id} (3 tests)**
- 조회 성공
- 존재하지 않는 상품 404 반환
- 음수 ID 조회 시 400 반환

**GET /api/products (4 tests)**
- 전체 목록 조회
- Scope 필터링
- PlaceId 필터링
- RoomId 필터링

**PUT /api/products/{id} (2 tests)**
- 수정 성공
- 존재하지 않는 상품 404 반환

**DELETE /api/products/{id} (2 tests)**
- 삭제 성공 (204)
- 존재하지 않는 상품 404 반환

---

## 관련 문서
- [도메인 모델](./domain.md)
- [데이터베이스 스키마](./database.md)
- [Product 기능 개요](./README.md)

## 관련 Issues
- Issue #14: Product API 구현 

---

**Last Updated**: 2025-11-12
