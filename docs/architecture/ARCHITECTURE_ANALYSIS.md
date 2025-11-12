# 아키텍처 패턴 비교 분석

## 개요

본 문서는 예약 가격 관리 서비스의 아키텍처 패턴을 선정하기 위한 비교 분석을 담고 있습니다.
요구사항 분석 결과를 바탕으로 3가지 주요 아키텍처 패턴을 평가하고 최적의 선택을 도출합니다.

---

## 아키텍처 패턴 후보

### 1. Layered Architecture (계층형 아키텍처)

#### 구조
```
┌─────────────────────────────────┐
│   Presentation Layer            │ ← REST API, Event Listener
├─────────────────────────────────┤
│   Application Layer             │ ← Service, UseCase
├─────────────────────────────────┤
│   Domain Layer                  │ ← Entity, Value Object
├─────────────────────────────────┤
│   Infrastructure Layer          │ ← Repository, Message Queue
└─────────────────────────────────┘
```

#### 특징
- 전통적이고 직관적인 구조
- 상위 레이어가 하위 레이어에 의존
- Database-Driven Design에 적합

#### 장점
1. **학습 곡선 낮음**: 대부분의 개발자가 익숙
2. **구현 속도 빠름**: 빠른 프로토타이핑 가능
3. **명확한 계층 분리**: 각 레이어의 역할이 명확
4. **풍부한 레퍼런스**: Spring Boot 기본 구조

#### 단점
1. **도메인 로직 유출**: Service 레이어에 비즈니스 로직 집중
2. **의존성 방향 문제**: Domain이 Infrastructure에 의존 가능
3. **테스트 어려움**: DB 의존성으로 인한 통합 테스트 필요
4. **확장성 제한**: 새로운 요구사항 추가 시 여러 레이어 수정

#### 프로젝트 적합성 평가

| 평가 항목 | 점수 | 근거 |
|---------|------|------|
| 복잡한 도메인 규칙 처리 | 2/5 | 도메인 로직이 Service에 분산될 위험 |
| 가격 계산 정확성 | 3/5 | 비즈니스 로직 캡슐화 약함 |
| 새로운 가격 정책 추가 | 2/5 | OCP 위반 가능성 높음 |
| 재고 관리 복잡도 | 2/5 | 상태 관리 로직이 분산됨 |
| 테스트 용이성 | 2/5 | Infrastructure 의존성 |
| 개발 속도 | 5/5 | 빠른 초기 개발 |

**총점: 16/30**

---

### 2. Hexagonal Architecture (육각형 아키텍처 / Ports & Adapters)

#### 구조
```
         ┌──────────────────────────────┐
         │   Adapters (Driving)         │
         │  ┌──────────┐  ┌──────────┐  │
         │  │REST API  │  │Event     │  │
         │  │Controller│  │Listener  │  │
         │  └────┬─────┘  └────┬─────┘  │
         └───────┼─────────────┼────────┘
                 ▼             ▼
         ┌──────────────────────────────┐
         │      Ports (Driving)         │
         │  ┌────────────────────────┐  │
         │  │  Application Service   │  │
         │  └───────────┬────────────┘  │
         │              ▼                │
         │  ┌────────────────────────┐  │
         │  │    Domain Model        │  │
         │  │  ┌──────────────────┐  │  │
         │  │  │ Aggregate Root   │  │  │
         │  │  │ Entity           │  │  │
         │  │  │ Value Object     │  │  │
         │  │  │ Domain Service   │  │  │
         │  │  └──────────────────┘  │  │
         │  └───────────┬────────────┘  │
         │              ▼                │
         │  ┌────────────────────────┐  │
         │  │  Ports (Driven)        │  │
         │  │  - RepositoryPort      │  │
         │  │  - EventPublisherPort  │  │
         │  └───────────┬────────────┘  │
         └──────────────┼───────────────┘
                        ▼
         ┌──────────────────────────────┐
         │   Adapters (Driven)          │
         │  ┌──────────┐  ┌──────────┐  │
         │  │JPA Repo  │  │Kafka     │  │
         │  │Adapter   │  │Adapter   │  │
         │  └──────────┘  └──────────┘  │
         └──────────────────────────────┘
```

#### 특징
- 도메인이 중심 (Domain-Centric)
- 의존성 역전 원칙 적용
- 외부 기술로부터 도메인 격리

#### 장점
1. **도메인 중심 설계**: 비즈니스 로직이 순수하게 유지
2. **의존성 방향 명확**: 모든 의존성이 도메인을 향함
3. **테스트 용이**: 도메인 로직 단위 테스트 가능
4. **기술 독립성**: DB, 메시지 큐 교체 용이
5. **OCP 준수**: 새로운 Adapter 추가 시 기존 코드 수정 불필요

