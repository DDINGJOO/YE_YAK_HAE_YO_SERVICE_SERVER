# 아키텍처 설계 (Architecture)

이 폴더는 시스템 아키텍처 설계와 기술 스택 선정 과정을 담고 있습니다.

## 문서 목록

### ARCHITECTURE_ANALYSIS.md
다양한 아키텍처 패턴을 비교 분석한 문서입니다.

**주요 내용:**
- Layered Architecture vs Hexagonal Architecture vs Clean Architecture
- 각 패턴별 장단점 및 평가 점수
- DDD 핵심 개념 적용 방안
- 비교 매트릭스 및 가중치 분석
- 최종 아키텍처 선정: Hexagonal Architecture + DDD
- 레이어 구조 및 패키지 구조

**대상 독자:**
- 아키텍트
- 시니어 개발자
- 기술 리더

### DOMAIN_MODEL_DESIGN.md
도메인 모델 설계 대안을 비교한 문서입니다.

**주요 내용:**
- 3가지 Aggregate별 설계 대안 비교
  - PricingPolicy (시간대별 가격 정책)
  - Product (추가상품)
  - ReservationPricing (예약 가격 스냅샷)
- 각 대안의 장단점 및 평가 점수
- Value Object vs Entity 선택 근거
- Domain Service 사용 시점
- SOLID 원칙 준수 검증
- 최종 도메인 모델 구조

**대상 독자:**
- 도메인 전문가
- DDD 설계자
- 백엔드 개발자

### TECH_STACK_ANALYSIS.md
기술 스택 분석 및 선정 문서입니다.

**주요 내용:**
- 레이어별 기술 스택 비교
  - Java 21 vs Java 17
  - PostgreSQL vs MySQL
  - Kafka vs RabbitMQ
  - Gradle vs Maven
- 각 기술 선정 근거 및 장단점
- 성능 최적화 전략
- 개발 환경 구성 (Docker Compose)
- CI/CD 파이프라인 (GitHub Actions)
- 코드 품질 도구 설정

**대상 독자:**
- 기술 리더
- DevOps 엔지니어
- 인프라 담당자

## 읽기 순서

1. **ARCHITECTURE_ANALYSIS.md** - 전체 아키텍처 이해
2. **DOMAIN_MODEL_DESIGN.md** - 도메인 모델 상세 설계
3. **TECH_STACK_ANALYSIS.md** - 기술 스택 및 구현 방안

## 다음 단계

아키텍처 설계를 확정한 후 [ADR (Architecture Decision Records)](../adr/)를 참고하여 최종 결정 사항을 확인하세요.
