# ReservationPricing 기능

## 개요
예약 시점의 가격 정보를 스냅샷으로 저장하고 관리하는 기능입니다. 시간대별 가격과 상품별 가격을 계산하여 예약 생성, 확정, 취소 생애주기를 관리합니다.

## 목적
- 예약 시점 가격의 불변성 보장 (이후 가격 정책 변경에도 영향 없음)
- 시간대별 가격 + 상품별 가격의 정확한 계산
- 예약 상태 관리 (PENDING → CONFIRMED → CANCELLED)
- PENDING 상태 타임아웃 관리
- 상품 업데이트 및 가격 재계산 (PENDING 상태에서만)

## 주요 컴포넌트

### Domain Layer (Task #74)
- **ReservationPricing**: Aggregate Root
  - ReservationId를 Aggregate ID로 사용
  - 예약 상태(PENDING, CONFIRMED, CANCELLED) 관리
  - 시간대 가격 + 상품 가격 계산 및 스냅샷 저장
  - 가격 불변성 보장
- **TimeSlotPriceBreakdown**: Value Object
  - 시간대별 가격 내역 관리
  - Map<LocalDateTime, Money> 형태로 슬롯별 가격 저장
  - TimeSlot 단위(HOUR/HALFHOUR) 정보 포함
- **ProductPriceBreakdown**: Record (불변 객체)
  - 상품별 가격 스냅샷 (productId, name, quantity, unitPrice, totalPrice)
  - PricingType 포함
- **ReservationStatus**: Enum
  - PENDING: 예약 대기 (결제 전)
  - CONFIRMED: 예약 확정 (결제 완료)
  - CANCELLED: 예약 취소

### Repository Layer (Task #74)
- **ReservationPricingRepository**: Port (출력 포트)
  - findById, findByRoomId, findByPlaceId 조회 메서드
  - findByPlaceIdAndTimeRange, findByRoomIdAndTimeRange (재고 관리용)
  - findByStatus (상태별 조회)
  - save, delete CRUD 메서드
- **ReservationPricingRepositoryAdapter**: JPA Adapter
  - Domain Entity ↔ JPA Entity 변환
- **ReservationPricingEntity**: JPA Entity
  - @ElementCollection을 사용한 시간대별 가격 매핑 (reservation_pricing_slots)
  - @ElementCollection을 사용한 상품별 가격 매핑 (reservation_pricing_products)
- **Flyway Migration**:
  - V4__create_reservation_pricing_tables.sql
  - V7__add_expires_at_to_reservation_pricings.sql (PENDING 타임아웃)

### Application Layer (Task #74)
- **Use Case Ports**: 입력 포트 (5개)
  - CreateReservationUseCase: 예약 생성 및 확정/취소
  - CalculateReservationPriceUseCase: 가격 미리보기
  - UpdateReservationProductsUseCase: 상품 업데이트
  - ConfirmReservationUseCase: 예약 확정 (이벤트 발행)
  - CancelReservationUseCase: 예약 취소 (이벤트 발행)
- **Application Services**: Use Case 구현
  - ReservationPricingService: 예약 생성/확정/취소 로직
  - PricePreviewService: 가격 미리보기 (예약 저장 없이 계산만)
- **ProductAvailabilityService**: Domain Service
  - 상품 재고 가용성 검증 (PLACE/ROOM/RESERVATION Scope별)
  - 시간대별 재고 계산
  - PENDING/CONFIRMED 상태의 예약만 재고 차감

### Event Handling Layer (Task #88, #89, Issue #164)
- **ReservationConfirmedEventHandler**: 결제 완료 이벤트 처리
  - ReservationConfirmed 이벤트 수신 시 PENDING → CONFIRMED 전환
  - 멱등성 보장 (중복 처리 방지)
- **ReservationCancelledEventHandler**: 결제 취소 이벤트 처리
  - ReservationCancelled 이벤트 수신 시 → CANCELLED 전환
  - 멱등성 보장
