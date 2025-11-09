# 프로젝트 요구사항 분석

## 프로젝트 개요

### 프로젝트명
예약 가격 관리 서비스 (Reservation Pricing Service)

### 목적
MSA 아키텍처 환경에서 시간대별 가격 정책, 추가상품 관리, 예약 총 가격 계산을 담당하는 독립적인 마이크로서비스

### 비즈니스 컨텍스트
플레이스(Place)와 룸(Room)을 관리하는 시스템에서 예약 시 발생하는 복잡한 가격 계산 로직을 분리하여 관리함으로써:
- 가격 정책의 유연한 변경 지원
- 시간대별/상품별 세밀한 가격 관리
- 과거 예약의 가격 정보 형상관리 (Immutable Snapshot)

---

## 핵심 요구사항

### 기능 1: 시간대별 예약 가격 세팅

#### 비즈니스 요구사항
1. **룸 정보 동기화**
   - 룸 관리 서버에서 `RoomCreatedEvent` 수신
   - 이벤트 정보: `roomId`, `placeId`, `timeSlot` (HOUR | HALF_HOUR)
   - 룸의 TimeSlot 정보를 중복 저장 (성능 최적화)

2. **가격 정책 관리**
   - 디폴트 가격 설정 (필수)
   - 요일별/시간대별 차등 가격 설정 (선택)
   - 예시:
     ```
     디폴트: 8,000원/슬롯
     월요일 09:00~13:00: 10,000원/슬롯
     월요일 14:00~21:00: 15,000원/슬롯
     ```
   - 미설정 시간대는 디폴트 가격 적용

3. **가격 정책 제약사항**
   - 동일 요일 내 시간대 중복 불가 (Validation)
   - 가격 정책 변경 시 전체 리셋 후 재설정 (원자적 작업)
   - TimeSlot과 정책 시간 정렬 규칙:
     - HOUR 방에 13:30~15:00 정책 설정 시
       - 13:00 슬롯 → 디폴트 가격 (범위 밖)
       - 14:00 슬롯 → 설정 가격 (범위 안)

4. **예약 시 가격 계산**
   - 입력: 연속된 시간 슬롯 시작 시간 리스트
     - 예: `[09:00, 10:00, 11:00, 12:00, 13:00, 14:00]`
   - 출력: 각 슬롯별 가격 + 총 가격
   - 비연속 슬롯 입력 불가 (Validation)

5. **가격 정책 복사**
   - 다른 룸의 가격 정책을 복사하는 API 제공
   - 룸별 독립적 정책 관리

#### 기술적 요구사항
- 이벤트 기반 데이터 동기화
- 시간대 중복 검증 알고리즘
- 요일/시간 범위 검색 최적화

---

### 기능 2: 추가상품 관리

#### 비즈니스 요구사항
1. **상품 범위 (Scope) 3가지**

   **A. 플레이스별 상품 (Place-scoped)**
   - 같은 플레이스의 모든 룸이 공유
   - 시간대별 재고 관리 (렌탈 방식)
   - 예시: 빔프로젝터, 음향장비
   - 제약: 같은 시간대에 총 재고 초과 불가

   **B. 룸별 상품 (Room-scoped)**
   - 특정 룸에만 속한 상품
   - 시간대별 재고 관리
   - 예시: 특정 룸 전용 장비

   **C. 예약별 상품 (Reservation-scoped)**
   - 시간 개념 없는 단순 재고
   - 예시: 음료, 간식

2. **렌탈 상품 가격 정책 2가지**

   **정책 A: 초기 슬롯 + 추가 시간**
   ```
   빔프로젝터:
   - 첫 1시간: 10,000원
   - 추가 시간당: 5,000원

   13:00~16:00 (3시간) 대여
   = 10,000 + 5,000 + 5,000 = 20,000원
   ```

   **정책 B: 1회 대여료 (시간 무관)**
   ```
   스크린:
   - 대여료: 50,000원 (시간 무관)

   13:00~18:00 대여 = 50,000원
   ```

3. **재고 관리 및 상태별 처리**

   ```
   PENDING (예약):
   - 재고 차감 (soft lock)
   - 다른 예약에서 해당 시간대 재고 감소

   CONFIRMED (예약확정):
   - 재고 유지 (이미 차감된 상태)
   - 가격 정보 확정

   CANCELLED (취소):
   - 재고 복구 (+1)
   - 가격 정보는 이력으로 보관
   ```