#### 단점
1. **초기 설계 복잡**: Port/Adapter 인터페이스 정의 필요
2. **코드량 증가**: 인터페이스와 구현체 분리로 파일 증가
3. **학습 곡선**: 팀원의 이해도 필요
4. **과도한 추상화 위험**: 단순한 CRUD도 복잡해질 수 있음

#### 프로젝트 적합성 평가

| 평가 항목 | 점수 | 근거 |
|---------|------|------|
| 복잡한 도메인 규칙 처리 | 5/5 | Aggregate로 불변식 보장 |
| 가격 계산 정확성 | 5/5 | 도메인 로직 캡슐화 완벽 |
| 새로운 가격 정책 추가 | 5/5 | Strategy Pattern + Port |
| 재고 관리 복잡도 | 5/5 | Domain Service로 명확한 처리 |
| 테스트 용이성 | 5/5 | 순수 도메인 로직 테스트 |
| 개발 속도 | 3/5 | 초기 설계 시간 필요 |

**총점: 28/30**

---

### 3. Clean Architecture (클린 아키텍처)

#### 구조
```
┌───────────────────────────────────────────────────┐
│                  Frameworks & Drivers             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │   Web    │  │    DB    │  │  Kafka   │        │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘        │
└───────┼─────────────┼─────────────┼───────────────┘
        │             │             │
┌───────┼─────────────┼─────────────┼───────────────┐
│       │   Interface Adapters      │               │
│  ┌────▼─────┐  ┌────▼──────┐  ┌──▼─────┐         │
│  │Controller│  │Repository │  │Event   │         │
│  │(Adapter) │  │(Adapter)  │  │Adapter │         │
│  └────┬─────┘  └────┬──────┘  └──┬─────┘         │
└───────┼─────────────┼─────────────┼───────────────┘
        │             │             │
┌───────┼─────────────┼─────────────┼───────────────┐
│       │      Application Business Rules           │
│  ┌────▼──────────────────────────────┐            │
│  │        Use Cases (Interactor)     │            │
│  │  - CreateReservationUseCase       │            │
│  │  - CalculatePriceUseCase          │            │
│  └────┬──────────────────────────────┘            │
└───────┼───────────────────────────────────────────┘
        │
┌───────┼───────────────────────────────────────────┐
│       │      Enterprise Business Rules            │
│  ┌────▼──────────────────────────────┐            │
│  │         Domain Entities            │            │
│  │  - PricingPolicy (Aggregate)      │            │
│  │  - Product (Aggregate)            │            │
│  │  - ReservationPricing (Aggregate) │            │
│  └───────────────────────────────────┘            │
└───────────────────────────────────────────────────┘
```

#### 특징
- 동심원 구조 (의존성 내부 방향)
- Use Case 중심 설계
- 엔터프라이즈 비즈니스 규칙과 애플리케이션 규칙 분리

#### 장점
1. **명확한 책임 분리**: Enterprise 규칙 vs Application 규칙
2. **Use Case 명시적 표현**: 비즈니스 유스케이스가 코드에 드러남
3. **높은 독립성**: 프레임워크/DB/UI 독립적
4. **테스트 용이**: 각 레이어별 독립 테스트
5. **변경 격리**: 외부 변경이 내부에 영향 최소화

#### 단점
1. **가장 높은 복잡도**: 4개 레이어 + 인터페이스 관리
2. **학습 비용**: 팀 전체의 철학 이해 필요
3. **과도한 설계**: 중소규모 프로젝트에는 오버엔지니어링
4. **Use Case 폭발**: 비슷한 Use Case 여러 개 생성 가능

#### 프로젝트 적합성 평가

| 평가 항목 | 점수 | 근거 |
|---------|------|------|
| 복잡한 도메인 규칙 처리 | 5/5 | Enterprise 규칙 명확히 분리 |
| 가격 계산 정확성 | 5/5 | Domain Entity에 캡슐화 |
| 새로운 가격 정책 추가 | 4/5 | Use Case 추가 필요 |
| 재고 관리 복잡도 | 5/5 | Use Case로 명확한 흐름 |
| 테스트 용이성 | 5/5 | 레이어별 독립 테스트 |
| 개발 속도 | 2/5 | 높은 초기 설계 비용 |

**총점: 26/30**

---

## 추가 고려사항: DDD (Domain-Driven Design)

DDD는 아키텍처 패턴이 아닌 **설계 방법론**이지만, Hexagonal/Clean Architecture와 함께 사용됩니다.

### DDD 핵심 개념 적용

