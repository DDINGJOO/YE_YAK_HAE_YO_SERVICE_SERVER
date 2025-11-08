# 아키텍처 개요

## 전체 아키텍처

본 프로젝트는 **헥사고날 아키텍처(Hexagonal Architecture)** 패턴을 기반으로 설계되었습니다.

### 핵심 원칙

1. **도메인 중심 설계**: 비즈니스 로직이 인프라스트럭처와 독립적
2. **의존성 역전**: 외부 계층이 내부(도메인)에 의존
3. **포트와 어댑터**: 명확한 인터페이스를 통한 외부 시스템 연동
4. **테스트 용이성**: 도메인 로직을 독립적으로 테스트 가능

## 계층 구조

```
src/main/java/com/teambind/springproject/
├── domain/                    # 도메인 계층 (핵심 비즈니스 로직)
│   ├── pricingpolicy/        # PricingPolicy Aggregate
│   │   ├── PricingPolicy.java
│   │   ├── TimeRangePrice.java
│   │   ├── TimeRangePrices.java
│   │   └── exception/        # 도메인 예외
│   ├── product/              # Product Aggregate (예정)
│   ├── reservationpricing/   # ReservationPricing Aggregate (예정)
│   └── shared/               # Shared Kernel (공통 Value Objects)
│       ├── Money.java
│       ├── RoomId.java
│       ├── PlaceId.java
│       ├── TimeSlot.java
│       ├── TimeRange.java
│       └── DayOfWeek.java
│
├── application/              # 애플리케이션 계층 (유스케이스)
│   ├── port/
│   │   ├── in/              # 인바운드 포트 (Use Case 인터페이스)
│   │   │   ├── CreatePricingPolicyUseCase.java
│   │   │   ├── UpdatePricingPolicyUseCase.java
│   │   │   ├── GetPricingPolicyUseCase.java
│   │   │   └── CopyPricingPolicyUseCase.java
│   │   └── out/             # 아웃바운드 포트 (Repository 인터페이스)
│   │       └── PricingPolicyRepository.java
│   ├── service/             # 유스케이스 구현
│   │   └── pricingpolicy/
│   │       ├── CreatePricingPolicyService.java
│   │       ├── UpdatePricingPolicyService.java
│   │       ├── GetPricingPolicyService.java
│   │       └── CopyPricingPolicyService.java
│   └── dto/                 # DTO (Request/Response)
│       ├── request/
│       └── response/
│
├── adapter/                 # 어댑터 계층 (외부 인터페이스)
│   ├── in/                  # 인바운드 어댑터
│   │   ├── web/            # REST API 컨트롤러
│   │   │   └── pricingpolicy/
│   │   │       └── PricingPolicyController.java
│   │   └── messaging/      # 이벤트 리스너
│   │       ├── consumer/
│   │       │   └── EventConsumer.java
│   │       └── handler/
│   │           └── RoomCreatedEventHandler.java
│   └── out/                # 아웃바운드 어댑터
│       ├── persistence/    # JPA Repository 구현
│       │   └── pricingpolicy/
│       │       ├── PricingPolicyRepositoryAdapter.java
│       │       └── JpaPricingPolicyRepository.java
│       └── messaging/      # 이벤트 발행 (예정)
│
└── common/                 # 공통 인프라
    ├── config/            # 설정
    ├── exceptions/        # 공통 예외 처리
    │   ├── ErrorResponse.java
    │   └── GlobalExceptionHandler.java
    └── util/              # 유틸리티
```

## 계층별 책임

### 1. Domain 계층

**위치**: `domain/`

**책임**:
- 핵심 비즈니스 로직 구현
- Aggregate Root, Entity, Value Object 정의
- 도메인 규칙 및 제약사항 강제
- 도메인 예외 정의

**의존성**: 없음 (외부 계층에 의존하지 않음)

**예시**:
```java
// PricingPolicy Aggregate Root
public class PricingPolicy {
    private final RoomId roomId;
    private final PlaceId placeId;
    private Money defaultPrice;
    private TimeRangePrices timeRangePrices;

    // 비즈니스 로직
    public void updateDefaultPrice(Money newPrice) {
        validatePrice(newPrice);
        this.defaultPrice = newPrice;
    }

    public void resetPrices(TimeRangePrices newPrices) {
        this.timeRangePrices = newPrices;
    }
}
```

### 2. Application 계층

**위치**: `application/`

**책임**:
- 유스케이스 구현 (비즈니스 플로우 조율)
- 트랜잭션 경계 관리
- 포트 인터페이스 정의
- DTO 변환

