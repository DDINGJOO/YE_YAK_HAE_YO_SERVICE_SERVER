# Hexagonal Architecture 패키지 구조

이 프로젝트는 **Hexagonal Architecture (Port & Adapter Pattern)** 와 **Domain-Driven Design (DDD)** 를 기반으로 설계되었습니다.

## 전체 패키지 구조

```
com.teambind.springproject/
├── domain/                          # 도메인 레이어 (핵심 비즈니스 로직)
│   ├── pricingpolicy/              # PricingPolicy Aggregate
│   ├── product/                    # Product Aggregate
│   ├── reservationpricing/         # ReservationPricing Aggregate
│   └── shared/                     # Shared Kernel (공통 Value Objects)
│
├── application/                     # 애플리케이션 레이어 (Use Cases)
│   ├── port/
│   │   ├── in/                     # Inbound Ports (Use Case Interfaces)
│   │   └── out/                    # Outbound Ports (Infrastructure Interfaces)
│   ├── service/                    # Application Services (Use Case 구현체)
│   │   ├── pricingpolicy/
│   │   ├── product/
│   │   └── reservationpricing/
│   └── dto/                        # Data Transfer Objects
│       ├── request/
│       └── response/
│
├── adapter/                         # 어댑터 레이어 (Infrastructure)
│   ├── in/                          # Inbound Adapters (Primary)
│   │   └── web/                     # REST Controllers
│   │       ├── pricingpolicy/
│   │       ├── product/
│   │       └── reservationpricing/
│   └── out/                         # Outbound Adapters (Secondary)
│       ├── persistence/             # JPA Repositories
│       │   ├── pricingpolicy/
│       │   ├── product/
│       │   └── reservationpricing/
│       └── messaging/               # Kafka Event Handlers
│           └── event/
│
└── common/                          # 공통 유틸리티 및 설정
    ├── config/                      # Spring Configuration
    ├── exceptions/                  # 예외 처리
    └── util/                        # 유틸리티 클래스
```

---

## 레이어별 상세 설명

### 1. Domain Layer (도메인 레이어)

**위치:** `com.teambind.springproject.domain`

**책임:**
- 핵심 비즈니스 로직 구현
- 도메인 규칙(Invariants) 강제
- 외부 기술 스택에 의존하지 않음

**구성 요소:**
- **Aggregate Root**: 트랜잭션 일관성 경계를 정의하는 엔티티
- **Entity**: 고유 식별자를 가진 도메인 객체
- **Value Object**: 불변 객체, 속성의 조합으로 식별
- **Domain Service**: 여러 Aggregate에 걸친 비즈니스 로직
- **Repository Interface**: 영속성 추상화 (Port)

**설계 원칙:**
- ✅ 불변성(Immutability) 최대 활용
- ✅ 외부 레이어에 의존하지 않음 (Dependency Inversion)
- ✅ 도메인 규칙을 생성자 및 메서드에서 강제

---

#### 1.1 PricingPolicy Aggregate

**위치:** `com.teambind.springproject.domain.pricingpolicy`

**책임:**
- 시간대별 예약 가격 정책 관리
- 요일별, 시간대별 가격 차등 적용
- 시간대 중복 검증

**도메인 규칙:**
- ❌ 같은 요일 내에서 시간대 중복 불가
- ❌ 시작 시간 > 종료 시간
- ❌ 가격 < 0원

**주요 클래스:**
- `PricingPolicy`: Aggregate Root
- `TimeSlot`: Value Object (시간대)
- `DayOfWeek`: Enum (요일)

---

#### 1.2 Product Aggregate

**위치:** `com.teambind.springproject.domain.product`

**책임:**
- 추가상품 등록 및 관리
- 적용 범위 관리 (플레이스/룸/예약별)
- 재고 관리 (가용 수량, 총 수량)

**도메인 규칙:**
- ❌ 사용 수량 > 총 수량
- ✅ PENDING 또는 CONFIRMED 상태만 재고 차감
- ❌ 가격 < 0원

**주요 클래스:**
- `Product`: Aggregate Root
- `ProductScope`: Value Object (적용 범위)
- `ProductAvailabilityService`: Domain Service

---

#### 1.3 ReservationPricing Aggregate

**위치:** `com.teambind.springproject.domain.reservationpricing`

**책임:**
- 예약 시점의 가격 정보 스냅샷 저장
- 예약 총 가격 계산 (룸 가격 + 추가상품)
- 가격 정책 변경 이력 관리

**도메인 규칙:**
- ✅ 예약 가격은 생성 후 변경 불가 (Immutable Snapshot)
- ✅ 총 가격 = 룸 가격 + ∑(추가상품 가격)
- ❌ 가격 < 0원

**주요 클래스:**
- `ReservationPricing`: Aggregate Root
- `PricingItem`: Entity (가격 항목)

---

#### 1.4 Shared Kernel

