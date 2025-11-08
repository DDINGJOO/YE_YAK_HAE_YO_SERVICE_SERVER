# Architecture Decision Records (ADR)

이 폴더는 프로젝트의 주요 아키텍처 결정 사항을 기록한 문서들을 담고 있습니다.

## ADR이란?

Architecture Decision Record (ADR)는 소프트웨어 아키텍처에서 중요한 결정을 문서화하는 방법입니다.

**ADR의 목적:**
- 왜 특정 기술이나 패턴을 선택했는지 기록
- 다른 대안들과 비교한 근거 제시
- 미래의 팀원들이 결정 배경을 이해하도록 지원
- 결정에 따른 영향(Consequences) 명시

## ADR 목록

### ADR-001: 예약 가격 관리 서비스 아키텍처 결정
- **Status**: ACCEPTED
- **Date**: 2025-11-08
- **주요 결정**:
  - Hexagonal Architecture + DDD 채택
  - Java 21, Spring Boot 3.2, PostgreSQL 16
  - 3개 Aggregate Root 설계
  - Value Object 중심 도메인 모델

**상세 내용:**
- Context (배경)
- Decision (결정 사항)
- Rationale (선정 근거)
- Consequences (긍정/부정 영향)
- Trade-offs (득실 분석)
- Alternatives Considered (거부한 대안들)
- Implementation Plan (구현 계획)

## ADR 작성 규칙

### 파일명 형식
```
ADR_{번호}_{주제}.md
```

예시:
- `ADR_001_ARCHITECTURE_DECISION.md`
- `ADR_002_CONCURRENCY_CONTROL.md`
- `ADR_003_PRICE_HISTORY_MANAGEMENT.md`

### 문서 구조

```markdown
# ADR {번호}: {제목}

## Status
PROPOSED | ACCEPTED | DEPRECATED | SUPERSEDED

## Context
왜 이 결정이 필요한가?

## Decision
무엇을 결정했는가?

## Rationale
왜 이렇게 결정했는가?

## Consequences
이 결정의 영향은?

### Positive
- 긍정적 영향

### Negative
- 부정적 영향

## Alternatives Considered
고려했지만 선택하지 않은 대안들

## References
참고 자료
```

## 향후 ADR 예정

### ADR-002: 재고 관리 동시성 제어 전략
- Optimistic Locking vs Pessimistic Locking
- 동시 예약 시나리오 처리 방안

### ADR-003: 가격 정책 변경 이력 관리
- Event Sourcing 도입 여부
- Temporal Table vs Application Level 이력 관리

### ADR-004: 이벤트 중복 처리 멱등성 보장
- Idempotency Key 전략
- Kafka Offset 관리 방안

### ADR-005: 캐싱 전략
- Redis 도입 여부
- 캐시 키 설계 및 TTL 정책

## ADR 변경 프로세스

1. **새로운 결정 필요 시**
   - 새로운 ADR 작성 (PROPOSED 상태)
   - 팀 리뷰 및 토론

2. **결정 승인 시**
   - Status를 ACCEPTED로 변경
   - 날짜 기록

3. **기존 결정 변경 시**
   - 기존 ADR을 DEPRECATED 또는 SUPERSEDED로 변경
   - 새로운 ADR 작성 및 링크

## 참고 자료

- [ADR GitHub Organization](https://adr.github.io/)
- [Documenting Architecture Decisions by Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
