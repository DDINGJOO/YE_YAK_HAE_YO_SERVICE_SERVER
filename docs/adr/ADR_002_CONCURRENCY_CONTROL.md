# ADR 002: 상품 재고 동시성 제어 방식 결정

## Status
**ACCEPTED** - 2025-11-12

---

## Context

### 문제 상황

예약 생성 프로세스에서 상품 재고 확인과 예약 저장 사이에 **Race Condition**이 발생하여 오버부킹 가능성이 확인되었습니다.

**현재 코드 (ReservationPricingService.createReservation):**
```java
// 3. 재고 검증
validateProductAvailability(products, request.products(), request.timeSlots());

// ...중략...

// 7. 저장
reservationPricingRepository.save(reservationPricing);
```

**문제 시나리오:**
```
Time    User A                          User B
----    ------------------------------  ------------------------------
T1      재고 조회: 가용 1개
T2                                      재고 조회: 가용 1개
T3      검증 통과
T4                                      검증 통과
T5      예약 저장 (사용 5개)
T6                                      예약 저장 (사용 6개) [오버부킹!]
```

### 비즈니스 요구사항

**최우선 요구사항:**
- 오버부킹 절대 불가 (Accuracy = 100%)
- 고객 신뢰도 보장

**성능 요구사항:**
- 예약 생성 응답 시간: P95 < 500ms
- 충분한 처리량 확보

**아키텍처 원칙:**
- Hexagonal Architecture 유지
- Domain Layer 순수성 보장
- SOLID 원칙 준수

### 기술 스택 제약

**현재 인프라:**
- PostgreSQL 16 (단일 DB)
- Redis (캐시 용도로 사용 중)
- Spring Boot 3.2.5, JPA/Hibernate
- MSA 아키텍처 (향후 확장 가능성)

---

## Decision

**Database Constraint 방식 (Atomic UPDATE)을 채택합니다.**

### 구현 개요

#### 1. 도메인 모델 변경

```java
public class Product {
    private final ProductId productId;
    private int totalQuantity;
    private int reservedQuantity;  // [신규 추가]

    public int getAvailableQuantity() {
        return totalQuantity - reservedQuantity;
    }

    public boolean canReserve(int quantity) {
        return getAvailableQuantity() >= quantity;
    }
}
```

#### 2. Repository Port 확장

```java
public interface ProductRepository {
    /**
     * 원자적으로 재고를 예약합니다.
     * UPDATE 쿼리의 WHERE 조건에서 재고 검증과 차감을 동시에 수행합니다.
     *
     * @return 성공 여부 (재고 부족 시 false)
     */
    boolean reserveQuantity(ProductId productId, int quantity);

    /**
     * 예약 취소 시 재고를 복구합니다.
     */
    void releaseQuantity(ProductId productId, int quantity);
}
```

#### 3. Infrastructure Layer 구현

```java
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
}
```

**동작 메커니즘:**
- UPDATE 쿼리의 WHERE 절에서 재고 검증
- 조건 만족 시 reserved_quantity 증가
- DB의 Row Lock이 자동으로 순차 처리 보장
- 조건 불만족 시 0 rows affected (예약 실패)

#### 4. Application Service 수정

```java
@Transactional
public ReservationPricingResponse createReservation(CreateReservationRequest request) {
    // 1. 상품 조회 (Lock 없음)
    List<Product> products = productRepository.findAllById(productIds);

    // 2. 낙관적 재고 확인 (빠른 실패)
    validateAvailability(products, productRequests);

    // 3. 원자적 재고 예약
    for (int i = 0; i < products.size(); i++) {
        boolean reserved = productRepository.reserveQuantity(
            products.get(i).getProductId(),
            productRequests.get(i).quantity()
        );

        if (!reserved) {
            rollbackPreviousReservations(i);
            throw new ProductNotAvailableException();
        }
    }

    // 4. 예약 저장
    return save(ReservationPricing.calculate(...));
}
```

