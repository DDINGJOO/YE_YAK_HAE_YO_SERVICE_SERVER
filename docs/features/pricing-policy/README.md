# PricingPolicy 기능

## 개요
예약 가능한 룸(Room)의 가격 정책을 관리하는 기능입니다. 시간 단위별 기본 가격과 요일/시간대별 차등 가격을 지원합니다.

## 목적
- 룸별로 독립적인 가격 정책 관리
- 시간 단위(시간당/30분당) 선택 가능
- 요일 및 시간대별 차등 가격 적용
- 예약 기간에 대한 정확한 가격 계산

## 주요 컴포넌트

### Domain Layer (Issue #7)
- **PricingPolicy**: Aggregate Root
  - RoomId를 Aggregate ID로 사용
  - 기본 가격 및 시간대별 가격 관리
  - 예약 기간에 대한 가격 계산 로직
- **TimeRangePrices**: Value Object
  - 시간대별 가격 컬렉션 관리
  - 중복 시간대 검증
- **TimeRangePrice**: Record (불변 객체)
  - 요일, 시간대, 가격 정보

### Repository Layer (Issue #8)
- **PricingPolicyRepository**: Port (출력 포트)
- **PricingPolicyRepositoryAdapter**: JPA Adapter
- **PricingPolicyEntity**: JPA Entity
  - @EmbeddedId, @Embedded, @ElementCollection 사용
- **Flyway Migration**: V2__create_pricing_policy_tables.sql

### Event Handling Layer (Issue #9)
- **CreatePricingPolicyUseCase**: 가격 정책 생성 Use Case
- **CreatePricingPolicyService**: Use Case 구현체
- **RoomCreatedEventHandler**: RoomCreatedEvent 처리 핸들러

## 관련 문서
- [도메인 모델](./domain.md)
- [플로우 및 시퀀스](./flow.md)
- [데이터베이스 스키마](../../implementation/DATABASE_SCHEMA.md) - 전체 DB 스키마 (pricing_policies 테이블)

### API Layer (Issue #10)
- **PricingPolicyController**: REST API 컨트롤러
  - GET /api/pricing-policies/{roomId}: 가격 정책 조회
  - PUT /api/pricing-policies/{roomId}/default-price: 기본 가격 업데이트
  - PUT /api/pricing-policies/{roomId}/time-range-prices: 시간대별 가격 업데이트
  - POST /api/pricing-policies/{targetRoomId}/copy: 가격 정책 복사
- **UpdatePricingPolicyUseCase**: 가격 업데이트 Use Case
- **GetPricingPolicyUseCase**: 가격 조회 Use Case
- **CopyPricingPolicyUseCase**: 가격 정책 복사 Use Case

## 관련 Issues
- Issue #7: PricingPolicy Aggregate 구현 
- Issue #8: PricingPolicy Repository 구현 
- Issue #9: RoomCreatedEvent 리스너 및 가격 정책 자동 생성 
- Issue #10: 가격 정책 관리 API 구현 

---

**Last Updated**: 2025-11-15
