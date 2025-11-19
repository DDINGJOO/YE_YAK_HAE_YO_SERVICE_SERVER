# API 명세서

## 목차
1. [상품 관리 API](#1-상품-관리-api)
2. [가격 정책 관리 API](#2-가격-정책-관리-api)
3. [예약 가격 관리 API](#3-예약-가격-관리-api)
4. [룸 허용 상품 관리 API (Admin)](#4-룸-허용-상품-관리-api-admin)
5. [룸별 상품 조회 API](#5-룸별-상품-조회-api)

---

## 1. 상품 관리 API

### 1.1 상품 등록
- **Endpoint**: `POST /api/v1/products`
- **설명**: 새로운 추가상품을 등록합니다.
- **Request Body**:
```json
{
  "scope": "PLACE | ROOM | RESERVATION",
  "placeId": 1,
  "roomId": 1,
  "name": "상품명",
  "pricingStrategy": {
    "pricingType": "INITIAL_PLUS_ADDITIONAL | ONE_TIME | SIMPLE_STOCK",
    "initialPrice": 10000,
    "additionalPrice": 5000
  },
  "totalQuantity": 10
}
```

**필드 설명**:
- `scope`: 상품 적용 범위 (필수)
  - `PLACE`: 플레이스 전체
  - `ROOM`: 특정 룸
  - `RESERVATION`: 모든 예약
- `placeId`: PLACE/ROOM scope일 때 필수
- `roomId`: ROOM scope일 때만 필수
- `pricingStrategy.pricingType`: 가격 전략 타입 (필수)
  - `INITIAL_PLUS_ADDITIONAL`: 초기가격 + 추가가격
  - `ONE_TIME`: 1회성 가격
  - `SIMPLE_STOCK`: 단순 재고 가격
- `pricingStrategy.initialPrice`: 초기 가격 (필수, >= 0)
- `pricingStrategy.additionalPrice`: 추가 가격 (INITIAL_PLUS_ADDITIONAL일 때만 필수)
- `totalQuantity`: 총 수량 (필수, >= 0)

**Response**: `201 Created`
```json
{
  "productId": 1,
  "scope": "PLACE",
  "placeId": 1,
  "roomId": null,
  "name": "상품명",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 10000,
    "additionalPrice": null
  },
  "totalQuantity": 10
}
```

---

### 1.2 상품 조회 (단건)
- **Endpoint**: `GET /api/v1/products/{productId}`
- **설명**: 상품 ID로 상품 정보를 조회합니다.
- **Path Parameters**:
  - `productId` (Long, 필수): 상품 ID (양수)
- **Response**: `200 OK`
```json
{
  "productId": 1,
  "scope": "PLACE",
  "placeId": 1,
  "roomId": null,
  "name": "상품명",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 10000,
    "additionalPrice": null
  },
  "totalQuantity": 10
}
```

---

### 1.3 상품 목록 조회
- **Endpoint**: `GET /api/v1/products`
- **설명**: 상품 목록을 조회합니다. Query Parameter로 필터링할 수 있습니다.
- **Query Parameters** (모두 선택):
  - `scope` (ProductScope): Scope로 필터링
  - `placeId` (Long): PlaceId로 필터링 (양수)
  - `roomId` (Long): RoomId로 필터링 (양수)
- **필터 우선순위**: `roomId` > `placeId` > `scope` > all
- **Response**: `200 OK`
```json
[
  {
    "productId": 1,
    "scope": "PLACE",
    "placeId": 1,
    "roomId": null,
    "name": "상품명",
    "pricingStrategy": {
      "pricingType": "ONE_TIME",
      "initialPrice": 10000,
      "additionalPrice": null
    },
    "totalQuantity": 10
  }
]
```

---

### 1.4 상품 수정
- **Endpoint**: `PUT /api/v1/products/{productId}`
- **설명**: 상품 정보를 수정합니다.
- **Path Parameters**:
  - `productId` (Long, 필수): 상품 ID (양수)
- **Request Body**:
```json
{
  "name": "수정된 상품명",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 15000,
    "additionalPrice": null
  },
  "totalQuantity": 20
}
```
- **Response**: `200 OK`
```json
{
  "productId": 1,
  "scope": "PLACE",
  "placeId": 1,
  "roomId": null,
  "name": "수정된 상품명",
  "pricingStrategy": {
    "pricingType": "ONE_TIME",
    "initialPrice": 15000,
    "additionalPrice": null
  },
  "totalQuantity": 20
}
```

---

### 1.5 상품 삭제
- **Endpoint**: `DELETE /api/v1/products/{productId}`
- **설명**: 상품을 삭제합니다.
- **Path Parameters**:
  - `productId` (Long, 필수): 상품 ID (양수)
- **Response**: `204 No Content`

---

### 1.6 상품 재고 가용성 조회
- **Endpoint**: `GET /api/v1/products/availability`
- **설명**: 특정 시간대에 예약 가능한 상품 목록과 각 상품의 가용 수량을 조회합니다.
- **Query Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
  - `placeId` (Long, 필수): 플레이스 ID (양수)
  - `timeSlots` (List<LocalDateTime>, 필수): 예약하려는 시간 슬롯 목록 (비어있지 않아야 함)
- **예시**: `GET /api/v1/products/availability?roomId=1&placeId=1&timeSlots=2025-01-20T10:00:00&timeSlots=2025-01-20T11:00:00`
- **Response**: `200 OK`
```json
{
  "availableProducts": [
    {
      "productId": 1,
      "productName": "상품명",
      "availableQuantity": 5
    }
  ]
}
```

---

## 2. 가격 정책 관리 API

### 2.1 가격 정책 조회
- **Endpoint**: `GET /api/v1/pricing-policies/{roomId}`
- **설명**: 특정 룸의 가격 정책을 조회합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Response**: `200 OK`
```json
{
  "roomId": 1,
  "placeId": 1,
  "timeSlot": "ONE_HOUR",
  "defaultPrice": 50000,
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "18:00",
      "endTime": "22:00",
      "pricePerSlot": 60000
    }
  ]
}
```

---

### 2.2 특정 날짜의 시간대별 가격 조회
- **Endpoint**: `GET /api/v1/pricing-policies/{roomId}/date/{date}`
- **설명**: 특정 날짜의 시간대별 가격을 조회합니다. 시작 시간을 키로, 해당 타임슬롯의 가격을 값으로 가지는 Map을 반환합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
  - `date` (LocalDate, 필수): 조회할 날짜 (yyyy-MM-dd)
- **예시**: `GET /api/v1/pricing-policies/1/date/2025-01-20`
- **Response**: `200 OK`
```json
{
  "timeSlotPrices": {
    "10:00": 50000,
    "11:00": 50000,
    "12:00": 50000,
    "18:00": 60000,
    "19:00": 60000
  }
}
```

---

### 2.3 기본 가격 업데이트
- **Endpoint**: `PUT /api/v1/pricing-policies/{roomId}/default-price`
- **설명**: 룸의 기본 가격을 업데이트합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Request Body**:
```json
{
  "defaultPrice": 55000
}
```
- **Response**: `200 OK`
```json
{
  "roomId": 1,
  "placeId": 1,
  "timeSlot": "ONE_HOUR",
  "defaultPrice": 55000,
  "timeRangePrices": []
}
```

---

### 2.4 시간대별 가격 업데이트
- **Endpoint**: `PUT /api/v1/pricing-policies/{roomId}/time-range-prices`
- **설명**: 룸의 시간대별 가격을 업데이트합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Request Body**:
```json
{
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "18:00",
      "endTime": "22:00",
      "price": 60000
    },
    {
      "dayOfWeek": "FRIDAY",
      "startTime": "18:00",
      "endTime": "23:00",
      "price": 70000
    }
  ]
}
```
- **Response**: `200 OK`
```json
{
  "roomId": 1,
  "placeId": 1,
  "timeSlot": "ONE_HOUR",
  "defaultPrice": 50000,
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "18:00",
      "endTime": "22:00",
      "pricePerSlot": 60000
    },
    {
      "dayOfWeek": "FRIDAY",
      "startTime": "18:00",
      "endTime": "23:00",
      "pricePerSlot": 70000
    }
  ]
}
```

---

### 2.5 다른 룸의 가격 정책 복사
- **Endpoint**: `POST /api/v1/pricing-policies/{targetRoomId}/copy`
- **설명**: 다른 룸의 가격 정책을 복사합니다. 같은 PlaceId를 가진 룸 간에만 복사 가능합니다.
- **Path Parameters**:
  - `targetRoomId` (Long, 필수): 복사 대상 룸 ID (양수)
- **Request Body**:
```json
{
  "sourceRoomId": 2
}
```
- **Response**: `200 OK`
```json
{
  "roomId": 1,
  "placeId": 1,
  "timeSlot": "ONE_HOUR",
  "defaultPrice": 50000,
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "18:00",
      "endTime": "22:00",
      "pricePerSlot": 60000
    }
  ]
}
```

---

## 3. 예약 가격 관리 API

### 3.1 예약 가격 미리보기
- **Endpoint**: `POST /api/v1/reservations/preview`
- **설명**: 예약을 생성하지 않고 가격만 계산하여 반환합니다.
- **Request Body**:
```json
{
  "roomId": 1,
  "timeSlots": [
    "2025-01-20T10:00:00",
    "2025-01-20T11:00:00"
  ],
  "products": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}
```

**필드 설명**:
- `roomId`: 룸 ID (필수)
- `timeSlots`: 예약 시간 슬롯 목록 (필수, 비어있지 않아야 함)
- `products`: 상품 목록 (필수, 비어있지 않아야 함)
  - `productId`: 상품 ID (필수)
  - `quantity`: 수량 (필수, >= 1)

**Response**: `200 OK`
```json
{
  "timeSlotPrice": 100000,
  "productBreakdowns": [
    {
      "productId": 1,
      "productName": "상품명1",
      "quantity": 2,
      "unitPrice": 10000,
      "subtotal": 20000
    },
    {
      "productId": 2,
      "productName": "상품명2",
      "quantity": 1,
      "unitPrice": 5000,
      "subtotal": 5000
    }
  ],
  "totalPrice": 125000
}
```

---

### 3.2 예약 확정
- **Endpoint**: `PUT /api/v1/reservations/{reservationId}/confirm`
- **설명**: 예약을 확정합니다.
- **Path Parameters**:
  - `reservationId` (Long, 필수): 예약 ID (양수)
- **Response**: `200 OK`
```json
{
  "reservationId": 1,
  "roomId": 1,
  "status": "CONFIRMED",
  "totalPrice": 125000,
  "calculatedAt": "2025-01-18T10:00:00"
}
```

---

### 3.3 예약 취소
- **Endpoint**: `PUT /api/v1/reservations/{reservationId}/cancel`
- **설명**: 예약을 취소합니다.
- **Path Parameters**:
  - `reservationId` (Long, 필수): 예약 ID (양수)
- **Response**: `200 OK`
```json
{
  "reservationId": 1,
  "roomId": 1,
  "status": "CANCELLED",
  "totalPrice": 125000,
  "calculatedAt": "2025-01-18T10:00:00"
}
```

---

### 3.4 예약 상품 업데이트
- **Endpoint**: `PUT /api/v1/reservations/{reservationId}/products`
- **설명**: 예약 상품을 업데이트하고 가격을 재계산합니다. PENDING 상태의 예약에서만 가능합니다.
- **Path Parameters**:
  - `reservationId` (Long, 필수): 예약 ID (양수)
- **Request Body**:
```json
{
  "products": [
    {
      "productId": 1,
      "quantity": 3
    }
  ]
}
```
- **Response**: `200 OK`
```json
{
  "reservationId": 1,
  "roomId": 1,
  "status": "PENDING",
  "totalPrice": 130000,
  "calculatedAt": "2025-01-18T10:30:00"
}
```

---

## 4. 룸 허용 상품 관리 API (Admin)

### 4.1 룸 허용 상품 설정
- **Endpoint**: `POST /api/v1/admin/rooms/{roomId}/allowed-products`
- **설명**: 특정 룸의 허용 상품 목록을 설정합니다. 기존 매핑은 모두 삭제되고 새로운 매핑이 저장됩니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Request Body**:
```json
{
  "productIds": [1, 2, 3]
}
```
- **Response**: `200 OK`
```json
{
  "roomId": 1,
  "allowedProductIds": [1, 2, 3]
}
```

---

### 4.2 룸 허용 상품 조회
- **Endpoint**: `GET /api/v1/admin/rooms/{roomId}/allowed-products`
- **설명**: 특정 룸의 허용 상품 목록을 조회합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Response**: `200 OK`
```json
{
  "roomId": 1,
  "allowedProductIds": [1, 2, 3]
}
```

---

### 4.3 룸 허용 상품 삭제
- **Endpoint**: `DELETE /api/v1/admin/rooms/{roomId}/allowed-products`
- **설명**: 특정 룸의 모든 허용 상품 매핑을 삭제합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Response**: `204 No Content`

---

## 5. 룸별 상품 조회 API

### 5.1 룸에서 이용 가능한 상품 조회
- **Endpoint**: `GET /api/v1/rooms/{roomId}/available-products`
- **설명**: 특정 룸에서 이용 가능한 상품 목록을 조회합니다.
- **Path Parameters**:
  - `roomId` (Long, 필수): 룸 ID (양수)
- **Query Parameters**:
  - `placeId` (Long, 필수): 플레이스 ID (양수)

**조회되는 상품**:
- ROOM Scope: 해당 roomId를 가진 상품
- PLACE Scope: RoomAllowedProduct에 허용된 상품
- RESERVATION Scope: 모든 룸에서 사용 가능한 상품

**예시**: `GET /api/v1/rooms/1/available-products?placeId=1`

**Response**: `200 OK`
```json
[
  {
    "productId": 1,
    "scope": "ROOM",
    "placeId": 1,
    "roomId": 1,
    "name": "룸 전용 상품",
    "pricingStrategy": {
      "pricingType": "ONE_TIME",
      "initialPrice": 10000,
      "additionalPrice": null
    },
    "totalQuantity": 10
  },
  {
    "productId": 2,
    "scope": "PLACE",
    "placeId": 1,
    "roomId": null,
    "name": "플레이스 상품",
    "pricingStrategy": {
      "pricingType": "SIMPLE_STOCK",
      "initialPrice": 5000,
      "additionalPrice": null
    },
    "totalQuantity": 20
  },
  {
    "productId": 3,
    "scope": "RESERVATION",
    "placeId": null,
    "roomId": null,
    "name": "예약 공통 상품",
    "pricingStrategy": {
      "pricingType": "INITIAL_PLUS_ADDITIONAL",
      "initialPrice": 3000,
      "additionalPrice": 2000
    },
    "totalQuantity": 50
  }
]
```

---

## 공통 응답 코드

| HTTP Status Code | 설명 |
|------------------|------|
| 200 OK | 요청이 성공적으로 처리됨 |
| 201 Created | 리소스가 성공적으로 생성됨 |
| 204 No Content | 요청이 성공적으로 처리되었으나 반환할 내용이 없음 |
| 400 Bad Request | 잘못된 요청 (유효성 검증 실패) |
| 404 Not Found | 요청한 리소스를 찾을 수 없음 |
| 500 Internal Server Error | 서버 내부 오류 |

---

## Enum 타입 정의

### ProductScope
- `PLACE`: 플레이스 범위
- `ROOM`: 룸 범위
- `RESERVATION`: 예약 범위

### PricingType
- `INITIAL_PLUS_ADDITIONAL`: 초기가격 + 추가가격
- `ONE_TIME`: 1회성 가격
- `SIMPLE_STOCK`: 단순 재고 가격

### ReservationStatus
- `PENDING`: 대기중
- `CONFIRMED`: 확정됨
- `CANCELLED`: 취소됨

### DayOfWeek
- `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`