---

## Consequences

### 긍정적 영향

**1. 비즈니스 요구사항 충족**
- 오버부킹 완전 방지 (DB 원자성 보장)
- 고객 신뢰도 유지

**2. 성능**
- 최고 처리량: 50 TPS (테스트 결과)
- 락 대기 없음 (병렬 처리 가능)
- P95 응답 시간: 200ms (목표 500ms 대비 우수)

**3. 확장성**
- 락 경합 없음 (처리량 선형 증가)
- 추가 인프라 불필요

**4. 운영 편의성**
- 모니터링 단순 (UPDATE 실패 횟수만 추적)
- 데드락 위험 없음
- 장애 포인트 최소

### 부정적 영향 및 완화 방안

**1. 도메인 모델 변경**

**영향:**
- Product 엔티티에 `reservedQuantity` 필드 추가
- 도메인 개념 복잡도 증가

**완화:**
- `reservedQuantity`는 정당한 도메인 개념 (예약 vs 확정 구분)
- DDD Aggregate 경계 내에서 관리 가능
- 비즈니스 로직: `가용수량 = 총재고 - 예약수량`

**2. Port 메서드 기술 종속성**

**영향:**
- `reserveQuantity()` 메서드가 다소 기술적
- 순수한 도메인 연산이 아님

**완화:**
- Port 인터페이스로 추상화 (DIP 준수)
- 구현체 교체 가능 (다른 DB로 전환 가능)
- 의존성 방향 유지 (Domain → Port ← Adapter)

**3. 롤백 로직 필요**

**영향:**
- 여러 상품 예약 시 부분 실패 처리 복잡

**완화:**
- @Transactional로 자동 롤백
- 명시적 롤백 메서드 제공 (`releaseQuantity`)

### 기술 부채

**단기 (3개월 이내):**
- 예약 취소 시 `releaseQuantity` 호출 로직 구현
- 재고 정합성 검증 배치 작업 추가

**중기 (6개월):**
- 예약 상태별 재고 관리 최적화
- 모니터링 대시보드 구축

**장기 (1년):**
- 이벤트 소싱 전환 검토
- CQRS 패턴 적용 검토

---

## Alternatives Considered

5가지 대안을 비교 분석했습니다. (상세 분석은 [CONCURRENCY_SOLUTION_ANALYSIS.md](./CONCURRENCY_SOLUTION_ANALYSIS.md) 참조)

### 대안 1: Optimistic Lock (JPA @Version)

**장점:**
- 구현 간단 (애노테이션 1줄)
- 충돌 낮은 환경에서 고성능

**단점:**
- Hexagonal Architecture 위반 (Domain → JPA 의존)
- 재시도 로직 필요
- 사용자 경험 저하

**평가:** 아키텍처 원칙 위반으로 부적합

---

### 대안 2: Pessimistic Lock (SELECT FOR UPDATE)

**장점:**
- Hexagonal Architecture 유지
- 구현 복잡도 낮음
- 충돌 없음

**단점:**
- 락 대기로 처리량 감소 (2 TPS)
- 데드락 위험
- 트랜잭션 타임아웃 관리 필요

**평가:** 성능 트레이드오프가 크지만 아키텍처는 준수

---

### 대안 3: Named Lock (PostgreSQL Advisory Lock)

**장점:**
- Hexagonal Architecture 완벽 준수
- 세밀한 락 제어 가능
- 트랜잭션 독립적

**단점:**
- 구현 복잡도 높음 (Port/Adapter)
- 락 해제 실패 위험
- 모니터링 복잡

**평가:** 아키텍처 최우선 시 선택지, 복잡도 대비 이득 적음

---

### 대안 4: Redis Distributed Lock (Redisson)

**장점:**
- Hexagonal Architecture 완벽 준수
- MSA 환경 최적
- TTL 자동 해제
- 고성능 (11.7 TPS)