**위치:** `com.teambind.springproject.domain.shared`

**포함 요소:**
- `Money`: 금액 Value Object
- `ReservationId`: 예약 ID Value Object
- `RoomId`: 룸 ID Value Object
- `PlaceId`: 플레이스 ID Value Object

**설계 원칙:**
- ✅ 모든 Value Object는 불변(Immutable)
- ✅ equals/hashCode 구현
- ✅ 생성자에서 유효성 검증

---

### 2. Application Layer (애플리케이션 레이어)

**위치:** `com.teambind.springproject.application`

**책임:**
- Use Case 구현 및 조율
- 트랜잭션 경계 정의
- DTO ↔ Domain 변환

**구성 요소:**
- **Inbound Port**: Use Case 인터페이스 (Primary Port)
- **Outbound Port**: Infrastructure 추상화 인터페이스 (Secondary Port)
- **Application Service**: Use Case 구현체
- **DTO**: 외부 레이어와 데이터 교환

**설계 원칙:**
- ✅ 얇은 레이어로 유지 (Thin Layer)
- ✅ 비즈니스 로직은 도메인 레이어에 위임
- ✅ Infrastructure 세부사항 모름 (Port를 통해 추상화)

---

### 3. Adapter Layer (어댑터 레이어)

**위치:** `com.teambind.springproject.adapter`

**책임:**
- 외부 세계와 애플리케이션 연결
- Port 구현 (Inbound/Outbound)
- 기술 세부사항 처리

#### 3.1 Inbound Adapter (Primary)

**위치:** `com.teambind.springproject.adapter.in.web`

**책임:**
- HTTP 요청 → Command/Query 변환
- Use Case 호출
- 결과 → HTTP 응답 변환
- 입력 Validation

**예시:**
```java
@RestController
@RequestMapping("/api/v1/pricing-policies")
public class PricingPolicyController {
    private final CreatePricingPolicyUseCase createPricingPolicyUseCase;

    @PostMapping
    public ResponseEntity<PricingPolicyResponse> createPricingPolicy(
            @Valid @RequestBody CreatePricingPolicyRequest request) {
        var command = CreatePricingPolicyCommand.from(request);
        var response = createPricingPolicyUseCase.createPricingPolicy(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### 3.2 Outbound Adapter (Secondary)

**Persistence (JPA):**

**위치:** `com.teambind.springproject.adapter.out.persistence`

**책임:**
- Domain Model ↔ JPA Entity 변환
- Domain Repository Port 구현
- 데이터베이스 접근 로직

**Messaging (Kafka):**

**위치:** `com.teambind.springproject.adapter.out.messaging`

**책임:**
- 도메인 이벤트 → Kafka 메시지 변환
- Kafka 메시지 발행/수신
- 이벤트 직렬화/역직렬화

---

## 의존성 규칙 (Dependency Rule)

```
┌─────────────────────────────────────────┐
│         Adapter Layer (Outer)           │
│  ┌───────────────────────────────────┐  │
│  │   Application Layer (Middle)      │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │   Domain Layer (Core)       │  │  │
│  │  │                             │  │  │
│  │  │  ✅ No Dependencies          │  │  │
│  │  └─────────────────────────────┘  │  │
│  │         ↑                          │  │
│  │         │ depends on               │  │
│  └───────────────────────────────────┘  │
│                ↑                         │
│                │ depends on              │
└─────────────────────────────────────────┘
```

**핵심 원칙:**
- ✅ **Domain Layer**: 어떤 레이어에도 의존하지 않음
- ✅ **Application Layer**: Domain Layer에만 의존
- ✅ **Adapter Layer**: Application Layer (Port)에만 의존
- ❌ **절대 금지**: 안쪽 레이어가 바깥쪽 레이어를 의존하는 것

---

## 개발 가이드

### 새로운 기능 추가 시 순서

1. **Domain Layer**: Aggregate, Entity, Value Object 구현
2. **Application Layer**: Use Case Port 정의 및 Application Service 구현
3. **Adapter Layer**: Controller, Repository Adapter 구현
4. **Test**: 각 레이어별 단위 테스트 작성

### 패키지 네이밍 규칙

- **소문자** 사용
- **복수형** 사용 (예: `products`, `pricingpolicies`)
- **명확한 비즈니스 용어** 사용

### 클래스 네이밍 규칙

- **Aggregate Root**: 명사 (예: `PricingPolicy`)
- **Value Object**: 명사 (예: `Money`, `TimeSlot`)
- **Domain Service**: `xxxService` (예: `ProductAvailabilityService`)
- **Application Service**: `xxxService` (예: `PricingPolicyService`)
- **Controller**: `xxxController` (예: `PricingPolicyController`)
- **Repository**: `xxxRepository` (예: `PricingPolicyRepository`)

---

## 참고 자료

- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

**Last Updated**: 2025-11-08
