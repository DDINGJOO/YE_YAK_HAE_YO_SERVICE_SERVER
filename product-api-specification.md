# 상품(Product) API 명세서

## 기본 정보
- Base URL: `/api/v1/products`
- Content-Type: `application/json`

## 열거형(Enum) 정의

### ProductScope
상품의 적용 범위를 나타냅니다.

| 값 | 설명 |
|---|---|
| `PLACE` | 플레이스 전체에 적용되는 상품 (모든 룸에서 사용 가능한 공용 비품) |
| `ROOM` | 특정 룸에만 적용되는 상품 (특정 룸 전용 장비) |
| `RESERVATION` | 예약에만 적용되는 상품 (시간과 무관한 음료/간식 등) |

### PricingType
상품의 가격 책정 방식을 나타냅니다.

| 값 | 설명 |
|---|---|
| `INITIAL_PLUS_ADDITIONAL` | 초기 대여료 + 추가 요금 방식 (예: 초기 2시간 10,000원 + 추가 시간당 5,000원) |
| `ONE_TIME` | 1회 대여료 고정 방식 (예: 1회 대여 시 15,000원, 시간 무관) |
| `SIMPLE_STOCK` | 단순 재고 관리 방식 (시간과 무관하게 수량만 관리) |

## DTO 정의

### PricingStrategyDto
가격 전략 정보를 담는 DTO입니다.

```json
{
  "pricingType": "INITIAL_PLUS_ADDITIONAL | ONE_TIME | SIMPLE_STOCK",
  "initialPrice": 10000.00,  // 초기 가격 (필수, 0 이상)
  "additionalPrice": 5000.00  // 추가 가격 (INITIAL_PLUS_ADDITIONAL인 경우만 필수, 다른 타입은 null)
}
```

## API 엔드포인트

### 1. 상품 등록
새로운 상품을 등록합니다.

**Endpoint:** `POST /api/v1/products`

**Request Body:**
```json
{
  "scope": "PLACE | ROOM | RESERVATION",  // 필수
  "placeId": 1,                           // PLACE, ROOM scope인 경우 필수
  "roomId": 1,                            // ROOM scope인 경우만 필수
  "name": "프로젝터",                      // 필수
  "pricingStrategy": {                     // 필수
    "pricingType": "INITIAL_PLUS_ADDITIONAL",
    "initialPrice": 10000.00,
    "additionalPrice": 5000.00
  },
  "totalQuantity": 5                      // 필수, 0 이상
}
```

**Validation Rules:**
- `scope`별 ID 요구사항:
  - `PLACE`: `placeId` 필수, `roomId` null
  - `ROOM`: `placeId`, `roomId` 모두 필수
  - `RESERVATION`: `placeId`, `roomId` 모두 null
- `pricingType`별 가격 요구사항:
  - `INITIAL_PLUS_ADDITIONAL`: `additionalPrice` 필수
  - `ONE_TIME`, `SIMPLE_STOCK`: `additionalPrice` null

**Response:** `201 Created`
```json
{
  "productId": 1,
  "scope": "ROOM",
  "placeId": 1,
  "roomId": 1,
  "name": "프로젝터",
  "pricingStrategy": {
    "pricingType": "INITIAL_PLUS_ADDITIONAL",
    "initialPrice": 10000.00,
    "additionalPrice": 5000.00
  },
  "totalQuantity": 5
}
```

### 2. 상품 단건 조회
특정 상품을 조회합니다.

**Endpoint:** `GET /api/v1/products/{productId}`

**Path Parameters:**
- `productId` (Long): 조회할 상품 ID (양수)

**Response:** `200 OK`
```json
{
  "productId": 1,
  "scope": "ROOM",
  "placeId": 1,
  "roomId": 1,
  "name": "프로젝터",
  "pricingStrategy": {
    "pricingType": "INITIAL_PLUS_ADDITIONAL",
    "initialPrice": 10000.00,
    "additionalPrice": 5000.00
  },
  "totalQuantity": 5
}
```

### 3. 상품 목록 조회
조건에 따라 상품 목록을 조회합니다.

**Endpoint:** `GET /api/v1/products`

**Query Parameters (모두 선택):**
- `scope`: ProductScope 값으로 필터링
- `placeId`: 특정 Place의 상품 조회 (양수)
- `roomId`: 특정 Room의 상품 조회 (양수)

**우선순위:**
1. `roomId`가 있으면 해당 룸의 상품만 조회
2. `placeId`가 있으면 해당 플레이스의 상품만 조회
3. `scope`가 있으면 해당 scope의 상품만 조회
4. 파라미터가 없으면 전체 상품 조회

