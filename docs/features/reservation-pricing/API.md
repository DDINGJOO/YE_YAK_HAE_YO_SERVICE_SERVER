# 예약 가격 관리 API

## 개요

예약의 가격 계산, 저장, 상태 관리를 담당하는 REST API입니다. 예약 생성, 확정, 취소, 가격 미리보기, 상품 업데이트 기능을 제공합니다.

## Base URL

```
http://localhost:8080/api/reservations
```

## 공통 응답 형식

### 성공 응답
```json
{
  "reservationId": 1,
  "roomId": 10,
  "status": "PENDING",
  "timeSlotBreakdown": {...},
  "productBreakdowns": [...],
  "totalPrice": 50000,
  "calculatedAt": "2025-01-15T10:00:00",
  "expiresAt": "2025-01-15T10:10:00"
}
```

### 에러 응답
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "RESERVATION_PRICING_001",
  "message": "Product is not available",
  "path": "/api/reservations/pricing",
  "exceptionType": "DOMAIN"
}
```

## 엔드포인트

### 1. 예약 생성

예약 가격을 계산하고 PENDING 상태로 저장합니다.

**Request**
```http
POST /api/reservations/pricing
Content-Type: application/json

{
  "roomId": 10,
  "timeSlots": [
    "2025-01-15T10:00:00",
    "2025-01-15T11:00:00",
    "2025-01-15T12:00:00"
  ],
  "products": [
    {
      "productId": 1,
      "quantity": 1
    },
    {
      "productId": 2,
      "quantity": 3
    }
  ]
}
```

**Request Body**
- `roomId` (Long, required): 룸 ID
- `timeSlots` (List<LocalDateTime>, required): 예약할 시간 슬롯 목록
- `products` (List<ProductRequest>, optional): 추가할 상품 목록
  - `productId` (Long, required): 상품 ID
  - `quantity` (int, required): 수량 (1 이상)

**Response (201 Created)**
```json
{
  "reservationId": 1,
  "roomId": 10,
  "status": "PENDING",
  "timeSlotBreakdown": {
    "slotPrices": [
      {
        "slotTime": "2025-01-15T10:00:00",
        "price": 10000
      },
      {
        "slotTime": "2025-01-15T11:00:00",
        "price": 10000
      },
      {
        "slotTime": "2025-01-15T12:00:00",
        "price": 10000
      }
    ],
    "totalPrice": 30000,
    "timeSlot": "HOUR"
  },
  "productBreakdowns": [
    {
      "productId": 1,
      "productName": "빔프로젝터",
      "quantity": 1,
      "unitPrice": 10000,
      "totalPrice": 10000,
      "pricingType": "ONE_TIME"
    },
    {
      "productId": 2,
      "productName": "아메리카노",
      "quantity": 3,
      "unitPrice": 2000,
      "totalPrice": 6000,
      "pricingType": "SIMPLE_STOCK"
    }
  ],
  "totalPrice": 46000,
  "timeSlotTotal": 30000,
  "productTotal": 16000,
  "calculatedAt": "2025-01-15T10:00:00",
  "expiresAt": "2025-01-15T10:10:00"
}
```

**Error Responses**

**404 Not Found** - 가격 정책을 찾을 수 없음
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 404,
  "code": "RESERVATION_PRICING_002",
  "message": "Pricing policy not found for roomId: 10",
  "path": "/api/reservations/pricing"
}
```

**404 Not Found** - 상품을 찾을 수 없음
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 404,
  "code": "RESERVATION_PRICING_003",
  "message": "Product not found: 999",
  "path": "/api/reservations/pricing"
}
```

**400 Bad Request** - 상품 재고 부족
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "RESERVATION_PRICING_004",
  "message": "Product is not available: 1",
  "path": "/api/reservations/pricing"
}
```

**400 Bad Request** - 유효성 검증 실패
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": [
    {
      "field": "roomId",
      "message": "Room ID must be positive"
    },
    {
      "field": "timeSlots",
      "message": "Time slots cannot be empty"
    }
  ]
}
```

---

### 2. 예약 확정

PENDING 상태의 예약을 CONFIRMED로 변경합니다.

**Request**
```http
PUT /api/reservations/{reservationId}/confirm
```

**Path Parameters**
- `reservationId` (Long, required): 예약 ID (양수)

**Response (200 OK)**
```json
{
  "reservationId": 1,
  "roomId": 10,
  "status": "CONFIRMED",
  "timeSlotBreakdown": {...},
  "productBreakdowns": [...],
  "totalPrice": 46000,
  "timeSlotTotal": 30000,
  "productTotal": 16000,
  "calculatedAt": "2025-01-15T10:00:00",
  "expiresAt": "2025-01-15T10:10:00"
}
```

**Error Responses**

**404 Not Found** - 예약을 찾을 수 없음
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 404,
  "code": "RESERVATION_PRICING_001",
  "message": "Reservation pricing not found: 999",
  "path": "/api/reservations/999/confirm"
}
```