**의존성**: Domain 계층만 의존

**구조**:
- `port/in`: 외부에서 호출하는 인터페이스 (Use Case)
- `port/out`: 외부 시스템을 호출하는 인터페이스 (Repository, Event Publisher 등)
- `service`: Use Case 구현체

**예시**:
```java
// Use Case 인터페이스
public interface UpdatePricingPolicyUseCase {
    PricingPolicy updateDefaultPrice(RoomId roomId, Money defaultPrice);
}

// Use Case 구현
@Service
@Transactional
public class UpdatePricingPolicyService implements UpdatePricingPolicyUseCase {
    private final PricingPolicyRepository repository;

    @Override
    public PricingPolicy updateDefaultPrice(RoomId roomId, Money defaultPrice) {
        PricingPolicy policy = repository.findById(roomId)
            .orElseThrow(() -> new PricingPolicyNotFoundException(...));

        policy.updateDefaultPrice(defaultPrice);
        return repository.save(policy);
    }
}
```

### 3. Adapter 계층

**위치**: `adapter/`

**책임**:
- 외부 시스템과의 통신
- 프로토콜 변환 (HTTP, Kafka 등)
- 영속성 구현 (JPA)
- 포트 인터페이스 구현

**의존성**: Application 계층의 포트에 의존

#### 3.1 Inbound Adapter (adapter/in)

외부에서 애플리케이션으로 들어오는 요청 처리

**Web Adapter (REST API)**:
```java
@RestController
@RequestMapping("/api/pricing-policies")
public class PricingPolicyController {
    private final UpdatePricingPolicyUseCase updateUseCase;

    @PutMapping("/{roomId}/default-price")
    public ResponseEntity<PricingPolicyResponse> updateDefaultPrice(
            @PathVariable Long roomId,
            @RequestBody UpdateDefaultPriceRequest request) {

        Money money = Money.of(request.defaultPrice());
        PricingPolicy policy = updateUseCase.updateDefaultPrice(
            RoomId.of(roomId), money);

        return ResponseEntity.ok(PricingPolicyResponse.from(policy));
    }
}
```

**Messaging Adapter (이벤트 리스너)**:
```java
@Component
public class RoomCreatedEventHandler {
    private final CreatePricingPolicyUseCase createUseCase;

    public void handle(Map<String, Object> eventData) {
        Long roomId = (Long) eventData.get("roomId");
        Long placeId = (Long) eventData.get("placeId");
        String timeSlot = (String) eventData.get("timeSlot");

        createUseCase.createDefaultPolicy(
            RoomId.of(roomId),
            PlaceId.of(placeId),
            TimeSlot.valueOf(timeSlot)
        );
    }
}
```

#### 3.2 Outbound Adapter (adapter/out)

애플리케이션에서 외부 시스템으로 나가는 호출 처리

**Persistence Adapter (JPA)**:
```java
@Component
@RequiredArgsConstructor
public class PricingPolicyRepositoryAdapter implements PricingPolicyRepository {
    private final JpaPricingPolicyRepository jpaRepository;

    @Override
    public Optional<PricingPolicy> findById(RoomId roomId) {
        return jpaRepository.findById(roomId.getValue())
            .map(PricingPolicyJpaEntity::toDomain);
    }

    @Override
    public PricingPolicy save(PricingPolicy policy) {
        PricingPolicyJpaEntity entity = PricingPolicyJpaEntity.from(policy);
        PricingPolicyJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
```

### 4. Common 계층

**위치**: `common/`

**책임**:
- 공통 설정 (Configuration)
- 전역 예외 처리
- 유틸리티 클래스

**특징**:
- 도메인과 무관한 기술적 관심사
- 모든 계층에서 사용 가능

## 의존성 흐름

```
[외부 시스템] → [Adapter In] → [Application (Use Case)] → [Domain]
                                        ↓
                               [Adapter Out] → [외부 시스템]
```

**규칙**:
1. Domain은 어떤 계층에도 의존하지 않음
2. Application은 Domain만 의존
3. Adapter는 Application의 포트를 구현
4. 의존성은 항상 안쪽(Domain)을 향함

## 주요 패턴

### 1. Aggregate Pattern

관련된 객체들을 하나의 단위로 묶어 관리

**예시**: `PricingPolicy` Aggregate
- Aggregate Root: `PricingPolicy`
- Value Objects: `TimeRangePrices`, `TimeRangePrice`
- 외부에서는 Aggregate Root를 통해서만 접근

### 2. Repository Pattern

