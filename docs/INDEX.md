# 예약 가격 관리 서비스 문서 인덱스

## 문서 구조

이 프로젝트의 문서는 다음과 같이 구조화되어 있습니다:

```
docs/
├── INDEX.md                          (이 파일)
├── INFO.md                           (프로젝트 자동화 시스템 개요)
├── ISSUE_GUIDE.md                    (이슈 작성 가이드)
├── PROJECT_SETUP.md                  (프로젝트 설정 및 워크플로우 가이드)
│
├── requirements/                     (요구사항 분석)
│   ├── README.md
│   └── PROJECT_REQUIREMENTS.md       (전체 요구사항 명세)
│
├── architecture/                     (아키텍처 설계)
│   ├── README.md
│   ├── ARCHITECTURE_ANALYSIS.md      (아키텍처 패턴 비교)
│   ├── DOMAIN_MODEL_DESIGN.md        (도메인 모델 설계)
│   └── TECH_STACK_ANALYSIS.md        (기술 스택 분석)
│
├── adr/                              (Architecture Decision Records)
│   ├── README.md
│   └── ADR_001_ARCHITECTURE_DECISION.md (최종 아키텍처 결정)
│
├── development/                      (개발 가이드)
│   └── performance/                  (성능 관련)
│       ├── optimization.md           (성능 최적화 전략)
│       └── testing-guide.md          (성능 테스트 가이드)
│
└── features/                         (기능별 상세 문서)
    ├── pricing-policy/               (가격 정책 기능)
    │   ├── README.md                 (개요)
    │   ├── domain.md                 (도메인 모델)
    │   ├── flow.md                   (플로우 및 시퀀스)
    │   ├── database.md               (DB 스키마)
    │   └── API.md                    (REST API 명세)
    │
    ├── product/                      (추가상품 기능)
    │   ├── README.md                 (개요)
    │   ├── domain.md                 (도메인 모델)
    │   ├── database.md               (DB 스키마)
    │   └── API.md                    (REST API 명세)
    │
    ├── reservation-pricing/          (예약 가격 기능)
    │   ├── README.md                 (개요)
    │   ├── domain.md                 (도메인 모델)
    │   ├── database.md               (DB 스키마)
    │   └── API.md                    (REST API 명세)
    │
    ├── reservation/                  (예약 기능)
    │   └── RESERVATION_FLOW.md       (예약 플로우 전체 시나리오)
    │
    └── event-handling/               (이벤트 처리 기능)
        ├── README.md                 (개요)
        ├── architecture.md           (아키텍처 상세)
        └── events.md                 (이벤트 타입 및 스키마)
```

---

##  새로 추가: 기능별 상세 문서 (Features)

**위치:** `features/`

**목적:** 완료된 기능의 상세한 구현 내용, 플로우, 설계 결정을 문서화합니다.