**400 Bad Request** - PENDING 상태가 아님
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "RESERVATION_PRICING_005",
  "message": "Cannot confirm reservation: current status is CONFIRMED",
  "path": "/api/reservations/1/confirm"
}
```

---

### 3. 예약 취소

PENDING 또는 CONFIRMED 상태의 예약을 CANCELLED로 변경합니다.

**Request**
```http
PUT /api/reservations/{reservationId}/cancel
```

**Path Parameters**
- `reservationId` (Long, required): 예약 ID (양수)

**Response (200 OK)**
```json
{
  "reservationId": 1,
  "roomId": 10,
  "status": "CANCELLED",
  "timeSlotBreakdown": {...},
  "productBreakdowns": [...],
  "totalPrice": 46000,
  "timeSlotTotal": 30000,
  "productTotal": 16000,
  "calculatedAt": "2025-01-15T10:00:00",
  "expiresAt": "2025-01-15T10:10:00"
}
```

**Error Responses**

**404 Not Found** - 예약을 찾을 수 없음
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 404,
  "code": "RESERVATION_PRICING_001",
  "message": "Reservation pricing not found: 999",
  "path": "/api/reservations/999/cancel"
}
```

**400 Bad Request** - 이미 취소된 예약
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "RESERVATION_PRICING_006",
  "message": "Cannot cancel reservation: already cancelled",
  "path": "/api/reservations/1/cancel"
}
```

---

### 4. 가격 미리보기

예약을 생성하지 않고 가격만 계산하여 반환합니다.

**Request**
```http
POST /api/reservations/pricing/preview
Content-Type: application/json

{
  "roomId": 10,
  "timeSlots": [
    "2025-01-15T10:00:00",
    "2025-01-15T11:00:00"
  ],
  "products": [
    {
      "productId": 1,
      "quantity": 1
    }
  ]
}
```

**Request Body**
- 예약 생성과 동일한 형식

**Response (200 OK)**
```json
{
  "timeSlotBreakdown": {
    "slotPrices": [
      {
        "slotTime": "2025-01-15T10:00:00",
        "price": 10000
      },
      {
        "slotTime": "2025-01-15T11:00:00",
        "price": 10000
      }
    ],
    "totalPrice": 20000,
    "timeSlot": "HOUR"
  },
  "productBreakdowns": [
    {
      "productId": 1,
      "productName": "빔프로젝터",
      "quantity": 1,
      "unitPrice": 10000,
      "totalPrice": 10000,
      "pricingType": "ONE_TIME"
    }
  ],
  "totalPrice": 30000,
  "timeSlotTotal": 20000,
  "productTotal": 10000
}
```

**주의사항**:
- 예약을 저장하지 않음
- 재고 가용성 검증 수행
- 가격 정책은 현재 시점 기준

**Error Responses**
- 예약 생성과 동일한 에러 응답 (재고 부족, 가격 정책 없음 등)

---

### 5. 상품 업데이트

PENDING 상태의 예약에서 상품을 추가/변경하고 가격을 재계산합니다.

**Request**
```http
PUT /api/reservations/{reservationId}/products
Content-Type: application/json