4. **시간대별 재고 검증 로직**
   ```
   플레이스 A: [Room A, Room B, Room C, Room D]
   렌탈 상품 "빔프로젝터" 총 2개

   [현재 예약 상황]
   Room A: 13:00~18:00 빔프로젝터 1개 (CONFIRMED)

   [신규 예약 요청]
   Room B: 14:00~17:00 빔프로젝터 2개

   [검증]
   - 14:00~17:00 시간대 기존 사용량: 1개
   - 신규 요청량: 2개
   - 총 필요량: 3개 > 총 재고 2개
   - 결과: 거부 (재고 부족)
   ```

5. **기존 예약 보호**
   - 상품 재고 수량 변경 시 기존 CONFIRMED 예약은 영향 없음
   - 신규 예약만 변경된 재고 적용

#### 기술적 요구사항
- 시간대별 재고 집계 알고리즘
- 동시성 제어 (Optimistic/Pessimistic Locking)
- Scope별 다른 검증 로직 (Strategy Pattern)

---

### 기능 3: 예약 총 가격 정보 저장 및 계산

#### 비즈니스 요구사항
1. **3단계 예약 프로세스**

   **1단계: 슬롯 예약**
   - 예약 서비스에서 SlotReservedEvent 발행
   - 가격 서비스가 시간대 가격만 계산
   - PENDING 상태로 생성 (상품 정보 없음)

   **2단계: 상품 추가 및 재고 락**
   - 사용자가 reservationId + 상품 목록 전달
   - 재고 검증 후 ReservationPricing 업데이트
   - 여전히 PENDING 상태 (재고 락 효과)

   **3단계: 결제 확정**
   - 결제 서비스에서 PaymentCompletedEvent 발행
   - 가격 서비스가 PENDING → CONFIRMED로 상태 변경

2. **저장해야 할 정보 (Snapshot)**
   ```
   예약 가격 정보:
   ├─ 시간대별 가격 내역
   │  ├─ 각 슬롯별 시작 시간
   │  ├─ 각 슬롯별 가격
   │  └─ 소계
   ├─ 추가상품별 가격 내역 (2단계에서 추가)
   │  ├─ 상품 ID
   │  ├─ 상품명
   │  ├─ 수량
   │  ├─ 단가
   │  ├─ 가격 정책 타입
   │  └─ 소계
   ├─ 총 가격
   ├─ 계산 시점 (timestamp)
   └─ 예약 상태 (PENDING/CONFIRMED/CANCELLED)
   ```

3. **형상관리 (Immutability)**
   - 가격 정보는 예약 생성 시점의 스냅샷
   - 이후 가격 정책 변경해도 기존 예약 가격 불변
   - 과거 예약의 가격 추적 가능
   - 상품 추가 시에만 가격 정보 업데이트 가능 (PENDING 상태에서만)

4. **예약 상태별 동작**
   ```
   슬롯 예약 (PENDING):
   ├─ SlotReservedEvent 수신
   ├─ 시간대 가격 계산
   └─ 상품 없이 PENDING 상태로 생성

   상품 추가 (PENDING):
   ├─ 재고 가용성 검증
   ├─ 상품 가격 계산 및 업데이트
   └─ 재고 차감 (soft lock)

   예약 확정 (CONFIRMED):
   ├─ PaymentCompletedEvent 수신
   ├─ 재고 유지 (이미 차감됨)
   └─ 가격 정보 확정

   예약 취소 (CANCELLED):
   ├─ 재고 복구
   └─ 가격 정보 보관 (이력)
   ```

#### 기술적 요구사항
- 불변 객체(Immutable Object) 설계
- Transaction 일관성 보장
- 가격 계산 로직의 정확성 (반올림, 통화 처리)

---

## 비기능 요구사항

### 성능
- 예약 생성 응답 시간: 평균 500ms 이하
- 가격 계산 정확도: 100% (금액 오차 허용 불가)
- 동시 예약 처리: 최소 100 TPS

### 확장성
- 룸당 최대 시간대 가격 정책: 10개
- 플레이스당 최대 추가상품: 50개
- 예약당 최대 추가상품 선택: 10개

### 신뢰성
- 재고 차감/복구의 원자성 보장
- 가격 계산 결과의 일관성
- 이벤트 처리 실패 시 재시도 메커니즘

### 유지보수성
- 새로운 가격 정책 추가 용이 (OCP)
- 새로운 상품 범위(Scope) 추가 용이
- 도메인 로직과 인프라 로직 분리

---

## 도메인 규칙 (Invariants)

### 시간대별 가격
1. 디폴트 가격은 항상 존재해야 함 (NOT NULL)
2. 같은 요일 내 시간대 중복 불가
3. 시작 시간 < 종료 시간
4. 예약 슬롯은 연속적이어야 함

### 추가상품
1. 총 재고는 0 이상 (음수 불가)
2. 시간대별 사용량 ≤ 총 재고
3. PENDING + CONFIRMED 상태의 예약만 재고 차감
4. CANCELLED 상태는 재고 집계에서 제외

