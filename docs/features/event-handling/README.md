# Event Handling 기능

## 개요
Kafka를 통한 이벤트 기반 아키텍처를 구현한 기능입니다. 객체지향 설계 원칙을 적용하여 확장 가능하고 유지보수가 용이한 이벤트 처리 구조를 제공합니다.

## 목적
- MSA 환경에서 서비스 간 느슨한 결합
- 이벤트 기반 비동기 처리
- 확장 가능한 이벤트 핸들러 구조
- 타입 안정성을 갖춘 이벤트 라우팅

## 아키텍처 원칙

### 1. Open-Closed Principle (OCP)
- 새로운 이벤트 타입 추가 시 기존 코드 수정 불필요
- Event 클래스 상속으로 확장
- EventHandler 인터페이스 구현으로 핸들러 추가

### 2. Single Responsibility Principle (SRP)
- **Event**: 이벤트 데이터 구조만 담당
- **EventHandler**: 특정 이벤트 타입 처리만 담당
- **EventConsumer**: 메시지 수신 및 라우팅만 담당

### 3. Dependency Inversion Principle (DIP)
- EventHandler는 인터페이스로 추상화
- EventConsumer는 구체 클래스가 아닌 인터페이스에 의존

## 주요 컴포넌트

### Event Layer
- **Event**: 추상 클래스, 모든 이벤트의 기반
- **RoomCreatedEvent**: 룸 생성 이벤트
- **RoomUpdatedEvent**: 룸 정보 업데이트 이벤트
- **SlotReservedEvent**: 예약 생성 이벤트
- **ReservationConfirmedEvent**: 예약 확정 이벤트 (결제 완료)
- **ReservationCancelledEvent**: 예약 취소 이벤트
- **ReservationRefundEvent**: 예약 환불 이벤트 (Issue #164)

### Handler Layer
- **EventHandler<T>**: Generic 인터페이스
- **RoomCreatedEventHandler**: 가격 정책 자동 생성
- **RoomUpdatedEventHandler**: 가격 정책 정보 업데이트
- **SlotReservedEventHandler**: 예약 가격 계산 및 재고 소프트 락
- **ReservationConfirmedEventHandler**: 예약 상태 CONFIRMED 전환 (Task #88)
- **ReservationCancelledEventHandler**: 예약 상태 CANCELLED 전환 (Task #88)
- **ReservationRefundEventHandler**: 예약 환불 처리 및 재고 해제 (Issue #164)

### Consumer Layer
- **EventConsumer**: Kafka Consumer 및 라우터

## 이벤트 플로우

```
Producer (Place Service)
    │
    │ publish
    ▼
Kafka Topic (room-events)
    │
    │ consume
    ▼
EventConsumer
    │
    │ deserialize & route
    ▼
EventHandler<RoomCreatedEvent>
    │
    │ handle
    ▼
Application Service (CreatePricingPolicyService)
    │
    │ business logic
    ▼
Repository (PricingPolicyRepository)
```

## 관련 문서
- [아키텍처 상세](./architecture.md)
- [이벤트 타입 및 스키마](./events.md)
- [통신 규약](./protocol.md)

## 관련 Issues
- Issue #9: RoomCreatedEvent 리스너 및 가격 정책 자동 생성 (완료)
- Task #88: Payment Event Handlers 구현 (완료)
  - ReservationConfirmedEventHandler
  - ReservationCancelledEventHandler
- Issue #157: 재고 예약/해제 로직 구현 (완료)
- Issue #164: 예약 환불 이벤트 처리 유즈케이스 구현 (완료)
  - ReservationRefundEventHandler 추가
  - 상품 재고 자동 해제 로직

## 확장 가능성
새로운 이벤트 추가 시 필요한 작업:
1. Event 클래스 상속하여 구현
2. EventHandler 구현체 작성
3. EVENT_TYPE_MAP에 매핑 추가
4. 테스트 작성

기존 코드 수정 불필요!

---

**Last Updated**: 2025-11-12
