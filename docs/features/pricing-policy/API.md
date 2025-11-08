# 가격 정책 관리 API

## 개요

룸별 가격 정책을 관리하는 REST API입니다. 기본 가격 설정, 시간대별 가격 설정, 정책 복사 기능을 제공합니다.

## 엔드포인트

### 1. 가격 정책 조회

룸의 가격 정책을 조회합니다.

**Request**
```http
GET /api/pricing-policies/{roomId}
```

**Path Parameters**
- `roomId` (Long, required): 룸 ID

**Response (200 OK)**
```json
{
  "roomId": 1,
  "placeId": 100,
  "timeSlot": "HOUR",
  "defaultPrice": 30000,
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "09:00",
      "endTime": "18:00",
      "price": 50000
    }
  ]
}
```

**Error Responses**
- `404 Not Found`: 가격 정책을 찾을 수 없음
```json
{
  "timestamp": "2025-11-09T12:34:56",
  "status": 404,
  "code": "PRICING_001",
  "message": "Pricing policy not found for roomId: 1",
  "path": "/api/pricing-policies/1",
  "exceptionType": "APPLICATION"
}
```

### 2. 기본 가격 업데이트

룸의 기본 가격을 변경합니다.

**Request**
```http
PUT /api/pricing-policies/{roomId}/default-price
Content-Type: application/json

{
  "defaultPrice": 50000
}
```

**Path Parameters**
- `roomId` (Long, required): 룸 ID

**Request Body**
- `defaultPrice` (BigDecimal, required): 새로운 기본 가격 (0 이상)

**Response (200 OK)**
```json
{
  "roomId": 1,
  "placeId": 100,
  "timeSlot": "HOUR",
  "defaultPrice": 50000,
  "timeRangePrices": []
}
```

**Error Responses**
- `400 Bad Request`: 음수 가격 입력
- `404 Not Found`: 가격 정책을 찾을 수 없음

### 3. 시간대별 가격 업데이트

룸의 시간대별 가격을 설정합니다. 기존 시간대별 가격은 모두 대체됩니다.

**Request**
```http
PUT /api/pricing-policies/{roomId}/time-range-prices
Content-Type: application/json

{
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "09:00",
      "endTime": "18:00",
      "price": 50000
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

**Path Parameters**
- `roomId` (Long, required): 룸 ID

**Request Body**
- `timeRangePrices` (Array, required): 시간대별 가격 목록
  - `dayOfWeek` (String, required): 요일 (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
  - `startTime` (String, required): 시작 시간 (HH:mm 형식)
  - `endTime` (String, required): 종료 시간 (HH:mm 형식)
  - `price` (BigDecimal, required): 시간대 가격 (0 이상)

**Response (200 OK)**
```json
{
  "roomId": 1,
  "placeId": 100,
  "timeSlot": "HOUR",
  "defaultPrice": 30000,
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "09:00",
      "endTime": "18:00",
      "price": 50000
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

**Error Responses**
- `400 Bad Request`: 잘못된 요청 데이터
  - 시간 형식 오류
  - 시간 범위 중복
  - 음수 가격
- `404 Not Found`: 가격 정책을 찾을 수 없음

### 4. 가격 정책 복사

다른 룸의 가격 정책을 복사합니다. 같은 플레이스 내의 룸 간에만 복사 가능합니다.

**Request**
```http
POST /api/pricing-policies/{targetRoomId}/copy
Content-Type: application/json

{
  "sourceRoomId": 1
}
```

**Path Parameters**
- `targetRoomId` (Long, required): 대상 룸 ID

**Request Body**
- `sourceRoomId` (Long, required): 원본 룸 ID

**Response (200 OK)**
```json
{
  "roomId": 2,
  "placeId": 100,
  "timeSlot": "HOUR",
  "defaultPrice": 30000,
  "timeRangePrices": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "09:00",
      "endTime": "18:00",
      "price": 50000
    }
  ]
}
```

**Error Responses**
- `400 Bad Request`: 다른 플레이스 간 복사 시도
```json
{
  "timestamp": "2025-11-09T12:34:56",
  "status": 400,
  "code": "PRICING_003",
  "message": "Cannot copy pricing policy between different places",
  "path": "/api/pricing-policies/2/copy",
  "exceptionType": "APPLICATION"
}
```
- `404 Not Found`: 원본 또는 대상 가격 정책을 찾을 수 없음

## 비즈니스 규칙

### 1. 같은 플레이스 제약
가격 정책 복사는 같은 `placeId`를 가진 룸 간에만 가능합니다. 다른 플레이스의 룸으로 복사를 시도하면 `PRICING_003` 에러가 발생합니다.

### 2. 시간대별 가격 우선순위
특정 시간대에 시간대별 가격이 설정되어 있으면 기본 가격보다 우선 적용됩니다.

### 3. 가격 정책 자동 생성
RoomCreatedEvent를 수신하면 해당 룸에 대한 기본 가격 정책이 자동으로 생성됩니다.
- 기본 가격: 0원
- 시간대별 가격: 없음

## 사용 예시

### 예시 1: 룸 생성 후 기본 가격 설정

```bash
# 1. 가격 정책 조회 (자동 생성된 정책 확인)
curl -X GET http://localhost:8080/api/pricing-policies/1

# 2. 기본 가격 설정
curl -X PUT http://localhost:8080/api/pricing-policies/1/default-price \
  -H "Content-Type: application/json" \
  -d '{
    "defaultPrice": 30000
  }'
```

### 예시 2: 평일/주말 차등 가격 설정

```bash
# 평일 주간 및 주말 가격 설정
curl -X PUT http://localhost:8080/api/pricing-policies/1/time-range-prices \
  -H "Content-Type: application/json" \
  -d '{
    "timeRangePrices": [
      {
        "dayOfWeek": "MONDAY",
        "startTime": "09:00",
        "endTime": "18:00",
        "price": 40000
      },
      {
        "dayOfWeek": "SATURDAY",
        "startTime": "00:00",
        "endTime": "23:59",
        "price": 60000
      },
      {
        "dayOfWeek": "SUNDAY",
        "startTime": "00:00",
        "endTime": "23:59",
        "price": 60000
      }
    ]
  }'
```

### 예시 3: 다른 룸으로 정책 복사

```bash
# 룸 1의 가격 정책을 룸 2로 복사 (같은 플레이스일 경우에만)
curl -X POST http://localhost:8080/api/pricing-policies/2/copy \
  -H "Content-Type: application/json" \
  -d '{
    "sourceRoomId": 1
  }'
```

## 관련 문서

- [도메인 모델](../../../docs/architecture/DOMAIN_MODEL_DESIGN.md)
- [아키텍처 개요](../../../docs/architecture/ARCHITECTURE_ANALYSIS.md)