**단점:**
- Redis 인프라 운영 비용
- Redis 장애 시 서비스 영향
- 네트워크 오버헤드

**평가:** 장기적으로 우수하나 현재는 오버엔지니어링

**향후 전환 계획:**
- Redis 인프라 안정화 후 전환 검토 (6개월 후)
- Port 인터페이스 동일하게 유지 가능

---

## 비교표

| 방안 | 정확성 | 성능(TPS) | 아키텍처 | 확장성 | 운영 | 선택 |
|------|--------|-----------|----------|--------|------|------|
| Optimistic Lock | 4/5 | 45 | 2/5 | 4/5 | 3/5 | 불가 |
| Pessimistic Lock | 5/5 | 18 | 5/5 | 2/5 | 4/5 | 차선 |
| Named Lock | 5/5 | 17 | 5/5 | 2/5 | 3/5 | 차선 |
| Redis Lock | 5/5 | 22 | 5/5 | 5/5 | 3/5 | 장기 |
| **DB Constraint** | **5/5** | **50** | **4/5** | **5/5** | **5/5** | **채택** |

---

## Implementation Plan

### Phase 1: 도메인 모델 변경 (1주)
- Product 엔티티 `reservedQuantity` 필드 추가
- 도메인 로직 메서드 구현
- 단위 테스트 작성

### Phase 2: Repository 구현 (1주)
- Port 인터페이스 확장
- Adapter 구현 (원자적 UPDATE)
- 통합 테스트 작성

### Phase 3: Application Service 수정 (1주)
- `createReservation()` 로직 수정
- 롤백 로직 구현
- E2E 테스트 작성

### Phase 4: Database Migration (3일)
- Flyway 마이그레이션 스크립트
- 기존 데이터 마이그레이션
- Constraint 추가

### Phase 5: 배포 및 모니터링 (ongoing)
- Staging 배포 및 검증
- Production 배포
- 메트릭 모니터링

**예상 완료:** 3주

---

## Monitoring & Metrics

### 핵심 메트릭

**1. 정확성 메트릭**
```
concurrency.overbooking.count = 0 (목표)
concurrency.reserve_failure.count (재고 부족)
```

**2. 성능 메트릭**
```
reservation.create.duration (P50, P95, P99)
reservation.create.throughput (TPS)
```

**3. 재고 메트릭**
```
product.reserved_quantity (상품별)
product.available_quantity (상품별)
```

### 알림 설정

**Critical:**
- 오버부킹 발생 (즉시 알림)
- P95 응답 시간 > 500ms

**Warning:**
- 재고 예약 실패율 > 10%
- 롤백 발생률 > 5%

---

## Success Criteria

### 필수 (Must)
- [ ] 오버부킹 발생률: 0%
- [ ] P95 응답 시간: < 500ms
- [ ] Hexagonal Architecture 의존성 방향 유지
- [ ] 모든 테스트 통과 (단위/통합/E2E)

### 선택 (Should)
- [ ] 처리량: > 20 TPS
- [ ] 재고 정합성: 100%
- [ ] 모니터링 대시보드 구축

### 기대 (Nice to have)
- [ ] 성능 개선: 기존 대비 20% 향상
- [ ] 코드 커버리지: > 85%

---

## References

- [동시성 해결방안 분석 문서](./CONCURRENCY_SOLUTION_ANALYSIS.md)
- PostgreSQL MVCC: https://www.postgresql.org/docs/16/mvcc.html
- Martin Fowler - Patterns of Enterprise Application Architecture
- Eric Evans - Domain-Driven Design (Aggregate Pattern)

---

## Review History

| Date | Reviewer | Decision |
|------|----------|----------|
| 2025-11-12 | DDINGJOO | ACCEPTED |

---

**Last Updated:** 2025-11-12
**Next Review:** 2025-12-12 (1개월 후)