### 예약 가격
1. 총 가격 = 시간대 가격 합계 + 상품 가격 합계
2. 가격은 생성 후 변경 불가 (Immutable)
3. 상태 전이: PENDING → CONFIRMED 또는 CANCELLED만 가능
4. CANCELLED에서 다른 상태로 전이 불가

---

## 외부 의존성

### 이벤트 수신

#### 1. RoomCreatedEvent
- **Source**: 룸 관리 서버
- **Payload**: `roomId`, `placeId`, `timeSlot`
- **Topic**: `room-events`
- **처리**: 기본 가격 정책 생성
- **상태**: ✅ 구현 완료

#### 2. SlotReservedEvent
- **Source**: 예약 서버
- **Payload**: `reservationId`, `roomId`, `slotDate`, `startTimes`, `occurredAt`
- **Topic**: `reservation-reserved`
- **처리**: 시간대 가격 계산 및 PENDING 상태 ReservationPricing 생성
- **특징**:
  - placeId는 PricingPolicy에서 조회
  - 상품 정보는 포함하지 않음
  - 멱등성 보장 (중복 처리 방지)
- **상태**: ✅ 구현 완료

#### 3. PaymentCompletedEvent
- **Source**: 결제 서버
- **Payload**: `paymentId`, `reservationId`, `amount`, `occurredAt`
- **Topic**: `payment-completed`
- **처리**: PENDING → CONFIRMED 상태 변경
- **상태**: ⚠️ 구현 필요

#### 4. ReservationExpiredEvent (Optional)
- **Source**: 예약 서버
- **Payload**: `reservationId`, `expiredAt`
- **Topic**: `reservation-expired`
- **처리**: 시간 초과된 예약 자동 취소 (재고 복구)
- **상태**: ⚠️ 구현 필요

### 이벤트 발행
이 서비스는 현재 이벤트를 발행하지 않음 (Read-Only from event perspective)

향후 필요 시 고려:
- **PricingCalculatedEvent**: 가격 계산 완료 알림
- **ReservationCancelledEvent**: 예약 취소 알림 (재고 복구 통보)

### 외부 서비스 호출 없음
- 이 서비스는 다른 서비스의 동기 API를 호출하지 않음
- 완전한 이벤트 기반 통신 (Event-Driven)

---

## 제약사항 및 가정

### 제약사항
1. 가격은 원(KRW) 단위, 소수점 없음
2. TimeSlot은 HOUR 또는 HALF_HOUR만 지원
3. 시간대는 LocalDateTime 기준 (타임존 고려 없음)
4. 예약 생성 후 시간/상품 변경 불가 (취소 후 재생성)

### 가정
1. 룸 관리 서버가 먼저 구동되어 있음
2. 이벤트는 최소 1회 전달 보장 (At-least-once)
3. 동일 이벤트 중복 처리 가능 (Idempotent)
4. 할인/프로모션은 별도 서버에서 처리

---

## 성공 지표

### 기능적 성공 지표
- 시간대별 가격 정책 설정 성공률: 100%
- 가격 계산 정확도: 100%
- 재고 차감/복구 정확도: 100%

### 비기능적 성공 지표
- 예약 생성 API 응답 시간 P95: 500ms 이하
- 재고 검증 실패율: 1% 이하 (동시성 충돌)
- 이벤트 처리 지연 시간: 평균 100ms 이하

### 개발 생산성 지표
- 새로운 가격 정책 추가 시간: 1일 이내
- 새로운 상품 타입 추가 시간: 2일 이내
- 단위 테스트 커버리지: 80% 이상

---

## 관련 문서

### 상세 설계 문서
- [예약 플로우 전체 시나리오](../features/reservation/RESERVATION_FLOW.md) - 예약 서비스와 가격 서비스 간 협업 프로세스
- [Product Domain](../features/product/domain.md) - 상품 도메인 설계
- [Pricing Policy Domain](../features/pricing-policy/domain.md) - 가격 정책 도메인 설계
- [Event Handling Architecture](../features/event-handling/architecture.md) - 이벤트 처리 아키텍처

### 아키텍처 문서
- [Architecture Decision Records](../adr/ADR_001_ARCHITECTURE_DECISION.md)
- [Domain Model Design](../architecture/DOMAIN_MODEL_DESIGN.md)
- [Tech Stack Analysis](../architecture/TECH_STACK_ANALYSIS.md)

### 개발 가이드
- [Performance Optimization](../development/performance/optimization.md) - N+1 쿼리 최적화 가이드
- [Performance Testing Guide](../development/performance/testing-guide.md) - 성능 테스트 방법론