**Response:** `200 OK`
```json
[
  {
    "productId": 1,
    "scope": "ROOM",
    "placeId": 1,
    "roomId": 1,
    "name": "프로젝터",
    "pricingStrategy": {
      "pricingType": "INITIAL_PLUS_ADDITIONAL",
      "initialPrice": 10000.00,
      "additionalPrice": 5000.00
    },
    "totalQuantity": 5
  },
  {
    "productId": 2,
    "scope": "PLACE",
    "placeId": 1,
    "roomId": null,
    "name": "화이트보드",
    "pricingStrategy": {
      "pricingType": "ONE_TIME",
      "initialPrice": 15000.00,
      "additionalPrice": null
    },
    "totalQuantity": 3
  }
]
```

**주의:** 빈 결과의 경우 빈 배열 `[]`을 반환합니다.

### 4. 상품 수정
기존 상품 정보를 수정합니다.

**Endpoint:** `PUT /api/v1/products/{productId}`

**Path Parameters:**
- `productId` (Long): 수정할 상품 ID (양수)

**Request Body:**
```json
{
  "name": "업데이트된 프로젝터",           // 필수
  "pricingStrategy": {                    // 필수
    "pricingType": "ONE_TIME",
    "initialPrice": 20000.00,
    "additionalPrice": null
  },
  "totalQuantity": 10                     // 필수, 0 이상
}
```

**Note:** `scope`, `placeId`, `roomId`는 수정할 수 없습니다.

**Response:** `200 OK`
```json
{
  "productId": 1,
  "scope": "ROOM",
  "placeId": 1,
  "roomId": 1,
  "name": "업데이트된 프로젝터",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 20000.00,
    "additionalPrice": null
  },
  "totalQuantity": 10
}
```

### 5. 상품 삭제
상품을 삭제합니다.

**Endpoint:** `DELETE /api/v1/products/{productId}`

**Path Parameters:**
- `productId` (Long): 삭제할 상품 ID (양수)

**Response:** `204 No Content`

### 6. 상품 가용성 조회
특정 시간대에 예약 가능한 상품 목록과 수량을 조회합니다.

**Endpoint:** `GET /api/v1/products/availability`

**Query Parameters (모두 필수):**
- `roomId` (Long): 룸 ID (양수)
- `placeId` (Long): 플레이스 ID (양수)
- `timeSlots` (List<LocalDateTime>): 예약하려는 시간 슬롯 목록 (비어있으면 안 됨)
  - 형식: ISO 8601 (예: `2024-01-01T10:00:00`)
  - 복수 개인 경우: `timeSlots=2024-01-01T10:00:00&timeSlots=2024-01-01T11:00:00`

**Response:** `200 OK`
```json
{
  "availableProducts": [
    {
      "productId": 1,
      "name": "프로젝터",
      "availableQuantity": 3,
      "pricingInfo": {
        "pricingType": "INITIAL_PLUS_ADDITIONAL",
        "initialPrice": 10000.00,
        "additionalPrice": 5000.00
      }
    }
  ]
}
```

## 에러 응답

### 400 Bad Request
- 유효성 검증 실패
- Scope에 맞지 않는 ID 제공
- PricingType에 맞지 않는 가격 정보

### 404 Not Found
- 존재하지 않는 상품 ID로 조회/수정/삭제 시도

### 500 Internal Server Error
- 서버 내부 오류

## Gateway 파싱 이슈 해결 방안

게이트웨이에서 응답을 파싱하지 못해 빈 리스트로 나오는 문제가 있다고 하셨습니다. 다음 사항들을 확인해보세요:

1. **Content-Type 헤더 확인**
   - 응답 헤더에 `Content-Type: application/json` 포함 여부
   - 게이트웨이의 Accept 헤더 설정

2. **JSON 형식 확인**
   - 목록 조회 시 항상 배열 형식으로 반환 (빈 결과도 `[]`)
   - 단건 조회 시 객체 형식으로 반환

3. **게이트웨이 설정 확인 사항**
   - Response body 읽기 설정
   - JSON 파서 설정
   - Timeout 설정
   - Buffer 크기 제한

4. **디버깅 제안**
   - 게이트웨이 로그에서 실제 응답 본문 확인
   - `curl` 등으로 직접 API 호출하여 응답 확인
   - 게이트웨이 바이패스하여 직접 호출 테스트

5. **Jackson 설정 확인** (Spring Boot)
   ```properties
   spring.jackson.default-property-inclusion=NON_NULL
   spring.jackson.serialization.write-dates-as-timestamps=false
   ```