- **ReservationRefundEventHandler**: 예약 환불 이벤트 처리 (Issue #164)
  - ReservationRefund 이벤트 수신 시 CONFIRMED → CANCELLED 전환
  - 상품 재고 해제 (releaseQuantity)
  - 멱등성 보장
- **PendingReservationTimeoutScheduler**: PENDING 타임아웃 처리
  - 스케줄러 (매 1분마다 실행)
  - 만료된 PENDING 예약 자동 취소
  - ShedLock 분산 락 (중복 실행 방지)

### API Layer (Task #74)
- **ReservationPricingController**: REST Controller
  - POST /api/reservations/pricing: 예약 생성 (201 Created)
  - PUT /api/reservations/{id}/confirm: 예약 확정 (200 OK)
  - PUT /api/reservations/{id}/cancel: 예약 취소 (200 OK)
  - POST /api/reservations/pricing/preview: 가격 미리보기 (200 OK)
  - PUT /api/reservations/{id}/products: 상품 업데이트 (200 OK)

## 관련 문서
- [도메인 모델](./domain.md)
- [데이터베이스 스키마](../../implementation/DATABASE_SCHEMA.md) - 전체 DB 스키마 (reservation_pricings 테이블)
- [API 문서](./API.md)

## 관련 Issues
- Task #74: ReservationPricing Aggregate 및 가격 계산 로직 구현 (완료)
- Task #88: Payment Event Handlers 구현 (완료)
- Task #89: PENDING timeout 처리 구현 (완료)
- Story #4: 예약 가격 계산 및 저장 (완료)
- Issue #157: 재고 예약/해제 로직 구현 (완료)
- Issue #164: 예약 환불 이벤트 처리 유즈케이스 구현 (완료)

## 예약 상태 전이

### PENDING (예약 대기)
- 예약 생성 시 초기 상태
- expiresAt 설정 (기본 10분)
- 상품 업데이트 가능
- 타임아웃 시 자동 CANCELLED

### CONFIRMED (예약 확정)
- ReservationConfirmed 이벤트 수신 시 전환
- PENDING 상태에서만 전환 가능
- 상품 업데이트 불가
- 취소 가능 (→ CANCELLED)

### CANCELLED (예약 취소)
- ReservationCancelled 이벤트 수신 시 전환
- PENDING 또는 CONFIRMED 상태에서 전환 가능
- 최종 상태 (더 이상 전이 없음)

## 가격 계산 로직

### 1. 시간대 가격 계산
- PricingPolicy의 calculatePriceBreakdown() 사용
- 요청된 시간대 슬롯별 가격 계산
- 요일/시간대별 차등 가격 적용
- TimeSlotPriceBreakdown으로 스냅샷 저장

### 2. 상품 가격 계산
- 각 상품의 PricingStrategy.calculate(quantity) 사용
- INITIAL_PLUS_ADDITIONAL: 첫 개 초기 가격 + 추가 가격
- ONE_TIME: 수량 무관 1회 가격
- SIMPLE_STOCK: 개당 단가 × 수량
- ProductPriceBreakdown으로 스냅샷 저장

### 3. 총 가격 계산
```
totalPrice = timeSlotTotal + productTotal
```

### 4. 재고 가용성 검증
- ProductAvailabilityService.isAvailable() 호출
- Scope별 재고 계산 (PLACE/ROOM/RESERVATION)
- 시간대별 중복 예약 고려
- PENDING/CONFIRMED만 재고 차감

## 비즈니스 규칙

### 1. 가격 불변성
- 예약 생성 시점의 가격 스냅샷 저장
- 이후 PricingPolicy나 Product 가격 변경 시에도 예약 가격은 불변

### 2. 상태 전이 규칙
- PENDING → CONFIRMED: ReservationConfirmed 이벤트만 가능
- PENDING/CONFIRMED → CANCELLED: ReservationCancelled 이벤트 가능
- CANCELLED → (다른 상태): 불가능

### 3. 상품 업데이트 규칙
- PENDING 상태에서만 상품 업데이트 가능
- CONFIRMED/CANCELLED 상태에서는 불가능
- 업데이트 시 가격 재계산 및 calculatedAt 갱신

### 4. 재고 관리 규칙
- PENDING과 CONFIRMED 상태만 재고 차감
- CANCELLED 상태는 재고 차감 제외
- 시간대별 최대 사용량 계산 (MAX 전략)

### 5. PENDING 타임아웃 규칙
- 기본 10분 (application.yml 설정)
- expiresAt < now && status = PENDING → 자동 CANCELLED
- 스케줄러가 매 1분마다 체크

## 테스트 커버리지

### Domain Layer (Task #74)
- **ReservationPricingTest**: 83개 테스트
  - Factory Method 테스트 (calculate, restore)
  - 상태 전이 테스트 (confirm, cancel)
  - 상품 업데이트 테스트
  - 가격 일관성 검증 테스트
  - Validation 테스트
- **TimeSlotPriceBreakdownTest**: 시간대 가격 내역 테스트
- **ProductAvailabilityServiceTest**: 112개 테스트 (재고 가용성)

### Repository Layer (Task #74)
- **ReservationPricingRepositoryAdapterTest**: 30개 통합 테스트
  - CRUD 동작 테스트
  - 시간대별 조회 테스트 (findByPlaceIdAndTimeRange 등)
  - 상태별 조회 테스트 (findByStatus)
  - ElementCollection 저장/조회 테스트

### Application Layer (Task #74)
- **ReservationPricingServiceTest**: 32개 단위 테스트
  - 예약 생성 성공/실패 케이스
  - 예약 확정/취소 성공/실패 케이스
  - 재고 부족 예외 처리
- **PricePreviewServiceTest**: 가격 미리보기 테스트

### Event Handling Layer (Task #88, #89)
- **ReservationConfirmedEventHandlerTest**: 11개 통합 테스트
  - 이벤트 수신 시 PENDING → CONFIRMED 전환
  - 멱등성 테스트 (중복 처리 방지)
  - 예외 처리 테스트
- **ReservationCancelledEventHandlerTest**: 11개 통합 테스트
  - 이벤트 수신 시 CANCELLED 전환
  - 멱등성 테스트
- **PendingReservationTimeoutSchedulerTest**: 스케줄러 테스트
  - 만료된 PENDING 예약 자동 취소
  - ShedLock 분산 락 테스트

### API Layer (Task #74)
- **ReservationPricingControllerTest**: 40개 통합 테스트
  - POST /api/reservations/pricing: 예약 생성 성공/실패
  - PUT /api/reservations/{id}/confirm: 확정 성공/실패
  - PUT /api/reservations/{id}/cancel: 취소 성공/실패
  - POST /api/reservations/pricing/preview: 미리보기
  - PUT /api/reservations/{id}/products: 상품 업데이트

**총 테스트: 319개 케이스**
**테스트 커버리지: Domain 100%, Repository 95%, Application 90%, API 100%**

## PENDING 타임아웃 처리 (Task #89)

### 개요
결제 미완료 예약이 일정 시간 후 자동으로 취소되도록 하는 기능입니다.

### 정책
- **타임아웃 시간**: 기본 10분 (application.yml 설정)
- **체크 주기**: 매 1분마다 스케줄러 실행
- **대상**: status = PENDING && LocalDateTime.now() > expiresAt

### 구현
```java
@Scheduled(cron = "${reservation.pending.scheduler.cron}")
@SchedulerLock(
    name = "PendingReservationTimeoutScheduler",
    lockAtMostFor = "9m",
    lockAtLeastFor = "30s"
)
public void cancelExpiredPendingReservations() {
  // 만료된 PENDING 예약 자동 취소
}
```

### ShedLock 분산 락
- 여러 인스턴스 환경에서도 중복 실행 방지
- JDBC 기반 락 (shedlock 테이블 사용)
- lockAtMostFor: 9분 (최대 락 유지 시간)
- lockAtLeastFor: 30초 (최소 락 유지 시간)

### 설정
```yaml
reservation:
  pending:
    timeout-minutes: 10
    scheduler:
      cron: "0 */1 * * * *"  # 매 1분마다
```

## 이벤트 처리 (Task #88, Issue #164)

### ReservationConfirmed 이벤트
- **발행자**: Payment 서비스
- **Topic**: reservation-events
- **처리**: PENDING → CONFIRMED 상태 전환
- **비즈니스 로직**: 상품 재고 하드 락 (결제 확정)

### ReservationCancelled 이벤트
- **발행자**: Reservation 서비스 또는 사용자 취소
- **Topic**: reservation-events
- **처리**: PENDING/CONFIRMED → CANCELLED 상태 전환
- **비즈니스 로직**: 상품 재고 해제

### ReservationRefund 이벤트 (Issue #164)
- **발행자**: Payment 서비스
- **Topic**: reservation-events
- **처리**: CONFIRMED → CANCELLED 상태 전환
- **비즈니스 로직**:
  - 예약 상태를 CANCELLED로 변경
  - 예약에 포함된 모든 상품 재고 해제 (ProductRepository.releaseQuantity)
  - RESERVATION Scope 상품: totalQuantity 복원
  - ROOM/PLACE Scope 상품: product_time_slot_inventory 테이블에서 재고 복원

### 멱등성 보장
- 동일한 reservationId에 대해 중복 이벤트 처리 시 예외 발생하지 않음
- 상태가 이미 변경된 경우 무시
- 이벤트 재처리 시 안전성 보장

---

## 구현 완료 사항
- Task #74: ReservationPricing Aggregate 및 가격 계산 로직 구현 (완료)
- Task #88: Payment Event Handlers 구현 (완료)
- Task #89: PENDING timeout 처리 구현 (완료)
- Story #4: 예약 가격 계산 및 저장 (완료)
- Issue #157: 재고 예약/해제 로직 구현 (완료)
  - ProductRepository.reserveQuantity() 구현
  - ProductRepository.releaseQuantity() 구현
  - Atomic UPDATE 방식으로 동시성 제어
- Issue #164: 예약 환불 이벤트 처리 유즈케이스 구현 (완료)
  - ReservationRefundEventHandler 구현
  - CONFIRMED → CANCELLED 상태 전환
  - 상품 재고 자동 해제

## 향후 계획
- 예약 히스토리 조회 API
- 통계 및 리포트 기능
- 캐싱 전략 (Redis)

---

**Last Updated**: 2025-11-15
