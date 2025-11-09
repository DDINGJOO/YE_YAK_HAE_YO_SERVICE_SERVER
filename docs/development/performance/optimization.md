# 성능 최적화 가이드

## 문서 개요

**목적**: 예약 가격 관리 서비스의 N+1 쿼리 문제 및 성능 병목 현상에 대한 체계적인 분석 및 해결 계획

**대상**: 백엔드 개발자, 아키텍트, 기술 리더

**최종 업데이트**: 2025-11-09

**상태**: 계획 단계

---

## 목차

1. [요약](#요약)
2. [식별된 문제점](#식별된-문제점)
3. [해결 방안](#해결-방안)
4. [구현 로드맵](#구현-로드맵)
5. [기술 사양](#기술-사양)
6. [리스크 평가](#리스크-평가)
7. [성능 지표](#성능-지표)

---

## 요약

### 현재 상황

코드베이스에 대한 성능 병목 현상 감사가 완료되었습니다. 아키텍처는 Hexagonal/DDD 원칙을 훌륭하게 따르고 있지만, 부하 상황에서 성능에 큰 영향을 미칠 수 있는 여러 N+1 쿼리 패턴이 식별되었습니다.

### 영향 평가

- **심각도**: 치명적
- **영향 받는 컴포넌트**: PricingPolicy, ReservationPricing, Product 리포지토리
- **예상 성능 저하**: 프로덕션 부하 상황에서 쿼리 40-70% 느려짐
- **사용자 영향**: 예약 생성 및 가격 조회 시 응답 시간 증가

### 제안 솔루션

여러 최적화 기법을 결합한 단계별 구현:
- Phase 1: Batch Size 최적화 (낮은 위험, 즉각적인 효과)
- Phase 2: 일반 쿼리를 위한 JPQL Fetch Join (중간 위험, 높은 효과)
- Phase 3: 복잡한 쿼리를 위한 QueryDSL 통합 (초기 비용 높음, 장기적 이득)

---

## 식별된 문제점

### 문제 1: PricingPolicyEntity EAGER 로딩

**위치**: `springProject/src/main/java/com/teambind/springproject/adapter/out/persistence/pricingpolicy/PricingPolicyEntity.java:47`

**코드**:
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "time_range_prices", ...)
private List<TimeRangePriceEmbeddable> timeRangePrices = new ArrayList<>();
```

**문제**:
- 필요하지 않을 때도 항상 time_range_prices 테이블을 로드
- 여러 가격 정책을 조회할 때 N+1 쿼리 발생
- 데이터베이스 조인이 무조건적으로 발생

**영향**: 높음
- 영향 대상: 모든 가격 정책 쿼리
- 쿼리 배수: N+1 (N = 정책 개수)
- 메모리 오버헤드: 모든 시간대 가격을 힙에 로드

**제안 수정 사항**: 선택적 FETCH JOIN과 함께 LAZY로 전환
**예상 개선**: 40-60% 쿼리 감소

---

### 문제 2: ReservationPricingEntity 다중 EAGER 컬렉션

**위치**: `springProject/src/main/java/com/teambind/springproject/adapter/out/persistence/reservationpricing/ReservationPricingEntity.java:62,71`

**코드**:
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "reservation_pricing_slots", ...)
private Map<LocalDateTime, BigDecimal> slotPrices = new HashMap<>();

@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "reservation_pricing_products", ...)
private List<ProductPriceBreakdownEmbeddable> productBreakdowns = new ArrayList<>();
```

**문제**:
- 두 개의 EAGER ElementCollection이 카르테시안 곱 위험 생성
- 사용 사례와 관계없이 항상 두 컬렉션을 로드
- 향후 MultipleBagFetchException 가능성

**영향**: 치명적
- 영향 대상: 모든 예약 쿼리
- 쿼리 배수: 1 + 2*N
- 메모리 오버헤드: 큰 예약 목록에서 상당함

**제안 수정 사항**: LAZY + BatchSize + 선택적 FETCH JOIN
**예상 개선**: 50-80% 쿼리 감소

---

### 문제 3: ProductAvailabilityService 인메모리 처리

**위치**: `springProject/src/main/java/com/teambind/springproject/domain/product/ProductAvailabilityService.java:103-115,137-150`

**코드**:
```java
final List<ReservationPricing> overlappingReservations =
    repository.findByPlaceIdAndTimeRange(...);

final int maxUsedQuantity = requestedSlots.stream()
    .mapToInt(slot -> calculateUsedAtSlot(overlappingReservations, productId, slot))
    .max()
    .orElse(0);
```

**문제**:
- 겹치는 모든 예약을 메모리로 로드
- O(n*m) 복잡도: requestedSlots * reservations 반복
- 대용량 데이터셋에 비효율적

**영향**: 높음
- 영향 대상: 예약 시 상품 재고 확인
- 복잡도: O(n*m) (n=슬롯 수, m=예약 수)
- 메모리: 전체 예약 목록 로드

**제안 수정 사항**: 데이터베이스 수준 집계 쿼리
**예상 개선**: 60-90% 성능 향상

---

### 문제 4: ReservationPricingService 루프 쿼리

**위치**: `springProject/src/main/java/com/teambind/springproject/application/service/reservationpricing/ReservationPricingService.java:138-146`

**코드**:
```java
private List<Product> fetchProducts(final List<ProductRequest> productRequests) {
    final List<Product> products = new ArrayList<>();
    for (final ProductRequest productRequest : productRequests) {
        final Product product = productRepository.findById(...)
            .orElseThrow(...);
        products.add(product);
    }
    return products;
}
```

**문제**:
- N개 상품에 대한 N개의 별도 데이터베이스 쿼리
- 고전적인 N+1 패턴
- 비효율적인 트랜잭션 사용

**영향**: 중간
- 영향 대상: 여러 상품을 포함한 예약 생성
- 쿼리 배수: N+1 (N = 상품 개수)
- 네트워크 왕복: N

**제안 수정 사항**: findAllById를 사용한 배치 쿼리
**예상 개선**: 다중 상품 예약에서 30-50% 빠름

---

### 문제 5: 복합 인덱스 누락

**위치**: `springProject/src/main/resources/db/migration/V4__create_reservation_pricing_tables.sql`

**문제**:
- 단일 컬럼 인덱스만 존재
- 쿼리 패턴이 다중 컬럼 WHERE 절 사용
- 최적화되지 않은 쿼리 실행 계획

**영향**: 중간
- 영향 대상: 시간 범위 및 상태 기반 쿼리
- 인덱스 사용: 부분적 또는 순차 스캔
- 데이터베이스 부하: I/O 증가

**제안 수정 사항**: 복합 인덱스 추가
**예상 개선**: 범위 쿼리 50-80% 빠름

---

## 해결 방안

### 방안 1: Batch Size 최적화

**설명**: Hibernate 배치 페칭 구성으로 N+1을 N/batch_size + 1로 감소

**구현**:
```java
// Entity 레벨
@ElementCollection(fetch = FetchType.LAZY)
@BatchSize(size = 10)
private List<TimeRangePriceEmbeddable> timeRangePrices;

// 전역 설정 (application.yml)
spring.jpa.properties.hibernate.default_batch_fetch_size: 100
```

**장점**:
- 최소한의 코드 변경
- 추가 의존성 없음
- 투명한 최적화
- 되돌리기 가능

**단점**:
- 완전한 솔루션 아님 (여전히 다중 쿼리)
- LAZY 전환 필요 (LazyInitializationException 위험)
- 큰 배치 사이즈로 인한 메모리 소비

**SOLID 준수**: 완전 준수, 아키텍처 변경 없음

**위험 수준**: 낮음

**작업량**: 1-2시간

**우선순위**: 즉시

---

### 방안 2: JPQL Fetch Join

**설명**: JPQL 쿼리에서 명시적 JOIN FETCH

**구현**:
```java
@Query("SELECT p FROM PricingPolicyEntity p " +
       "LEFT JOIN FETCH p.timeRangePrices " +
       "WHERE p.roomId = :roomId")
Optional<PricingPolicyEntity> findByIdWithTimeRangePrices(@Param("roomId") RoomIdEmbeddable roomId);
```

**장점**:
- JPA 표준, 의존성 없음
- 단일 쿼리 솔루션
- 명시적 제어
- 이해하기 쉬움

**단점**:
- 문자열 기반 쿼리 (컴파일 타임 검사 없음)
- 다중 컬렉션에서 MultipleBagFetchException
- 컬렉션 조인 시 DISTINCT 필요
- 수동 쿼리 작성

**SOLID 준수**: 완전 준수

**위험 수준**: 낮음-중간

**작업량**: 1-2일

**우선순위**: 단기

---

### 방안 3: EntityGraph

**설명**: 동적 페치 전략을 위한 JPA EntityGraph

**구현**:
```java
@EntityGraph(attributePaths = {"timeRangePrices"})
Optional<PricingPolicyEntity> findWithTimeRangePricesById(RoomIdEmbeddable id);
```

**장점**:
- JPA 표준
- 선언적 접근
- 재사용 가능한 그래프
- 기본 페치 계획 오버라이드

**단점**:
- 페치 메타데이터로 인한 엔티티 오염
- JPQL보다 명시성 낮음
- Entity와 Repository 간 분산된 설정
- 여전히 카르테시안 곱 가능

**SOLID 준수**: 경미한 SRP 우려 (Entity가 페칭을 알게됨)

**위험 수준**: 중간

**작업량**: 1-2일

**우선순위**: 선택적

---

### 방안 4: QueryDSL

**설명**: 완전한 제어를 가진 타입 안전 쿼리 빌더

**구현**:
```java
public Optional<PricingPolicy> findByIdWithTimeRangePrices(RoomId roomId) {
    QPricingPolicyEntity policy = QPricingPolicyEntity.pricingPolicyEntity;

    PricingPolicyEntity entity = queryFactory
        .selectFrom(policy)
        .leftJoin(policy.timeRangePrices).fetchJoin()
        .where(policy.roomId.value.eq(roomId.getValue()))
        .fetchOne();

    return Optional.ofNullable(entity).map(PricingPolicyEntity::toDomain);
}
```

**장점**:
- 컴파일 타임 타입 안정성
- IDE 리팩토링 지원
- 동적 쿼리 빌딩
- 복잡한 쿼리 지원
- 다단계 페치를 통한 MultipleBagFetchException 회피 가능
- Custom Repository 패턴을 통한 깔끔한 분리

**단점**:
- 추가 의존성 (querydsl-jpa)
- 빌드 시 Q-class 생성 단계
- 학습 곡선
- Custom repository 보일러플레이트

**SOLID 준수**: 우수 (깔끔한 repository 인터페이스 촉진)

**위험 수준**: 중간 (초기 설정), 낮음 (설정 후)

**작업량**: 1주 (초기 설정 + 학습), 이후 더 빠름

**우선순위**: 전략적 (장기 투자)

---

### 방안 5: 데이터베이스 집계

**설명**: 계산을 데이터베이스 계층으로 푸시

**구현**:
```java
@Query("SELECT COALESCE(MAX(usedQty), 0) FROM (" +
       "  SELECT SUM(pb.quantity) as usedQty " +
       "  FROM ReservationPricingEntity r " +
       "  JOIN r.productBreakdowns pb " +
       "  WHERE r.placeId = :placeId " +
       "  AND r.status IN :statuses " +
       "  AND pb.productId = :productId " +
       "  GROUP BY r.id" +
       ") subquery")
Integer findMaxUsedQuantity(...);
```

**장점**:
- 최소 메모리 사용
- 데이터베이스 최적화 계산
- 인메모리 처리 제거
- 대용량 데이터셋에 확장 가능

**단점**:
- 복잡한 SQL/JPQL
- 데이터베이스별 최적화
- 이식성 낮음
- 테스트 어려움

**SOLID 준수**: 허용 가능 (Repository 책임)

**위험 수준**: 중간

**작업량**: 2-3일

**우선순위**: 중기

---

### 방안 6: DTO Projection

**설명**: Entity 매핑을 우회하여 DTO로 직접 쿼리

**구현**:
```java
@Query("SELECT new com.teambind...PricingPolicyDto(" +
       "p.roomId, p.placeId, p.timeSlot, p.defaultPrice) " +
       "FROM PricingPolicyEntity p")
List<PricingPolicyDto> findAllSummary();
```

**장점**:
- 최대 성능 (필요한 컬럼만)
- 최소 네트워크 전송
- 명확한 의도 분리 (읽기 전용)

**단점**:
- 코드 중복 (Entity + DTO)
- 유지보수 오버헤드
- 주의하지 않으면 Hexagonal 경계 위반 가능
- 중첩 구조에 대한 복잡한 매핑

**SOLID 준수**: SRP 이점, 하지만 DTO가 도메인으로 누출되면 DIP 위반 가능

**위험 수준**: 중간

**작업량**: 사용 사례당 2-3일

**우선순위**: 선택적 (특별한 읽기 엔드포인트만)

---

### 방안 7: 복합 인덱스

**설명**: 쿼리 패턴에 맞춘 다중 컬럼 데이터베이스 인덱스

**구현**:
```sql
-- V6__add_composite_indexes.sql
CREATE INDEX idx_reservation_pricings_place_time_status
    ON reservation_pricings (place_id, calculated_at, status);

CREATE INDEX idx_reservation_pricings_room_time_status
    ON reservation_pricings (room_id, calculated_at, status);

CREATE INDEX idx_reservation_pricing_slots_composite
    ON reservation_pricing_slots (reservation_id, slot_time);
```

**장점**:
- 대규모 쿼리 속도 향상 (50-80%)
- 코드 변경 없음
- 데이터베이스 수준 최적화
- 간단한 롤백

**단점**:
- 디스크 공간 증가
- 쓰기 성능 약간 저하
- 인덱스 유지보수 오버헤드

**SOLID 준수**: N/A (인프라)

**위험 수준**: 낮음

**작업량**: 1-2시간

**우선순위**: 즉시

---

## 구현 로드맵

### Phase 1: 빠른 성과 (1주차)

**목표**: 최소 위험으로 40-50% 성능 개선 달성

**작업**:
1. 전역 배치 페치 사이즈 활성화
2. 모든 EAGER를 LAZY로 전환
3. 복합 데이터베이스 인덱스 추가
4. ReservationPricingService.fetchProducts에 배치 쿼리 구현

**산출물**:
- 마이그레이션 파일 V6__add_composite_indexes.sql
- 배치 사이즈가 포함된 업데이트된 application.yml
- 리팩토링된 Entity 클래스 (LAZY 전환)
- 리팩토링된 fetchProducts 메서드

**성공 기준**:
- 모든 테스트 통과
- 통합 테스트에서 LazyInitializationException 없음
- 쿼리 수 최소 40% 감소

**담당자**: Backend Team

**관련 이슈**: TBD (Epic + Tasks)

---

### Phase 2: JPQL 최적화 (2-3주차)

**목표**: 중요 경로에서 N+1 제거

**작업**:
1. PricingPolicyJpaRepository에 FETCH JOIN 쿼리 추가
2. ReservationPricingJpaRepository에 FETCH JOIN 쿼리 추가
3. ProductJpaRepository에 FETCH JOIN 쿼리 추가
4. 최적화된 쿼리를 사용하도록 Repository Adapter 업데이트
5. @Transactional 경계로 Service 레이어 업데이트

**산출물**:
- fetch join 메서드가 포함된 향상된 Repository 인터페이스
- 업데이트된 Adapter 구현
- 쿼리 수를 검증하는 통합 테스트

**성공 기준**:
- 시간 범위를 포함한 가격 정책 조회 시 단일 쿼리
- 슬롯 및 상품을 포함한 예약 가격 조회 시 단일 쿼리
- 쿼리 수 최소 70% 감소

**담당자**: Backend Team

**관련 이슈**: TBD (Story + Tasks)

---

### Phase 3: QueryDSL 통합 (4-5주차)

**목표**: 장기적인 타입 안전 쿼리 기반 확립

**작업**:
1. build.gradle에 QueryDSL 의존성 추가
2. Q-class 생성 구성
3. 기본 QueryDSL 설정 생성
4. QueryDSL을 사용한 Custom Repository 구현
5. ProductAvailabilityService에서 복잡한 쿼리 마이그레이션
6. 검색 엔드포인트를 위한 동적 쿼리 지원 추가

**산출물**:
- QueryDSL 설정
- 생성된 Q-class
- Custom Repository 구현
- 마이그레이션된 ProductAvailabilityService 로직
- QueryDSL 사용을 위한 개발자 문서

**성공 기준**:
- 모든 복잡한 쿼리가 QueryDSL 사용
- 컴파일 타임 쿼리 검증
- 쿼리 성능이 JPQL과 동등하거나 초과
- 팀의 QueryDSL 기본 숙련도

**담당자**: Backend Team + 학습 세션

**관련 이슈**: TBD (Story + Tasks)

---

### Phase 4: 고급 최적화 (6주차+)

**목표**: 나머지 엣지 케이스 및 모니터링 처리

**작업**:
1. ProductAvailabilityService를 위한 데이터베이스 집계 쿼리 구현
2. 요약/목록 엔드포인트를 위한 DTO projection 추가
3. 쿼리 성능 모니터링 설정
4. 느린 쿼리 알림 추가
5. 최적화 패턴 문서화

**산출물**:
- 집계 쿼리 구현
- 목록 엔드포인트를 위한 DTO projection
- 성능 모니터링 대시보드
- 느린 쿼리 알림 설정
- 학습 사항이 반영된 업데이트된 PERFORMANCE_OPTIMIZATION.md

**성공 기준**:
- 모든 재고 확인이 데이터베이스 집계 사용
- 목록 엔드포인트가 DTO projection 사용
- 95 백분위수 쿼리 시간 100ms 미만
- 프로덕션 로그에서 N+1 쿼리 제로

**담당자**: Backend Team

**관련 이슈**: TBD (Tasks)

---

## 기술 사양

### 환경 요구사항

- Java 21
- Spring Boot 3.2.5
- Spring Data JPA
- PostgreSQL 16
- Hibernate 6.x

### 추가할 의존성

```gradle
// Phase 3: QueryDSL
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
```

### 설정 변경

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
        format_sql: true
        use_sql_comments: true
    show-sql: false

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

---

## 리스크 평가

### 기술적 리스크

**리스크 1: LazyInitializationException**
- 발생 가능성: 중간
- 영향: 높음
- 완화 방안: 포괄적인 통합 테스트, 신중한 @Transactional 경계
- 대응책: 특정 사용 사례에 대한 선택적 EAGER 폴백

**리스크 2: MultipleBagFetchException**
- 발생 가능성: 낮음 (적절한 JPQL/QueryDSL 사용 시)
- 영향: 중간
- 완화 방안: QueryDSL 다단계 페치 또는 @BatchSize 사용
- 대응책: 여러 쿼리로 분할

**리스크 3: 카르테시안 곱**
- 발생 가능성: 낮음
- 영향: 높음
- 완화 방안: JPQL의 DISTINCT, QueryDSL 쿼리 검사
- 대응책: 쿼리 실행 계획 모니터링

**리스크 4: 팀 학습 곡선 (QueryDSL)**
- 발생 가능성: 중간
- 영향: 낮음
- 완화 방안: 팀 교육 세션, 페어 프로그래밍
- 대응책: 점진적 도입, JPQL 폴백

---

### 비즈니스 리스크

**리스크 1: 개발 시간 증가**
- 발생 가능성: 중간
- 영향: 중간
- 완화 방안: 단계적 접근, 병렬 작업 스트림
- 대응책: Phase 2가 요구사항을 충족하면 Phase 3 연기

**리스크 2: 회귀 버그**
- 발생 가능성: 낮음 (포괄적 테스트 시)
- 영향: 높음
- 완화 방안: 광범위한 통합 테스트, 스테이징 검증
- 대응책: 기능 플래그, 빠른 롤백

---

## 성능 지표

### 기준선 (현재 상태)

**PricingPolicy 쿼리**:
- 쿼리 수: 1 (policy) + N (time_range_prices) = N+1
- 평균 시간: ~50ms (10개 시간 범위를 가진 정책 1개)
- 최악의 경우: ~500ms (정책 10개)

**ReservationPricing 쿼리**:
- 쿼리 수: 1 (pricing) + N (slots) + N (products) = 2N+1
- 평균 시간: ~80ms (예약 1개)
- 최악의 경우: ~800ms (예약 10개)

**Product 재고 확인**:
- 쿼리 수: 1 (reservations) + 인메모리 처리
- 평균 시간: ~100ms (겹치는 예약 10개)
- 최악의 경우: ~1000ms (예약 100개)

**예약 생성**:
- 쿼리 수: 1 (policy) + N (products) + 1 (save) = N+2
- 평균 시간: ~150ms (상품 3개)
- 최악의 경우: ~500ms (상품 10개)

---

### 목표 (최적화 후)

**PricingPolicy 쿼리**:
- 쿼리 수: 1 (FETCH JOIN 사용)
- 평균 시간: ~20ms (60% 개선)
- 최악의 경우: ~100ms (80% 개선)

**ReservationPricing 쿼리**:
- 쿼리 수: 1 또는 2 (QueryDSL 다단계 사용)
- 평균 시간: ~30ms (62% 개선)
- 최악의 경우: ~200ms (75% 개선)

**Product 재고 확인**:
- 쿼리 수: 1 (데이터베이스 집계)
- 평균 시간: ~20ms (80% 개선)
- 최악의 경우: ~100ms (90% 개선)

**예약 생성**:
- 쿼리 수: 2 (policy + 배치 products) + 1 (save) = 3
- 평균 시간: ~60ms (60% 개선)
- 최악의 경우: ~150ms (70% 개선)

---

### 모니터링 계획

**주요 지표**:
- 요청당 쿼리 수
- 쿼리 실행 시간 (p50, p95, p99)
- 데이터베이스 연결 풀 사용량
- 메모리 힙 사용량

**알림 임계값**:
- 요청당 쿼리 수 > 10: 경고
- 쿼리 실행 시간 p95 > 200ms: 경고
- 쿼리 실행 시간 p99 > 500ms: 치명적

**도구**:
- Spring Boot Actuator
- Hibernate Statistics
- PostgreSQL pg_stat_statements
- APM (사용 가능한 경우)

---

## 참고 자료

### 내부 문서

- [Architecture Decision Records](../../adr/)
- [Hexagonal Architecture Guide](../../architecture/)
- [Issue Management Guide](../../ISSUE_GUIDE.md)

### 외부 리소스

- [Hibernate Performance Tuning](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#performance)
- [QueryDSL Reference](http://querydsl.com/static/querydsl/latest/reference/html/)
- [JPA Best Practices](https://vladmihalcea.com/tutorials/hibernate/)

---

## 부록 A: 쿼리 분석 예제

### 예제 1: PricingPolicy N+1

**현재 동작**:
```sql
-- Query 1: 정책 로드
SELECT * FROM pricing_policies WHERE room_id = 1;

-- Query 2: 시간 범위 로드
SELECT * FROM time_range_prices WHERE room_id = 1;
```

**FETCH JOIN 적용 후**:
```sql
-- 단일 쿼리
SELECT p.*, trp.*
FROM pricing_policies p
LEFT JOIN time_range_prices trp ON p.room_id = trp.room_id
WHERE p.room_id = 1;
```

---

### 예제 2: Product 배치 로딩

**현재 동작**:
```sql
SELECT * FROM products WHERE product_id = 1;
SELECT * FROM products WHERE product_id = 2;
SELECT * FROM products WHERE product_id = 3;
-- N개의 쿼리
```

**배치 쿼리 적용 후**:
```sql
SELECT * FROM products WHERE product_id IN (1, 2, 3);
-- 단일 쿼리
```

---

## 부록 B: 테스트 전략

### 단위 테스트

- Repository 메서드가 예상 데이터 반환
- 쿼리 메서드가 null/empty 케이스 처리
- 도메인 로직 변경 없음

### 통합 테스트

- Hibernate Statistics로 쿼리 수 검증
- @Transactional 경계 테스트
- LazyInitializationException 발생 안함 검증

### 성능 테스트

- 기준선 vs 최적화 버전에 대한 JMeter 시나리오
- 쿼리 실행 계획 비교
- 메모리 프로파일링

### 회귀 테스트

- 기존 기능 변경 없음
- API 계약 유지
- 도메인 불변성 유지

---

## 변경 이력

| 날짜 | 작성자 | 변경 사항 |
|------|--------|---------|
| 2025-11-09 | Backend Team | 초기 문서 생성 |

---

문서 끝