#### Aggregate (집합체)
```java
// PricingPolicy Aggregate
class PricingPolicy {
    private RoomId roomId; // Aggregate Root ID
    private Money defaultPrice;
    private TimeRangePrices timeRangePrices; // Value Object

    // 불변식: 시간대 중복 불가
    void resetAndUpdatePrices(Money defaultPrice, List<TimeRangePrice> newPrices) {
        this.timeRangePrices = TimeRangePrices.of(newPrices); // 검증 포함
    }
}
```

**장점:**
- 비즈니스 규칙을 Aggregate 내부에 캡슐화
- Transaction 경계 명확
- 불변식(Invariant) 보장

#### Value Object (값 객체)
```java
// Money (불변)
record Money(BigDecimal amount) {
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
}

// TimeRangePrice (불변)
record TimeRangePrice(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Money pricePerSlot
) {
    // 중복 검증 로직
}
```

**장점:**
- 도메인 개념을 명시적으로 표현
- 불변성으로 Side Effect 방지
- 비즈니스 로직을 객체에 캡슐화

#### Domain Event (도메인 이벤트)
```java
record ReservationCreatedEvent(
    ReservationId reservationId,
    List<ProductId> productIds,
    List<Integer> quantities
) implements DomainEvent {}
```

**장점:**
- 도메인 변화를 명시적으로 표현
- 비동기 처리 가능 (재고 차감)
- 이벤트 소싱 확장 가능

---

## 비교 매트릭스

| 평가 기준 | Layered | Hexagonal | Clean | 가중치 |
|----------|---------|-----------|-------|--------|
| **도메인 복잡도 처리** | 2/5 | 5/5 | 5/5 | 30% |
| **확장성 (OCP)** | 2/5 | 5/5 | 4/5 | 25% |
| **테스트 용이성** | 2/5 | 5/5 | 5/5 | 20% |
| **개발 속도** | 5/5 | 3/5 | 2/5 | 10% |
| **유지보수성** | 2/5 | 5/5 | 4/5 | 10% |
| **팀 학습 곡선** | 5/5 | 3/5 | 2/5 | 5% |
| **가중 평균** | **2.4** | **4.6** | **4.3** | |

---

## 최종 권장: Hexagonal Architecture + DDD

### 선정 이유

#### 1. 도메인 복잡도에 최적
본 프로젝트는 다음과 같은 복잡한 도메인 규칙을 가짐:
- 시간대별 가격 중복 검증
- 3가지 Scope의 재고 관리 전략
- 예약 상태별 재고 차감/복구
- 가격 Snapshot 불변성 보장

**Hexagonal + DDD는 이러한 복잡도를 Aggregate와 Domain Service로 명확히 표현 가능**

#### 2. SOLID 원칙 준수
- **SRP**: 각 Aggregate는 단일 책임
- **OCP**: Port를 통해 확장에 열려있음
- **LSP**: Value Object와 Entity 분리
- **ISP**: Port 인터페이스 세분화
- **DIP**: 도메인이 Infrastructure에 의존하지 않음

#### 3. 비즈니스 요구사항 변경 대응
- 새로운 가격 정책 추가 → Strategy Pattern + Port 확장
- 새로운 상품 Scope 추가 → Enum + Domain Service 확장
- 새로운 계산 로직 추가 → Use Case 추가 (기존 코드 불변)

#### 4. 테스트 전략 명확
```
Unit Test (도메인):
├─ Aggregate 불변식 테스트
├─ Value Object 계산 로직 테스트
└─ Domain Service 재고 검증 테스트

Integration Test (Application):
├─ Use Case 시나리오 테스트
└─ Port/Adapter 통합 테스트

E2E Test:
└─ 전체 플로우 테스트
```

#### 5. Clean Architecture 대비 장점
- Use Case 레이어가 없어 복잡도 낮음
- Application Service가 Use Case 역할 수행
- 4레이어 → 3레이어로 단순화

---

## 레이어 구조 확정

### Hexagonal Architecture + DDD 적용