도메인 객체의 영속성을 추상화

**구현**:
- `application/port/out/PricingPolicyRepository.java`: 인터페이스 (포트)
- `adapter/out/persistence/.../PricingPolicyRepositoryAdapter.java`: 구현 (어댑터)

### 3. Use Case Pattern

각 비즈니스 기능을 명확한 인터페이스로 정의

**예시**:
- `CreatePricingPolicyUseCase`
- `UpdatePricingPolicyUseCase`
- `GetPricingPolicyUseCase`
- `CopyPricingPolicyUseCase`

### 4. Event-Driven Architecture

도메인 이벤트를 통한 느슨한 결합

**예시**: RoomCreatedEvent
1. Room 서비스에서 RoomCreatedEvent 발행
2. Kafka를 통해 이벤트 전달
3. EventConsumer가 이벤트 수신
4. RoomCreatedEventHandler가 처리
5. 가격 정책 자동 생성

## 예외 처리 전략

### 도메인별 예외 계층

각 Bounded Context는 독립적인 예외 구조 보유:

```
domain/pricingpolicy/exception/
├── PricingPolicyException.java              # Base Exception
├── PricingPolicyErrorCode.java              # Error Code Enum
├── PricingPolicyNotFoundException.java
├── CannotCopyDifferentPlaceException.java
└── ...
```

### 전역 예외 처리

`GlobalExceptionHandler`에서 모든 도메인 예외를 일관된 형식으로 변환:

```java
@ExceptionHandler(PricingPolicyException.class)
public ResponseEntity<ErrorResponse> handlePricingPolicyException(
        PricingPolicyException ex, HttpServletRequest request) {

    ErrorResponse response = ErrorResponse.builder()
        .status(ex.getHttpStatus().value())
        .code(ex.getErrorCode().getErrCode())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(ex.getHttpStatus()).body(response);
}
```

## 트랜잭션 관리

**전략**: Application 계층의 Service에서 트랜잭션 경계 관리

```java
@Service
@Transactional  // 서비스 메서드 단위로 트랜잭션
public class UpdatePricingPolicyService implements UpdatePricingPolicyUseCase {
    // ...
}
```

**이유**:
- Use Case 실행 전체가 원자적 작업 단위
- 도메인 계층은 트랜잭션에 무관
- Adapter는 기술적 구현만 담당

## 테스트 전략

### 1. 단위 테스트 (Domain)

도메인 로직을 독립적으로 테스트:

```java
@Test
void updateDefaultPrice_ShouldUpdatePrice() {
    // given
    PricingPolicy policy = PricingPolicy.create(...);
    Money newPrice = Money.of(new BigDecimal("50000"));

    // when
    policy.updateDefaultPrice(newPrice);

    // then
    assertThat(policy.getDefaultPrice()).isEqualTo(newPrice);
}
```

### 2. 단위 테스트 (Application)

Mock을 사용한 Use Case 테스트:

```java
@ExtendWith(MockitoExtension.class)
class UpdatePricingPolicyServiceTest {
    @Mock
    private PricingPolicyRepository repository;

    @InjectMocks
    private UpdatePricingPolicyService service;

    @Test
    void updateDefaultPrice_Success() {
        // given
        when(repository.findById(any())).thenReturn(Optional.of(policy));

        // when
        PricingPolicy result = service.updateDefaultPrice(roomId, money);

        // then
        verify(repository).save(any());
    }
}
```

### 3. 통합 테스트 (Adapter)

실제 환경과 유사한 테스트:

```java
@WebMvcTest(PricingPolicyController.class)
class PricingPolicyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UpdatePricingPolicyUseCase updateUseCase;

    @Test
    void updateDefaultPrice_Success() throws Exception {
        mockMvc.perform(put("/api/pricing-policies/1/default-price")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }
}
```

## 확장성

### 새로운 Aggregate 추가

1. `domain/{aggregate}/` 패키지 생성
2. Aggregate Root 및 Value Objects 구현
3. `application/port/in/` 에 Use Case 인터페이스 추가
4. `application/service/` 에 Use Case 구현
5. `adapter/in/web/` 에 Controller 추가
6. `adapter/out/persistence/` 에 Repository 구현

### 새로운 외부 시스템 연동

1. `application/port/out/` 에 포트 인터페이스 정의
2. `adapter/out/{system}/` 에 어댑터 구현
3. 기존 코드 수정 없이 확장 가능

## 관련 문서

- [도메인 모델](DOMAIN_MODEL.md)
- [API 문서](../features/pricing-policy/API.md)