{
  "products": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```

**Path Parameters**
- `reservationId` (Long, required): 예약 ID (양수)

**Request Body**
- `products` (List<ProductRequest>, required): 새로운 상품 목록
  - `productId` (Long, required): 상품 ID
  - `quantity` (int, required): 수량 (1 이상)

**Response (200 OK)**
```json
{
  "reservationId": 1,
  "roomId": 10,
  "status": "PENDING",
  "timeSlotBreakdown": {...},
  "productBreakdowns": [
    {
      "productId": 1,
      "productName": "빔프로젝터",
      "quantity": 2,
      "unitPrice": 10000,
      "totalPrice": 20000,
      "pricingType": "ONE_TIME"
    },
    {
      "productId": 3,
      "productName": "노트북",
      "quantity": 1,
      "unitPrice": 10000,
      "totalPrice": 10000,
      "pricingType": "INITIAL_PLUS_ADDITIONAL"
    }
  ],
  "totalPrice": 60000,
  "timeSlotTotal": 30000,
  "productTotal": 30000,
  "calculatedAt": "2025-01-15T10:05:00",
  "expiresAt": "2025-01-15T10:10:00"
}
```

**주의사항**:
- PENDING 상태에서만 상품 업데이트 가능
- 기존 상품 목록을 완전히 대체 (부분 업데이트 아님)
- 가격 재계산 및 calculatedAt 갱신
- 재고 가용성 재검증

**Error Responses**

**404 Not Found** - 예약을 찾을 수 없음
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 404,
  "code": "RESERVATION_PRICING_001",
  "message": "Reservation pricing not found: 999",
  "path": "/api/reservations/999/products"
}
```

**400 Bad Request** - PENDING 상태가 아님
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "RESERVATION_PRICING_007",
  "message": "Cannot update products: reservation status is CONFIRMED",
  "path": "/api/reservations/1/products"
}
```

**400 Bad Request** - 상품 재고 부족
```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 400,
  "code": "RESERVATION_PRICING_004",
  "message": "Product is not available: 1",
  "path": "/api/reservations/1/products"
}
```

---

## 예외 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| RESERVATION_PRICING_001 | 404 | 예약을 찾을 수 없음 |
| RESERVATION_PRICING_002 | 404 | 가격 정책을 찾을 수 없음 |
| RESERVATION_PRICING_003 | 404 | 상품을 찾을 수 없음 |
| RESERVATION_PRICING_004 | 400 | 상품 재고 부족 |
| RESERVATION_PRICING_005 | 400 | 예약 확정 불가 (상태 오류) |
| RESERVATION_PRICING_006 | 400 | 예약 취소 불가 (이미 취소됨) |
| RESERVATION_PRICING_007 | 400 | 상품 업데이트 불가 (상태 오류) |
| VALIDATION_ERROR | 400 | 입력 유효성 검증 실패 |

---

## 사용 예시

### 시나리오 1: 회의실 예약 (상품 없음)

```bash
# 1. 가격 미리보기
curl -X POST http://localhost:8080/api/reservations/pricing/preview \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 10,
    "timeSlots": [
      "2025-01-15T10:00:00",
      "2025-01-15T11:00:00"
    ],
    "products": []
  }'

# 2. 예약 생성
curl -X POST http://localhost:8080/api/reservations/pricing \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 10,
    "timeSlots": [
      "2025-01-15T10:00:00",
      "2025-01-15T11:00:00"
    ],
    "products": []
  }'

# 3. 결제 완료 후 예약 확정
curl -X PUT http://localhost:8080/api/reservations/1/confirm
```

### 시나리오 2: 회의실 + 상품 예약

```bash
# 1. 예약 생성 (빔프로젝터 + 음료)
curl -X POST http://localhost:8080/api/reservations/pricing \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 10,
    "timeSlots": [
      "2025-01-15T10:00:00",
      "2025-01-15T11:00:00",
      "2025-01-15T12:00:00"
    ],
    "products": [
      {"productId": 1, "quantity": 1},
      {"productId": 2, "quantity": 3}
    ]
  }'

# 2. PENDING 상태에서 상품 변경 (노트북 추가)
curl -X PUT http://localhost:8080/api/reservations/1/products \
  -H "Content-Type: application/json" \
  -d '{
    "products": [
      {"productId": 1, "quantity": 1},
      {"productId": 2, "quantity": 3},
      {"productId": 3, "quantity": 1}
    ]
  }'

# 3. 결제 완료 후 예약 확정
curl -X PUT http://localhost:8080/api/reservations/1/confirm
```

### 시나리오 3: 예약 취소

```bash
# PENDING 상태 취소 (결제 전)
curl -X PUT http://localhost:8080/api/reservations/1/cancel

# CONFIRMED 상태 취소 (결제 후 환불)
curl -X PUT http://localhost:8080/api/reservations/2/cancel
```

---

## 상태 전이 다이어그램

```
PENDING (예약 생성)
   |
   |-- confirm() --> CONFIRMED (결제 완료)
   |                     |
   |                     |-- cancel() --> CANCELLED
   |
   |-- cancel() -------> CANCELLED (결제 전 취소)
   |
   |-- timeout --------> CANCELLED (자동 취소, 10분 후)
```

---

## 재고 관리

### PLACE Scope 상품
- 플레이스 내 모든 룸의 예약 확인
- 요청 시간대와 겹치는 PENDING/CONFIRMED 예약의 상품 사용량 계산
- 최대 사용량 < totalQuantity 확인

### ROOM Scope 상품
- 특정 룸의 예약만 확인
- PLACE Scope와 동일한 계산 로직

### RESERVATION Scope 상품
- 단순 재고 체크
- requestedQuantity <= totalQuantity

---

## PENDING 타임아웃

### 정책
- 기본 타임아웃: 10분 (application.yml 설정)
- 스케줄러: 매 1분마다 실행
- 대상: status = PENDING && now > expiresAt

### 동작
1. 스케줄러가 만료된 PENDING 예약 조회
2. 자동으로 CANCELLED 상태로 변경
3. 재고 해제 (CANCELLED는 재고 차감 제외)

### 설정
```yaml
reservation:
  pending:
    timeout-minutes: 10
    scheduler:
      cron: "0 */1 * * * *"