**현재 문서:**
- **pricing-policy/**: 가격 정책 기능 (Issue #7, #8, #9, #10)
  - [README.md](features/pricing-policy/README.md) - 기능 개요 및 완료된 이슈
  - [domain.md](features/pricing-policy/domain.md) - 도메인 모델 상세 (Aggregate, Value Objects, Use Cases)
  - [flow.md](features/pricing-policy/flow.md) - 플로우 및 시퀀스 다이어그램 (생성, 계산, 수정, 복사)
  - [database.md](features/pricing-policy/database.md) - DB 스키마 및 JPA 매핑
  - [API.md](features/pricing-policy/API.md) - REST API 명세 및 사용 예시

- **product/**: 추가상품 기능 (Issue #11, #13, #14)
  - [README.md](features/product/README.md) - 기능 개요 및 완료된 이슈
  - [domain.md](features/product/domain.md) - 도메인 모델 상세 (Aggregate, Value Objects, PricingStrategy)
  - [database.md](features/product/database.md) - DB 스키마 및 JPA 매핑
  - [API.md](features/product/API.md) - REST API 명세 및 사용 예시

- **reservation-pricing/**: 예약 가격 기능 (Task #74, #88, #89)
  - [README.md](features/reservation-pricing/README.md) - 기능 개요 및 완료된 태스크
  - [domain.md](features/reservation-pricing/domain.md) - 도메인 모델 상세 (ReservationPricing Aggregate, 상태 전이, 재고 관리)
  - [database.md](features/reservation-pricing/database.md) - DB 스키마, JPA 매핑, 인덱스 전략
  - [API.md](features/reservation-pricing/API.md) - REST API 명세 및 사용 예시 (예약 생성/확정/취소/미리보기/상품 업데이트)

- **reservation/**: 예약 기능 (Issue #68)
  - [RESERVATION_FLOW.md](features/reservation/RESERVATION_FLOW.md) - 예약 플로우 전체 시나리오 (7단계)
    - 슬롯 예약 → 시간대 가격 계산 → 재고 조회 → 상품 추가 및 재고 락 → 결제 → 예약 확정 → 취소
    - 상태 전이 다이어그램 (PENDING → CONFIRMED → CANCELLED)
    - 재고 관리 메커니즘 (소프트 락, 하드 락, 해제)
    - 실패 시나리오 및 보상 트랜잭션

- **event-handling/**: 이벤트 처리 기능 (Issue #9)
  - [README.md](features/event-handling/README.md) - 기능 개요 및 아키텍처 원칙
  - [architecture.md](features/event-handling/architecture.md) - 계층별 상세 설계
  - [events.md](features/event-handling/events.md) - 이벤트 타입 및 스키마

**언제 읽나요?**
- 특정 기능 구현 방법 이해 필요 시
- 유사한 기능 개발 시 참고용
- 코드 리뷰 시
- 버그 분석 시
- 전체 예약 플로우 이해 필요 시

---

## 문서 읽기 가이드

### 신규 팀원을 위한 읽기 순서

프로젝트를 처음 접하는 팀원은 다음 순서로 문서를 읽는 것을 권장합니다:

#### 1단계: 프로젝트 이해 (30분)
1. **INFO.md** - 프로젝트 자동화 시스템 개요
2. **requirements/PROJECT_REQUIREMENTS.md** - 비즈니스 요구사항

#### 2단계: 아키텍처 이해 (1시간)
3. **adr/ADR_001_ARCHITECTURE_DECISION.md** - 최종 아키텍처 결정 (핵심)
4. **architecture/ARCHITECTURE_ANALYSIS.md** - 아키텍처 선정 과정

#### 3단계: 상세 설계 이해 (1시간)
5. **architecture/DOMAIN_MODEL_DESIGN.md** - 도메인 모델 설계
6. **architecture/TECH_STACK_ANALYSIS.md** - 기술 스택

#### 4단계: 구현된 기능 이해 (1시간)
7. **features/pricing-policy/README.md** - 가격 정책 기능 개요
8. **features/product/README.md** - 추가상품 기능 개요
9. **features/event-handling/README.md** - 이벤트 처리 아키텍처

#### 5단계: 프로젝트 관리 (30분)
10. **ISSUE_GUIDE.md** - 이슈 작성 방법
11. **PROJECT_SETUP.md** - 프로젝트 워크플로우 가이드

**총 소요 시간: 약 4시간**

---

### 역할별 필독 문서

#### 개발자 (Backend)
- requirements/PROJECT_REQUIREMENTS.md
- adr/ADR_001_ARCHITECTURE_DECISION.md
- architecture/DOMAIN_MODEL_DESIGN.md
- architecture/TECH_STACK_ANALYSIS.md
- features/pricing-policy/README.md
- features/product/README.md
- features/event-handling/architecture.md

#### 프로젝트 매니저
- INFO.md
- requirements/PROJECT_REQUIREMENTS.md
- ISSUE_GUIDE.md
- adr/ADR_001_ARCHITECTURE_DECISION.md (Context, Decision만)

#### QA 엔지니어
- requirements/PROJECT_REQUIREMENTS.md
- architecture/DOMAIN_MODEL_DESIGN.md (도메인 규칙)
- ISSUE_GUIDE.md

#### DevOps 엔지니어
- architecture/TECH_STACK_ANALYSIS.md
- adr/ADR_001_ARCHITECTURE_DECISION.md (기술 스택 부분)

#### 아키텍트
- 모든 문서

---

## 문서 카테고리별 안내

### 요구사항 분석 (Requirements)

**위치:** `requirements/`

**목적:** 프로젝트의 비즈니스 요구사항과 기능 명세를 정의합니다.

**주요 문서:**
- [PROJECT_REQUIREMENTS.md](requirements/PROJECT_REQUIREMENTS.md)
  - 3가지 핵심 기능 상세 명세
  - 도메인 규칙 (Invariants)
  - 비기능 요구사항
  - 성공 지표

**언제 읽나요?**
- 프로젝트 시작 전
- 새로운 기능 추가 시
- 테스트 케이스 작성 시

---

### 아키텍처 설계 (Architecture)

**위치:** `architecture/`

**목적:** 시스템 아키텍처 설계와 기술 스택 선정 과정을 문서화합니다.

**주요 문서:**

#### ARCHITECTURE_ANALYSIS.md
- Layered vs Hexagonal vs Clean Architecture 비교
- 각 패턴의 장단점 및 평가 점수
- 최종 선택: Hexagonal Architecture + DDD

#### DOMAIN_MODEL_DESIGN.md
- 3개 Aggregate 설계 대안 비교
  - PricingPolicy (시간대별 가격)
  - Product (추가상품)
  - ReservationPricing (예약 가격 스냅샷)
- Value Object vs Entity 선택 근거
- SOLID 원칙 검증

#### TECH_STACK_ANALYSIS.md
- Java 21, Spring Boot 3.2, PostgreSQL 16 선정 근거
- 성능 최적화 전략
- Docker Compose 구성
- CI/CD 파이프라인

**언제 읽나요?**
- 아키텍처 이해 필요 시
- 새로운 모듈 추가 시
- 기술 스택 변경 검토 시

---

### Architecture Decision Records (ADR)

**위치:** `adr/`

**목적:** 주요 아키텍처 결정 사항을 기록하고 근거를 남깁니다.

**주요 문서:**

#### ADR_001_ARCHITECTURE_DECISION.md
- **Status:** ACCEPTED
- **Date:** 2025-11-08
- **핵심 결정:**
  - Hexagonal Architecture + DDD
  - Java 21 + Spring Boot 3.2
  - PostgreSQL 16 + Kafka 3.6
- **구조:**
  - Context (왜 필요한가?)
  - Decision (무엇을 결정했는가?)
  - Rationale (왜 이렇게 결정했는가?)
  - Consequences (영향은?)
  - Trade-offs (득실은?)
  - Alternatives (거부한 대안은?)

**향후 예정 ADR:**
- ADR-002: 재고 관리 동시성 제어 전략
- ADR-003: 가격 정책 변경 이력 관리
- ADR-004: 이벤트 중복 처리 멱등성 보장

**언제 읽나요?**
- 아키텍처 결정 배경 이해 필요 시
- 유사한 결정 필요 시
- 온보딩 시

---

### 개발 가이드 (Development)

**위치:** `development/`

**목적:** 개발 과정에서 필요한 가이드 및 최적화 전략을 제공합니다.

**주요 문서:**

#### performance/optimization.md
- JPA N+1 문제 해결 전략
- 캐싱 전략 (Redis, Caffeine)
- DB 쿼리 최적화
- 인덱스 설계 및 최적화
- 성능 개선 사례

#### performance/testing-guide.md
- JMeter를 활용한 부하 테스트
- 성능 테스트 시나리오
- 모니터링 및 분석
- 성능 목표 및 검증

**언제 읽나요?**
- 성능 최적화 필요 시
- 부하 테스트 수행 시
- 성능 이슈 분석 시
- 프로덕션 배포 전 성능 검증 시

---

### 프로젝트 자동화 (Project Automation)

**위치:** `docs/` (최상위)

**목적:** GitHub Issues, PR, 라벨링 자동화 시스템 안내

**주요 문서:**

#### INFO.md
- 자동 라벨링 시스템
- 이슈 관리 (Epic/Story/Task/Spike/Change Request)
- PR 자동화
- 사용 가이드

#### ISSUE_GUIDE.md
- 이슈 타입별 작성 예시
- Epic → Story → Task 계층 구조
- 실전 시나리오

#### PROJECT_SETUP.md
- 프로젝트 설정 가이드
- 이슈/PR 관리 워크플로우
- GitHub CLI 활용법
- 커밋 메시지 컨벤션

**언제 읽나요?**
- 이슈 생성 시
- PR 생성 시
- 워크플로우 이해 필요 시

---

## 문서 업데이트 규칙

### 요구사항 변경 시
1. `requirements/PROJECT_REQUIREMENTS.md` 업데이트
2. 영향받는 아키텍처 문서 검토
3. 필요 시 새로운 ADR 작성

### 아키텍처 변경 시
1. 새로운 ADR 작성 (PROPOSED 상태)
2. 팀 리뷰 후 ACCEPTED
3. 관련 아키텍처 문서 업데이트

### 기술 스택 변경 시
1. `architecture/TECH_STACK_ANALYSIS.md` 업데이트
2. ADR 작성 (변경 근거)
3. README 및 설정 파일 업데이트

---

## 빠른 참조 (Quick Reference)

### 핵심 아키텍처 정보

| 항목 | 내용 | 참조 문서 |
|------|------|----------|
| 아키텍처 패턴 | Hexagonal + DDD | ADR-001 |
| 언어/프레임워크 | Java 21 + Spring Boot 3.2 | TECH_STACK_ANALYSIS |
| 데이터베이스 | PostgreSQL 16 | TECH_STACK_ANALYSIS |
| 메시징 | Apache Kafka 3.6 | TECH_STACK_ANALYSIS |
| Aggregate Root | PricingPolicy, Product, ReservationPricing | DOMAIN_MODEL_DESIGN |

### 핵심 도메인 규칙

| 규칙 | 내용 | 참조 문서 |
|------|------|----------|
| 시간대 중복 | 같은 요일 내 시간대 중복 불가 | PROJECT_REQUIREMENTS |
| 재고 관리 | PENDING/CONFIRMED만 재고 차감 | PROJECT_REQUIREMENTS |
| 가격 불변성 | 예약 가격은 생성 후 변경 불가 | DOMAIN_MODEL_DESIGN |

### 성능 목표

| 항목 | 목표 | 참조 문서 |
|------|------|----------|
| 예약 생성 응답 시간 | P95 500ms 이하 | PROJECT_REQUIREMENTS |
| 가격 계산 정확도 | 100% | PROJECT_REQUIREMENTS |
| 테스트 커버리지 | 80% 이상 | ADR-001 |

---

## 문서 검색 팁

### 특정 개념 찾기

#### "시간대별 가격"에 대해 알고 싶다면?
1. requirements/PROJECT_REQUIREMENTS.md (기능 1 - 요구사항)
2. architecture/DOMAIN_MODEL_DESIGN.md (PricingPolicy Aggregate 설계)
3. **features/pricing-policy/README.md** (기능 개요) 
4. **features/pricing-policy/domain.md** (도메인 모델 상세) 
5. **features/pricing-policy/flow.md** (실제 플로우) 
6. **features/pricing-policy/API.md** (REST API 사용법) 
7. adr/ADR_001_ARCHITECTURE_DECISION.md (설계 결정)

#### "재고 관리" 또는 "추가상품"에 대해 알고 싶다면?
1. requirements/PROJECT_REQUIREMENTS.md (기능 2 - 요구사항)
2. architecture/DOMAIN_MODEL_DESIGN.md (Product Aggregate 설계)
3. **features/product/README.md** (기능 개요)
4. **features/product/domain.md** (도메인 모델 상세)
5. **features/product/database.md** (DB 스키마 및 JPA 매핑)
6. adr/ADR_001_ARCHITECTURE_DECISION.md (설계 결정)

#### "예약 가격 계산" 또는 "예약 플로우"에 대해 알고 싶다면?
1. requirements/PROJECT_REQUIREMENTS.md (기능 3 - 3단계 예약 프로세스)
2. **features/reservation/RESERVATION_FLOW.md** (전체 예약 플로우 시나리오)
3. **features/reservation-pricing/README.md** (예약 가격 기능 개요)
4. **features/reservation-pricing/domain.md** (ReservationPricing Aggregate, 상태 전이, 재고 관리)
5. **features/reservation-pricing/database.md** (DB 스키마 및 인덱스 전략)
6. **features/reservation-pricing/API.md** (REST API 사용법)
7. architecture/DOMAIN_MODEL_DESIGN.md (ReservationPricing Aggregate 설계)
8. adr/ADR_001_ARCHITECTURE_DECISION.md (Value Object Snapshot)

---

## 추가 리소스

### 외부 참고 자료

**Books:**
- "Domain-Driven Design" by Eric Evans
- "Implementing Domain-Driven Design" by Vaughn Vernon
- "Clean Architecture" by Robert C. Martin

**Articles:**
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) by Alistair Cockburn
- [ADR GitHub Organization](https://adr.github.io/)

**Documentation:**
- [Spring Boot 3.2 Reference](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [PostgreSQL Range Types](https://www.postgresql.org/docs/16/rangetypes.html)

---

## 피드백 및 문의

문서 개선 사항이나 문의 사항은 GitHub Issues로 등록해주세요.

**라벨:**
- `docs`: 문서 관련
- `question`: 질문

---

---

## 최근 업데이트

### 2025-11-10
- **예약 가격 기능 문서 작성 완료** (Task #74, #88, #89)
  - docs/features/reservation-pricing/ 디렉토리 신규 생성
  - README.md: 기능 개요, 주요 컴포넌트, 상태 전이, PENDING 타임아웃, 이벤트 처리 (완료된 Task 체크)
  - domain.md: ReservationPricing Aggregate, Value Objects, 상태 전이 메서드, ProductAvailabilityService, 가격 계산 로직 상세 설계
  - database.md: ERD, JPA 매핑, 인덱스 전략 (ElementCollection, PlaceId 비정규화), Flyway 마이그레이션, 성능 최적화
  - API.md: REST API 5개 엔드포인트 명세 (예약 생성/확정/취소/미리보기/상품 업데이트), 사용 예시, 재고 관리, PENDING 타임아웃
  - docs/INDEX.md 업데이트: reservation-pricing 문서 추가

### 2025-11-09
- **문서 구조 정리 및 예약 플로우 문서화**
  - docs/development/performance/ 디렉토리 신규 생성
  - PERFORMANCE_OPTIMIZATION.md, PERFORMANCE_TESTING_GUIDE.md 이동 및 정리
  - docs/features/reservation/ 디렉토리 신규 생성
  - RESERVATION_FLOW.md: 예약 플로우 전체 시나리오 문서화 (370 lines)
    - 7단계 예약 프로세스 (슬롯 예약 → 시간대 가격 계산 → 재고 조회 → 상품 추가 및 재고 락 → 결제 → 예약 확정 → 취소)
    - 상태 전이 다이어그램 (PENDING → CONFIRMED → CANCELLED)
    - 재고 관리 메커니즘 (소프트 락, 하드 락, 해제)
    - 실패 시나리오 및 보상 트랜잭션
  - PROJECT_REQUIREMENTS.md 업데이트
    - 기능 3을 3단계 예약 프로세스로 수정
    - 외부 의존성 섹션에 모든 이벤트 명시
    - 구현 상태 표시 추가
    - 관련 문서 링크 추가

- **SlotReservedEvent 핸들러 구현 완료** (Issue #68)
  - SlotReservedEvent 이벤트 처리
  - 시간대 가격 자동 계산 및 PENDING 상태 ReservationPricing 생성
  - 멱등성 보장 (중복 처리 방지)
  - 통합 테스트 완료

- **추가상품 기능 완료** (Issue #11, #13, #14)
  - Issue #11: Product Aggregate 구현 (도메인 모델, 테스트 67개) 
  - Issue #13: Product Repository 구현 (영속성 계층, 통합 테스트 13개) 
  - Issue #14: Product API 구현 (Application Layer, Controller, 통합 테스트 16개) 
  
- **추가상품 기능 문서 작성 완료**
  - README.md: 기능 개요, Scope 및 PricingType 설명, 가격 계산 예시, 완료 이슈 체크
  - domain.md: Product Aggregate, PricingStrategy, ProductPriceBreakdown 상세 설계
  - database.md: ERD, JPA 매핑, Flyway 마이그레이션, 성능 최적화 전략
  - API.md: REST API 5개 엔드포인트 명세 및 사용 예시 (652 lines)

- **가격 정책 API 문서 추가 및 보완** (Issue #10)
  - API.md: REST API 4개 엔드포인트 명세 및 사용 예시
  - README.md: API Layer 섹션 추가, 완료 이슈 체크
  - domain.md: 복사 기능, Use Cases, 테스트 커버리지 업데이트
  - flow.md: 가격 수정/복사 플로우 시퀀스 다이어그램 추가

### 2025-11-08
- **features/** 폴더 신규 추가
- 가격 정책 기능 문서 작성 완료 (Issue #7, #8, #9)
  - domain.md: 도메인 모델 상세 (Aggregate, Value Objects, 비즈니스 규칙)
  - flow.md: 가격 정책 자동 생성 및 가격 계산 플로우
  - database.md: ERD, JPA 매핑, Flyway 마이그레이션
- 이벤트 처리 아키텍처 문서 작성 완료
  - architecture.md: 계층별 설계, 디자인 패턴, 확장 전략
  - events.md: RoomCreatedEvent 스키마 및 처리 흐름

**Last Updated:** 2025-11-10
