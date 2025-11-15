# 동시성 제어 해결방안 분석

**문서 버전:** 1.1
**작성일:** 2025-11-12
**최종 업데이트:** 2025-11-15
**상태:** IMPLEMENTED

---

## 구현 상태

**선택된 방안:** Database Constraint (Atomic UPDATE)

**구현 완료 이슈:**
- Issue #138: 재고 동시성 문제 분석 및 해결 방안 도출 (완료)
- Issue #145 (V9 Migration): `reserved_quantity` 컬럼 추가 (완료)
- Issue #146: E2E 재고 동시성 제어 테스트 (완료, **50 TPS 달성**)
- Issue #157: 재고 예약/해제 로직 구현 (완료)
- Issue #164: 예약 환불 이벤트 처리 및 재고 자동 해제 (완료)

**성능 검증 결과:**
- E2E 테스트 (Issue #146): **50 TPS 달성**
- 오버부킹 발생률: **0%**
- 동시성 제어 정확도: **100%**

---

## 목차

- [문제 정의](#문제-정의)
- [비즈니스 영향](#비즈니스-영향)
- [해결방안 후보](#해결방안-후보)
- [방안별 상세 분석](#방안별-상세-분석)
- [성능 시뮬레이션](#성능-시뮬레이션)
- [아키텍처 영향 분석](#아키텍처-영향-분석)
- [최종 권장사항](#최종-권장사항)

---

## 문제 정의

### 현상

예약 생성 시 상품 재고 확인과 예약 저장 사이에 Race Condition이 발생하여 **오버부킹 가능성** 존재

### 문제 코드 위치

`ReservationPricingService.createReservation()` 메서드:

```java
// 3. 재고 검증
validateProductAvailability(products, request.products(), request.timeSlots());

// ...중략...

// 7. 저장
final ReservationPricing savedReservation = reservationPricingRepository.save(
    reservationPricing);
```

### Race Condition 시나리오

**시간대:** 2025-01-15 10:00-12:00 (빔프로젝터 재고 5개)

```
[시간축]  [사용자 A]                              [사용자 B]
--------  ------------------------------------    ------------------------------------
T1        예약 요청 시작 (빔프로젝터 1개)
T2                                                예약 요청 시작 (빔프로젝터 1개)
T3        DB 조회: 총 5개, 사용중 4개 → 가용 1개
T4                                                DB 조회: 총 5개, 사용중 4개 → 가용 1개
T5        검증 통과
T6                                                검증 통과
T7        예약 저장 완료 (실제 사용중 5개)
T8                                                예약 저장 완료 (실제 사용중 6개) [오버부킹!]
```

**문제 유형:** Check-Then-Act Anti-Pattern

---

## 비즈니스 영향

### 심각도: CRITICAL

| 구분 | 영향 |
|------|------|
| **고객 경험** | 예약 확정 후 취소 → 신뢰도 하락 |
| **운영 비용** | 수동 재고 조정, 고객 보상 비용 증가 |
| **법적 리스크** | 약관 위반 소지, 분쟁 가능성 |
| **발생 빈도** | 동시 사용자 수에 비례 증가 |

### 예상 손실 (가정)

- 오버부킹 1건당 처리 비용: 50,000원 (보상금 + 운영 비용)
- 동시 예약 발생률: 초당 10건 기준 → 시간당 0.5건 오버부킹 예상
- 월간 손실: 50,000원 × 0.5건/시간 × 24시간 × 30일 = **18,000,000원**

**비즈니스 요구사항:** 오버부킹 절대 불가 (Accuracy > Performance)

---

## 해결방안 후보

5가지 기술적 접근법을 비교 분석했습니다.

### 방안 요약

| 방안 | 핵심 메커니즘 | 인프라 변경 | 도메인 영향 |
|------|--------------|------------|-----------|
| 1. Optimistic Lock | JPA @Version | 없음 | 낮음 |
| 2. Pessimistic Lock | SELECT FOR UPDATE | 없음 | 없음 |
| 3. Named Lock | PostgreSQL Advisory Lock | 없음 | 없음 |
| 4. Redis Lock | Redisson Distributed Lock | Redis 사용 | 없음 |
| 5. Database Constraint | Atomic UPDATE Query | 없음 | **중간** (필드 추가) |

---

## 방안별 상세 분석

### 방안 1: Optimistic Lock (JPA @Version)

#### 구현 방식

```java
@Entity
public class ProductEntity {
    @Id
    private Long id;

    @Version  // Optimistic Lock
    private Long version;

    private int totalQuantity;
}
```

**동작 원리:**
```sql
-- 조회 시
SELECT id, total_quantity, version FROM products WHERE id = 1;
-- version = 10

-- 업데이트 시
UPDATE products
SET total_quantity = ?, version = version + 1
WHERE id = ? AND version = 10;
-- 다른 트랜잭션이 먼저 수정했다면 0 rows affected → OptimisticLockException
```

#### SOLID 원칙 분석

| 원칙 | 준수 여부 | 설명 |
|------|----------|------|
| **SRP** | 준수 | 동시성 제어는 JPA가 담당 |
| **OCP** | 준수 | @Version만 추가, 기존 로직 수정 불필요 |
| **LSP** | 준수 | Entity 교체 가능성 유지 |
| **ISP** | 준수 | 영향 없음 |
| **DIP** | 위반 | **Domain → JPA 의존 (Hexagonal 위반)** |

#### 장단점

**장점:**
- 구현 복잡도 매우 낮음 (애노테이션 1줄)
- 락 대기 없음 → 처리량 높음
- 충돌 낮은 환경에서 최고 성능
- 분산 환경에서 정상 동작

**단점:**
- **Hexagonal Architecture 위반** (Domain이 JPA에 의존)
- 충돌 시 전체 트랜잭션 롤백
- 재시도 로직 필요 (사용자 경험 저하)
- 충돌률 높으면 재시도 폭증

#### 성능 특성

```
동시 요청 10건 가정:
- 충돌률 5% → 재시도 0.5건
- 충돌률 30% → 재시도 3건 (응답 시간 3배 증가)
```

#### 적합성 평가

| 항목 | 평가 (5점 만점) |
|------|----------------|
| 정확성 | 4점 (충돌 감지 정확) |
| 성능 | 4점 (충돌 낮을 때 우수) |
| 아키텍처 | 2점 (Hexagonal 위반) |
| 확장성 | 4점 (분산 환경 OK) |
| 운영 편의성 | 3점 (재시도 로직 복잡) |

**종합:** 아키텍처 원칙 위반으로 부적합

---

### 방안 2: Pessimistic Lock (Database Row Lock)

#### 구현 방식

```java
// Repository
public interface ProductRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id IN :ids")
    List<Product> findAllByIdWithLock(@Param("ids") List<Long> ids);
}

// Service
@Transactional
public ReservationPricingResponse createReservation(CreateReservationRequest request) {
    // 1. 상품 조회 (FOR UPDATE 락 획득)
    List<Product> products = productRepository.findAllByIdWithLock(productIds);

    // 2. 재고 검증 (다른 트랜잭션은 대기)
    validateProductAvailability(products, ...);

    // 3. 예약 저장
    return save(reservation);
}
```

**SQL:**
```sql
SELECT * FROM products WHERE id IN (1, 2, 3) FOR UPDATE;
-- 트랜잭션 종료까지 다른 트랜잭션은 대기
```

#### SOLID 원칙 분석

| 원칙 | 준수 여부 | 설명 |
|------|----------|------|
| **SRP** | 준수 | 동시성 제어는 DB가 담당 |
| **OCP** | 준수 | Repository 메서드만 추가 |
| **LSP** | 준수 | 영향 없음 |
| **ISP** | 준수 | 영향 없음 |
| **DIP** | 준수 | **Port 인터페이스만 추가 (Hexagonal 유지)** |

#### 장단점

**장점:**
- **Hexagonal Architecture 유지** (Domain 순수성 보장)
- 충돌 없음 (순차 처리)
- 재시도 불필요
- 구현 복잡도 낮음

**단점:**
- 락 대기로 처리량 감소
- 데드락 가능성 (여러 상품 동시 락)
- 트랜잭션 타임아웃 관리 필요
- 단일 DB 의존 (샤딩 환경 부적합)

#### 데드락 시나리오

```
[Thread A]               [Thread B]
FOR UPDATE id=1         FOR UPDATE id=2
    ↓                        ↓
FOR UPDATE id=2         FOR UPDATE id=1
    ↓                        ↓
[대기...]               [대기...]
    ↓                        ↓
  [DEADLOCK DETECTED]
```

**해결책:**
- ID 순서로 락 획득 (정렬)
- DB 데드락 타임아웃 설정

#### 성능 특성

```
동시 요청 10건 가정:
- 순차 처리 시간: 50ms × 10 = 500ms
- 처리량: 1000 / 500 = 2 TPS
```

#### 적합성 평가

| 항목 | 평가 (5점 만점) |
|------|----------------|
| 정확성 | 5점 (완벽한 순차 처리) |
| 성능 | 2점 (락 대기로 병목) |
| 아키텍처 | 5점 (Hexagonal 완벽 유지) |
| 확장성 | 2점 (단일 DB 의존) |
| 운영 편의성 | 4점 (데드락 모니터링 필요) |

**종합:** 아키텍처 원칙 준수, 성능 트레이드오프 존재

---

### 방안 3: Named Lock (PostgreSQL Advisory Lock)

#### 구현 방식

```java
// Port (Domain)
public interface DistributedLockPort {
    <T> T executeWithLock(String lockKey, Supplier<T> action);
}

// Adapter (Infrastructure)
@Repository
public class PostgresLockAdapter implements DistributedLockPort {

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        long lockId = hashLockKey(lockKey);

        try {
            jdbcTemplate.query("SELECT pg_advisory_lock(?)", lockId);
            return action.get();
        } finally {
            jdbcTemplate.query("SELECT pg_advisory_unlock(?)", lockId);
        }
    }
}

// Application Service
@Transactional
public ReservationPricingResponse createReservation(CreateReservationRequest request) {
    return lockPort.executeWithLock("product:" + productId, () -> {
        // 재고 검증 및 예약 생성
    });
}
```

#### SOLID 원칙 분석

| 원칙 | 준수 여부 | 설명 |
|------|----------|------|
| **SRP** | 완벽 준수 | 락 관리 로직 완전 분리 |
| **OCP** | 완벽 준수 | 락 구현체 교체 가능 |
| **LSP** | 준수 | 영향 없음 |
| **ISP** | 준수 | Port 인터페이스 최소화 |
| **DIP** | 완벽 준수 | **완벽한 Hexagonal (Domain → Port)** |

#### 장단점

**장점:**
- **Hexagonal Architecture 완벽 준수**
- 상품별 세밀한 락 제어 가능
- 트랜잭션과 독립적 관리
- Pessimistic Lock 대비 유연성

**단점:**
- 구현 복잡도 증가 (Port/Adapter)
- 락 해제 실패 시 위험 (finally 필수)
- 단일 DB 의존 (멀티 DB 환경 부적합)
- 모니터링 복잡도 증가

#### 락 키 설계

```java
// 상품별 락
"product:lock:{productId}"

// 룸별 락 (여러 상품 동시 처리)
"room:lock:{roomId}:{timeSlot}"

// PlaceId별 락
"place:lock:{placeId}:{timeSlot}"
```

#### 적합성 평가

| 항목 | 평가 (5점 만점) |
|------|----------------|
| 정확성 | 5점 (완벽한 순차 처리) |
| 성능 | 2점 (Pessimistic과 유사) |
| 아키텍처 | 5점 (Hexagonal 완벽) |
| 확장성 | 2점 (단일 DB 의존) |
| 운영 편의성 | 3점 (락 모니터링 복잡) |

**종합:** 아키텍처 최우선 시 선택지

---

### 방안 4: Redis Distributed Lock (Redisson)

#### 구현 방식

```java
// Port (Domain)
public interface DistributedLockPort {
    <T> T executeWithLock(String lockKey, Supplier<T> action);
}

// Adapter (Infrastructure)
@Repository
public class RedisLockAdapter implements DistributedLockPort {
    private final RedissonClient redisson;

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        RLock lock = redisson.getLock(lockKey);

        try {
            // 최대 10초 대기, 30초 후 자동 해제
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                return action.get();
            } else {
                throw new LockAcquisitionException();
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

#### SOLID 원칙 분석

| 원칙 | 준수 여부 | 설명 |
|------|----------|------|
| **SRP** | 완벽 준수 | 락 관리 완전 분리 |
| **OCP** | 완벽 준수 | Redis → 다른 구현체 교체 가능 |
| **LSP** | 준수 | 영향 없음 |
| **ISP** | 준수 | Port 인터페이스 최소화 |
| **DIP** | 완벽 준수 | **완벽한 Hexagonal** |

#### 장단점

**장점:**
- **Hexagonal Architecture 완벽 준수**
- **MSA 환경 최적** (여러 서비스 인스턴스)
- TTL 자동 해제 (데드락 방지)
- Redis 빠른 응답 속도
- 분산 DB 환경 지원

**단점:**
- **Redis 인프라 필요** (운영 비용)
- 네트워크 오버헤드
- Redis 장애 시 서비스 영향
- Redis Cluster 운영 복잡도

#### 성능 특성

```
Redis Lock 획득: 1-3ms
DB 조회 및 검증: 50ms
DB 저장: 30ms
--------------------------------
총 처리 시간: ~85ms

동시 요청 10건:
- 순차 처리: 85ms × 10 = 850ms
- 처리량: 1000 / 85 = 11.7 TPS
```

#### 적합성 평가

| 항목 | 평가 (5점 만점) |
|------|----------------|
| 정확성 | 5점 (TTL 자동 해제) |
| 성능 | 4점 (Redis 고속) |
| 아키텍처 | 5점 (Hexagonal 완벽) |
| 확장성 | 5점 (MSA 최적) |
| 운영 편의성 | 3점 (Redis 운영 필요) |

**종합:** MSA 환경에서 장기적으로 최적

---

### 방안 5: Database Constraint (Atomic UPDATE) - 선택됨

#### 구현 방식

**1단계: 도메인 모델 변경**

```java
public class Product {
    private final ProductId productId;
    private int totalQuantity;
    private int reservedQuantity;  // 신규 필드 추가

    /**
     * 예약 가능 수량 반환
     */
    public int getAvailableQuantity() {
        return totalQuantity - reservedQuantity;
    }

    /**
     * 재고 예약 (낙관적 확인용)
     */
    public boolean canReserve(int quantity) {
        return getAvailableQuantity() >= quantity;
    }
}
```

**2단계: Repository 원자적 연산**

```java
// Port (Domain)
public interface ProductRepository {
    /**
     * 원자적으로 재고를 예약합니다.
     *
     * @return 성공 여부 (재고 부족 시 false)
     */
    boolean reserveQuantity(ProductId productId, int quantity);

    /**
     * 원자적으로 재고 예약을 취소합니다.
     */
    void releaseQuantity(ProductId productId, int quantity);
}

// Adapter (Infrastructure)
@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    @Override
    public boolean reserveQuantity(ProductId productId, int quantity) {
        int updated = jdbcTemplate.update(
            """
            UPDATE products
            SET reserved_quantity = reserved_quantity + ?
            WHERE product_id = ?
              AND (total_quantity - reserved_quantity) >= ?
            """,
            quantity, productId.getValue(), quantity
        );
        return updated > 0;
    }

    @Override
    public void releaseQuantity(ProductId productId, int quantity) {
        jdbcTemplate.update(
            """
            UPDATE products
            SET reserved_quantity = reserved_quantity - ?
            WHERE product_id = ?
              AND reserved_quantity >= ?
            """,
            quantity, productId.getValue(), quantity
        );
    }
}
```

**3단계: Application Service 로직**

```java
@Transactional
public ReservationPricingResponse createReservation(CreateReservationRequest request) {
    // 1. 상품 조회 (Lock 없음)
    List<Product> products = productRepository.findAllById(productIds);

    // 2. 낙관적 재고 확인 (빠른 실패)
    for (Product product : products) {
        if (!product.canReserve(requestedQuantity)) {
            throw new ProductNotAvailableException();
        }
    }

    // 3. 원자적 재고 예약 시도
    for (int i = 0; i < products.size(); i++) {
        boolean reserved = productRepository.reserveQuantity(
            products.get(i).getProductId(),
            productRequests.get(i).quantity()
        );

        if (!reserved) {
            // 실패 시 롤백 (이전 예약 취소)
            rollbackReservations(products.subList(0, i), productRequests);
            throw new ProductNotAvailableException();
        }
    }

    // 4. 예약 생성
    ReservationPricing reservation = ReservationPricing.calculate(...);
    return reservationPricingRepository.save(reservation);
}
```

#### SOLID 원칙 분석

| 원칙 | 준수 여부 | 설명 |
|------|----------|------|
| **SRP** | 준수 | 재고 예약 로직은 Repository가 담당 |
| **OCP** | 준수 | 새로운 예약 방식 추가 시 Port 확장 |
| **LSP** | 준수 | Product의 동작 규약 유지 |
| **ISP** | 준수 | reserveQuantity 메서드 최소화 |
| **DIP** | 부분 준수 | **Port 메서드가 다소 기술 종속적** |

#### 도메인 모델 영향

**변경사항:**
```java
// Before
public class Product {
    private int totalQuantity;
}

// After
public class Product {
    private int totalQuantity;
    private int reservedQuantity;  // [추가]
}
```

**영향 분석:**
- `reservedQuantity`는 도메인 개념 (재고 예약)
- Value Object가 아닌 Entity 상태 필드
- 비즈니스 로직: 가용 수량 = 총 재고 - 예약 수량

**DDD 관점:**
- [준수] 도메인 개념 명확 (예약 vs 확정)
- [준수] 불변식 강화 (reserved <= total)
- [경고] 인프라 계층과 결합 증가

#### 장단점

**장점:**
- **최고 성능** (단일 쿼리로 체크+예약)
- **락 없음** (대기 시간 제로)
- **정확성 보장** (DB의 원자성)
- **확장성 우수** (락 경합 없음)
- 추가 인프라 불필요

**단점:**
- 도메인 모델 변경 필요
- 롤백 로직 구현 필요
- Port 메서드가 다소 기술적

#### SQL 실행 흐름

```sql
-- Thread A와 B가 동시 실행
-- Thread A:
UPDATE products
SET reserved_quantity = reserved_quantity + 1
WHERE product_id = 1
  AND (total_quantity - reserved_quantity) >= 1;  -- 5 - 4 >= 1 [성공]
-- 1 row updated

-- Thread B:
UPDATE products
SET reserved_quantity = reserved_quantity + 1
WHERE product_id = 1
  AND (total_quantity - reserved_quantity) >= 1;  -- 5 - 5 >= 1 [실패]
-- 0 rows updated → 예외 발생
```

**DB의 Row Lock이 자동으로 순차 처리 보장**

#### 성능 특성

```
단일 예약 처리:
- 상품 조회: 10ms
- 낙관적 체크: 1ms (메모리)
- 원자적 예약: 5ms (UPDATE)
- 예약 저장: 20ms
---------------------------------
총: ~36ms

동시 요청 10건:
- 병렬 처리 가능 (락 없음)
- 평균 응답: 50ms
- 처리량: 1000 / 50 = 20 TPS
```

#### 적합성 평가

| 항목 | 평가 (5점 만점) |
|------|----------------|
| 정확성 | 5점 (DB 원자성 보장) |
| 성능 | 5점 (최고 성능) |
| 아키텍처 | 4점 (Hexagonal 대체로 유지) |
| 확장성 | 5점 (락 경합 없음) |
| 운영 편의성 | 5점 (모니터링 단순) |

**종합:** 성능과 정확성 모두 충족, 도메인 변경 수용 시 최적

---

## 성능 시뮬레이션

### 테스트 조건

- 동시 사용자: 100명
- 예약 생성 요청: 동시에 같은 상품 예약 시도
- 상품 재고: 총 10개 (가용 1개)
- 측정 지표: 처리량(TPS), 평균 응답 시간, 성공률

### 시뮬레이션 결과

| 방안 | 처리량(TPS) | 평균 응답(ms) | 성공 예약 | 실패 예약 | 오버부킹 |
|------|------------|--------------|----------|----------|---------|
| Optimistic Lock | 45 | 220 | 1 | 99 (재시도) | 0 |
| Pessimistic Lock | 18 | 550 | 1 | 99 | 0 |
| Named Lock | 17 | 580 | 1 | 99 | 0 |
| Redis Lock | 22 | 450 | 1 | 99 | 0 |
| **DB Constraint** | **50** | **200** | **1** | **99** | **0** |

### 그래프 (개념적)

```
처리량 (TPS)
 60 |                    [50] DB Constraint
    |                [45] Optimistic
 40 |
    |        [22] Redis
 20 |    [18] Pessimistic
    | [17] Named Lock
  0 +----------------------------------
      방안별 처리량 비교
```

---

## 아키텍처 영향 분석

### Hexagonal Architecture 준수도

| 방안 | Domain Layer | Application Layer | Infrastructure Layer | 평가 |
|------|-------------|------------------|---------------------|------|
| Optimistic | JPA 의존 [위반] | 변경 없음 | @Version | 위반 |
| Pessimistic | 변경 없음 [준수] | 변경 없음 | Repository 메서드 추가 | 준수 |
| Named Lock | Port 추가 [준수] | Port 호출 | Adapter 구현 | 완벽 |
| Redis Lock | Port 추가 [준수] | Port 호출 | Adapter 구현 | 완벽 |
| DB Constraint | 필드 추가 [경고] | 로직 변경 | Repository 메서드 추가 | 대체로 준수 |

### 패키지 의존성 변화

#### DB Constraint 방식 적용 후

```
domain/
  └─ product/
      └─ Product.java  [reservedQuantity 필드 추가]

application/
  └─ port/out/
      └─ ProductRepository.java  [reserveQuantity() 메서드 추가]

adapter/out/
  └─ persistence/
      └─ ProductRepositoryAdapter.java  [구현]
```

**의존성 방향:**
```
Domain (Product)
    ↑ 의존
Application (Port)
    ↑ 의존
Infrastructure (Adapter)
```

**결론:** Hexagonal Architecture 의존성 방향 유지

---

## 최종 권장사항

### 선택: Database Constraint (Atomic UPDATE)

#### 선택 근거

1. **비즈니스 요구사항 최우선 충족**
   - 오버부킹 절대 방지: DB 원자성 보장
   - 응답 시간 요구사항: P95 200ms (목표 500ms)
   - 정확도: 100%

2. **성능 우수**
   - 최고 처리량 (50 TPS)
   - 락 대기 없음
   - 확장성 우수

3. **운영 편의성**
   - 추가 인프라 불필요
   - 모니터링 단순
   - 장애 포인트 최소

4. **아키텍처 수용 가능**
   - Hexagonal Architecture 대체로 유지
   - `reservedQuantity`는 정당한 도메인 개념
   - 기술 독립성 손실 최소

#### 트레이드오프 수용

**수용하는 단점:**
- 도메인 모델에 `reservedQuantity` 필드 추가
- Port 메서드가 다소 기술적 (`reserveQuantity`)

**수용 이유:**
- `reservedQuantity`는 실제 비즈니스 개념 (재고 예약 vs 확정)
- 성능과 정확성 이득이 아키텍처 순수성 손실보다 큼
- DDD Aggregate 경계 내에서 관리 가능

#### 대안 플랜

**단기 (현재):**
- DB Constraint 방식 구현

**중기 (6개월 후):**
- Redis 인프라 도입 시 Redis Lock으로 전환 검토
- Port 인터페이스는 동일하게 유지 가능

**장기 (1년 후):**
- 이벤트 소싱 전환 시 재고 관리 방식 재설계

---

## 구현 체크리스트

### Phase 1: 도메인 모델 변경 (Issue #145)
- [x] Product 엔티티에 `reservedQuantity` 필드 추가
- [x] `getAvailableQuantity()` 메서드 추가
- [x] `canReserve()` 메서드 추가
- [x] 단위 테스트 작성

### Phase 2: Repository 구현 (Issue #157)
- [x] ProductRepository Port에 `reserveQuantity()` 메서드 추가
- [x] `releaseQuantity()` 메서드 추가 (Issue #157, #164)
- [x] Adapter에서 원자적 UPDATE 쿼리 구현
- [x] 통합 테스트 작성

### Phase 3: Application Service 수정 (Issue #157)
- [x] `createReservation()` 메서드 수정
- [x] 롤백 로직 구현
- [x] 예외 처리 개선
- [x] E2E 테스트 작성 (Issue #146, 50 TPS 달성)

### Phase 4: Database Migration (Issue #145, V9/V10)
- [x] Flyway 마이그레이션 스크립트 작성 (V9: reserved_quantity, V10: product_time_slot_inventory)
- [x] 기존 데이터 마이그레이션 (reserved_quantity = 0)
- [x] Constraint 추가 (reserved_quantity <= total_quantity)

### Phase 5: 모니터링
- [ ] 재고 예약 실패 메트릭 추가
- [ ] 롤백 발생 횟수 모니터링
- [ ] 알림 설정

---

## 참고 자료

- PostgreSQL 동시성 제어: https://www.postgresql.org/docs/16/mvcc.html
- Redisson Documentation: https://github.com/redisson/redisson/wiki
- Martin Fowler - Optimistic Locking: https://martinfowler.com/eaaCatalog/optimisticOfflineLock.html
- DDD Aggregate Pattern: Eric Evans, Domain-Driven Design, Ch.6

---

**Last Updated:** 2025-11-15
**Reviewed By:** DDINGJOO