```

---

## 이벤트 기반 상태 전이

### ReservationConfirmed 이벤트 (Payment 서비스)
```json
{
  "eventType": "ReservationConfirmed",
  "reservationId": 1,
  "paymentId": 12345,
  "timestamp": "2025-01-15T10:05:00"
}
```

**처리**:
- PENDING → CONFIRMED 상태 전환
- 멱등성 보장 (중복 이벤트 무시)

### ReservationCancelled 이벤트 (Reservation 서비스)
```json
{
  "eventType": "ReservationCancelled",
  "reservationId": 1,
  "reason": "USER_CANCELLED",
  "timestamp": "2025-01-15T10:20:00"
}
```

**처리**:
- PENDING/CONFIRMED → CANCELLED 상태 전환
- 멱등성 보장

### ReservationRefund 이벤트 (Payment 서비스, Issue #164)
```json
{
  "eventType": "ReservationRefund",
  "reservationId": 1,
  "refundedAt": "2025-01-07T14:30:00",
  "reason": "USER_REQUEST"
}
```

**처리**:
- CONFIRMED → CANCELLED 상태 전환
- 예약에 포함된 모든 상품 재고 해제
- RESERVATION Scope: products.reserved_quantity 감소
- ROOM/PLACE Scope: product_time_slot_inventory.reserved_quantity 감소
- 멱등성 보장 (이미 CANCELLED 상태면 무시)

---

## 성능 최적화

### 1. 인덱스 활용
- `idx_reservation_pricings_room_status`: 룸별 재고 조회
- `idx_reservation_pricings_place_status`: 플레이스별 재고 조회
- `idx_reservation_pricing_slots_time`: 시간대별 조회

### 2. EAGER Fetch
- ElementCollection을 EAGER로 설정
- 단일 쿼리로 모든 데이터 로드
- N+1 문제 방지

### 3. PlaceId 비정규화
- RoomId → PlaceId 조인 불필요
- PLACE Scope 재고 조회 성능 향상

---

## 테스트

### Postman Collection
```json
{
  "info": {
    "name": "Reservation Pricing API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Reservation",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/reservations/pricing",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"roomId\": 10,\n  \"timeSlots\": [\"2025-01-15T10:00:00\"],\n  \"products\": []\n}"
        }
      }
    },
    {
      "name": "Preview Price",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/reservations/pricing/preview",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"roomId\": 10,\n  \"timeSlots\": [\"2025-01-15T10:00:00\"],\n  \"products\": []\n}"
        }
      }
    },
    {
      "name": "Confirm Reservation",
      "request": {
        "method": "PUT",
        "url": "{{baseUrl}}/api/reservations/{{reservationId}}/confirm"
      }
    },
    {
      "name": "Cancel Reservation",
      "request": {
        "method": "PUT",
        "url": "{{baseUrl}}/api/reservations/{{reservationId}}/cancel"
      }
    },
    {
      "name": "Update Products",
      "request": {
        "method": "PUT",
        "url": "{{baseUrl}}/api/reservations/{{reservationId}}/products",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"products\": [{\"productId\": 1, \"quantity\": 2}]\n}"
        }
      }
    }
  ]
}
```

### cURL Scripts
```bash
# 환경 변수 설정
export BASE_URL=http://localhost:8080

# 예약 생성
export RESERVATION_ID=$(curl -s -X POST $BASE_URL/api/reservations/pricing \
  -H "Content-Type: application/json" \
  -d '{"roomId": 10, "timeSlots": ["2025-01-15T10:00:00"], "products": []}' \
  | jq -r '.reservationId')

echo "Created reservation: $RESERVATION_ID"

# 예약 확정
curl -X PUT $BASE_URL/api/reservations/$RESERVATION_ID/confirm

# 예약 취소
curl -X PUT $BASE_URL/api/reservations/$RESERVATION_ID/cancel
```

---

## 참고 사항

### 가격 불변성
- 예약 생성 시점의 가격 스냅샷 저장
- 이후 PricingPolicy나 Product 가격 변경에도 예약 가격은 불변
- 단, PENDING 상태에서 상품 업데이트 시에만 가격 재계산

### 상품 목록 업데이트
- 완전 대체 방식 (부분 업데이트 아님)
- 기존 상품 제거하려면 products 배열에서 제외
- 빈 배열 전송 시 모든 상품 제거 (시간대 가격만 남음)

### 타임아웃 관리
- PENDING 상태 예약은 타임아웃 관리 필요
- expiresAt 시각 초과 시 스케줄러가 자동 취소
- 클라이언트는 expiresAt을 UI에 표시하여 사용자에게 알림 권장

### 멱등성
- 동일한 reservationId에 대해 confirm/cancel 중복 호출 시 안전
- 이미 목표 상태인 경우 예외 발생
- 이벤트 핸들러는 멱등성 보장 (중복 이벤트 무시)
---

**Last Updated**: 2025-11-12
