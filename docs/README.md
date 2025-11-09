# 예약 가격 관리 서비스 문서

## 시작하기

이 프로젝트의 모든 문서는 체계적으로 구조화되어 있습니다.
**[INDEX.md](INDEX.md)** 에서 전체 문서 인덱스와 읽기 가이드를 확인하세요.

---

## 문서 구조

```
docs/
├── README.md                          (이 파일 - 문서 시작점)
├── INDEX.md                           (전체 문서 인덱스)
├── INFO.md                            (프로젝트 개요 및 GitHub 자동화)
├── ISSUE_GUIDE.md                     (이슈 작성 가이드)
├── PROJECT_SETUP.md                   (AI 어시스턴트 운영 가이드)
│
├── requirements/                      (요구사항 분석)
│   ├── README.md
│   └── PROJECT_REQUIREMENTS.md        (비즈니스 요구사항 명세)
│
├── architecture/                      (아키텍처 설계)
│   ├── README.md
│   ├── ARCHITECTURE_ANALYSIS.md       (아키텍처 패턴 비교)
│   ├── DOMAIN_MODEL_DESIGN.md         (도메인 모델 설계)
│   ├── TECH_STACK_ANALYSIS.md         (기술 스택 분석)
│   └── HEXAGONAL_ARCHITECTURE.md      (헥사고날 아키텍처 구현)
│
├── adr/                               (Architecture Decision Records)
│   ├── README.md
│   └── ADR_001_ARCHITECTURE_DECISION.md (최종 아키텍처 결정)
│
└── features/                          (기능별 상세 문서) 
    ├── pricing-policy/                (가격 정책 기능)
    │   ├── README.md                  (기능 개요)
    │   ├── domain.md                  (도메인 모델)
    │   ├── flow.md                    (플로우 및 시퀀스)
    │   ├── database.md                (DB 스키마)
    │   └── API.md                     (REST API 명세)
    │
    └── event-handling/                (이벤트 처리)
        ├── README.md                  (개요)
        ├── architecture.md            (아키텍처)
        └── events.md                  (이벤트 스키마)
```

---

## 빠른 시작 (Quick Start)

### 신규 팀원 온보딩 (4.5시간)

#### 1단계: 프로젝트 이해 (30분)
1. **[INFO.md](INFO.md)** - 프로젝트 개요
2. **[requirements/PROJECT_REQUIREMENTS.md](requirements/PROJECT_REQUIREMENTS.md)** - 요구사항

#### 2단계: 아키텍처 이해 (1시간)
3. **[adr/ADR_001_ARCHITECTURE_DECISION.md](adr/ADR_001_ARCHITECTURE_DECISION.md)** - 핵심 결정 사항
4. **[architecture/ARCHITECTURE_ANALYSIS.md](architecture/ARCHITECTURE_ANALYSIS.md)** - 선정 과정

#### 3단계: 상세 설계 (1시간)
5. **[architecture/DOMAIN_MODEL_DESIGN.md](architecture/DOMAIN_MODEL_DESIGN.md)** - 도메인 모델
6. **[architecture/TECH_STACK_ANALYSIS.md](architecture/TECH_STACK_ANALYSIS.md)** - 기술 스택

#### 4단계: 구현된 기능 이해 (1시간)
7. **[features/pricing-policy/README.md](features/pricing-policy/README.md)** - 가격 정책 기능
8. **[features/pricing-policy/API.md](features/pricing-policy/API.md)** - REST API 사용법

#### 5단계: 프로젝트 관리 (30분)
9. **[ISSUE_GUIDE.md](ISSUE_GUIDE.md)** - 이슈 작성법
10. **[PROJECT_SETUP.md](PROJECT_SETUP.md)** - 워크플로우 가이드

---

## 역할별 필독 문서

### 백엔드 개발자
-  [requirements/PROJECT_REQUIREMENTS.md](requirements/PROJECT_REQUIREMENTS.md)
-  [adr/ADR_001_ARCHITECTURE_DECISION.md](adr/ADR_001_ARCHITECTURE_DECISION.md)
-  [architecture/ARCHITECTURE_ANALYSIS.md](architecture/ARCHITECTURE_ANALYSIS.md)
-  [architecture/DOMAIN_MODEL_DESIGN.md](architecture/DOMAIN_MODEL_DESIGN.md)
-  [architecture/TECH_STACK_ANALYSIS.md](architecture/TECH_STACK_ANALYSIS.md)
-  [features/pricing-policy/](features/pricing-policy/) - 구현된 기능 참고
-  [features/product/](features/product/) - 구현된 기능 참고

### 프로젝트 매니저
-  [INFO.md](INFO.md)
-  [requirements/PROJECT_REQUIREMENTS.md](requirements/PROJECT_REQUIREMENTS.md)
-  [ISSUE_GUIDE.md](ISSUE_GUIDE.md)

### QA 엔지니어
-  [requirements/PROJECT_REQUIREMENTS.md](requirements/PROJECT_REQUIREMENTS.md)
-  [architecture/DOMAIN_MODEL_DESIGN.md](architecture/DOMAIN_MODEL_DESIGN.md)

### DevOps 엔지니어
-  [architecture/TECH_STACK_ANALYSIS.md](architecture/TECH_STACK_ANALYSIS.md)
-  [adr/ADR_001_ARCHITECTURE_DECISION.md](adr/ADR_001_ARCHITECTURE_DECISION.md)

---

## 핵심 정보 요약

### 프로젝트 정보
- **이름**: 예약 가격 관리 서비스 (Reservation Pricing Service)
- **목적**: MSA 환경에서 시간대별 가격 정책 및 예약 총 가격 계산
- **아키텍처**: Hexagonal Architecture + DDD

### 기술 스택
- **언어/프레임워크**: Java 21 + Spring Boot 3.2
- **데이터베이스**: PostgreSQL 16
- **메시징**: Apache Kafka 3.6
- **빌드**: Gradle 8.5 (Kotlin DSL)

### 핵심 도메인 모델
- **PricingPolicy** (시간대별 가격 정책) 구현 완료
- **Product** (추가상품)
- **ReservationPricing** (예약 가격 스냅샷)

### 구현 완료된 기능
-  PricingPolicy Aggregate (Issue #7)
-  PricingPolicy Repository (Issue #8)
-  RoomCreatedEvent 처리 (Issue #9)
-  가격 정책 관리 REST API (Issue #10)

---

## 문서 기여 가이드

### 새로운 문서 추가 시

1. **요구사항 문서**: `requirements/` 폴더
2. **아키텍처 문서**: `architecture/` 폴더
3. **결정 사항 문서**: `adr/` 폴더 (ADR 형식 준수)

### 기존 문서 수정 시

1. 해당 문서 직접 수정
2. `INDEX.md` 업데이트 (필요 시)
3. 관련 README.md 업데이트 (필요 시)

---

## 추가 리소스

### 외부 참고 자료
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/) by Eric Evans
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) by Alistair Cockburn
- [ADR GitHub Organization](https://adr.github.io/)

### 프로젝트 관련
- GitHub Issues 탭
- Pull Requests 탭
- GitHub Actions 워크플로우

---

**문서에 대한 질문이나 개선 사항은 GitHub Issues로 등록해주세요.**

**Last Updated:** 2025-11-09