```
┌─────────────────────────────────────────────────────────────┐
│                        Adapter Layer                         │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ REST Controller│  │Event Listener│  │JPA Repository   │  │
│  │                │  │              │  │Adapter          │  │
│  └───────┬────────┘  └──────┬───────┘  └────────┬────────┘  │
└──────────┼────────────────┼──────────────────────┼──────────┘
           │                │                      │
           ▼                ▼                      ▼
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           Application Services (Use Cases)            │  │
│  │  - ReservationPricingService                          │  │
│  │  - PricingPolicyService                               │  │
│  │  - ProductService                                     │  │
│  └───────────────────┬───────────────────────────────────┘  │
└─────────────────────┼──────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                 Aggregates                            │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │ PricingPolicy                                   │  │  │
│  │  │  - RoomId (ID)                                  │  │  │
│  │  │  - Money defaultPrice                           │  │  │
│  │  │  - TimeRangePrices (VO)                         │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │ Product                                         │  │  │
│  │  │  - ProductId (ID)                               │  │  │
│  │  │  - ProductScope (Enum)                          │  │  │
│  │  │  - PricingStrategy (VO)                         │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │ ReservationPricing                              │  │  │
│  │  │  - ReservationId (ID)                           │  │  │
│  │  │  - TimeSlotPriceBreakdown (VO)                  │  │  │
│  │  │  - List<ProductPriceBreakdown> (VO)             │  │  │
│  │  │  - Money totalPrice                             │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Domain Services                          │  │
│  │  - ProductAvailabilityService                         │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │            Repository Ports (Interfaces)              │  │
│  │  - PricingPolicyRepository                            │  │
│  │  - ProductRepository                                  │  │
│  │  - ReservationPricingRepository                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 패키지 구조

```
com.example.reservationpricing
├── adapter
│   ├── in
│   │   ├── web
│   │   │   ├── PricingPolicyController.java
│   │   │   ├── ProductController.java
│   │   │   └── ReservationPricingController.java
│   │   └── event
│   │       └── RoomEventListener.java
│   └── out
│       ├── persistence
│       │   ├── PricingPolicyJpaAdapter.java
│       │   ├── ProductJpaAdapter.java
│       │   └── ReservationPricingJpaAdapter.java
│       └── event
│           └── KafkaEventPublisher.java
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── CreateReservationUseCase.java
│   │   │   ├── UpdatePricingPolicyUseCase.java
│   │   │   └── RegisterProductUseCase.java
│   │   └── out
│   │       ├── PricingPolicyRepository.java
│   │       ├── ProductRepository.java
│   │       └── EventPublisher.java
│   └── service
│       ├── ReservationPricingService.java
│       ├── PricingPolicyService.java
│       └── ProductService.java
└── domain
    ├── pricingpolicy
    │   ├── PricingPolicy.java (Aggregate Root)
    │   ├── TimeRangePrices.java (Value Object)
    │   └── TimeRangePrice.java (Value Object)
    ├── product
    │   ├── Product.java (Aggregate Root)
    │   ├── ProductScope.java (Enum)
    │   ├── PricingStrategy.java (Value Object)
    │   └── PricingType.java (Enum)
    ├── reservation
    │   ├── ReservationPricing.java (Aggregate Root)
    │   ├── ReservationStatus.java (Enum)
    │   ├── TimeSlotPriceBreakdown.java (Value Object)
    │   └── ProductPriceBreakdown.java (Value Object)
    ├── service
    │   └── ProductAvailabilityService.java (Domain Service)
    └── shared
        ├── Money.java (Value Object)
        ├── RoomId.java (Value Object)
        └── PlaceId.java (Value Object)
```

---

## 아키텍처 품질 속성 보장

### 1. 정확성 (Correctness)
- **Aggregate 불변식**: 가격 중복, 재고 음수 방지
- **Value Object 불변성**: Money, TimeRangePrice 불변
- **Domain Event**: 상태 변화 추적

### 2. 확장성 (Extensibility)
- **Strategy Pattern**: 새로운 가격 정책 추가
- **Port/Adapter**: 새로운 외부 시스템 연동
- **Open-Closed Principle**: 기존 코드 수정 없이 확장

### 3. 테스트 가능성 (Testability)
- **순수 도메인**: Infrastructure 의존 없음
- **Port Mocking**: Use Case 단위 테스트
- **Aggregate 단위**: 불변식 검증 테스트

### 4. 유지보수성 (Maintainability)
- **명확한 책임**: 각 레이어 역할 분명
- **낮은 결합도**: Adapter 교체 가능
- **높은 응집도**: 관련 로직을 Aggregate에 집중

---

## 결론

**Hexagonal Architecture + DDD 조합**이 본 프로젝트의 복잡한 도메인 규칙과 비기능 요구사항을 충족하는 최적의 선택입니다.

**핵심 근거:**
1. 복잡한 가격 계산 및 재고 관리 로직을 도메인에 캡슐화
2. SOLID 원칙 준수로 확장성 보장
3. 테스트 용이성으로 정확성 보장
4. Clean Architecture보다 단순하면서도 충분한 격리 제공

다음 단계: **도메인 모델 설계 대안 비교**에서 구체적인 Aggregate 설계를 다룹니다.

---

**Last Updated**: 2025-11-12
