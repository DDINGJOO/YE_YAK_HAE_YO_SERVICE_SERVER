# Product 기능

## 개요
예약 시 추가 가능한 상품(Product)을 관리하는 기능입니다. 플레이스(Place), 룸(Room), 예약(Reservation) 범위에서 사용 가능한 추가상품을 지원합니다.

## 목적
- 플레이스/룸/예약 범위별 상품 관리
- 다양한 가격 책정 방식 지원 (초기+추가, 1회 대여, 단순 재고)
- 재고 수량 관리
- 예약 시 상품별 정확한 가격 계산

## 주요 컴포넌트

### Domain Layer (Issue #11)
- **Product**: Aggregate Root
  - ProductId를 Aggregate ID로 사용
  - ProductScope(PLACE/ROOM/RESERVATION)별 상품 관리
  - PricingStrategy를 통한 가격 계산 로직
  - 재고 수량(totalQuantity) 관리
- **PricingStrategy**: Value Object
  - PricingType(INITIAL_PLUS_ADDITIONAL, ONE_TIME, SIMPLE_STOCK) 지원
  - Factory Method 패턴으로 생성
  - 수량에 따른 단가 계산
- **ProductPriceBreakdown**: Record (불변 객체)
  - 상품 가격 계산 결과 스냅샷
  - 단가, 수량, 총액 정보
  - 가격 일관성 검증

### Repository Layer (Issue #13)
- **ProductRepository**: Port (출력 포트)
  - findById, findByPlaceId, findByRoomId, findByScope 조회 메서드
  - save, deleteById, existsById CRUD 메서드
- **ProductRepositoryAdapter**: JPA Adapter
  - Domain Entity ↔ JPA Entity 변환 처리
- **ProductEntity**: JPA Entity
  - @Embedded를 사용한 PricingStrategy 매핑
  - Scope별 제약조건 (DB 레벨)
- **Flyway Migration**: V3__create_products_table.sql
  - products 테이블 생성
  - Scope 및 PricingType 제약조건
  - 성능 최적화 인덱스

### Application Layer (Issue #14)
- **Use Case Ports**: 입력 포트 (4개)
  - RegisterProductUseCase: 상품 등록
  - GetProductUseCase: 상품 조회 (ID, PlaceId, RoomId, Scope)
  - UpdateProductUseCase: 상품 수정
  - DeleteProductUseCase: 상품 삭제
- **Application Services**: Use Case 구현 (4개)
  - RegisterProductService: 상품 등록 로직, @Transactional
  - GetProductService: 다양한 조건 조회, @Transactional(readOnly)
  - UpdateProductService: 상품 정보 업데이트
  - DeleteProductService: 상품 삭제 및 존재 검증
- **DTOs**: Request/Response 변환 (4개)
  - PricingStrategyDto: 가격 전략 DTO (toDomain/from 메서드)
  - RegisterProductRequest: 상품 등록 요청 (Scope별 ID 검증)
  - UpdateProductRequest: 상품 수정 요청
  - ProductResponse: 상품 응답 DTO

### API Layer (Issue #14)
- **ProductController**: REST Controller
  - POST /api/products: 상품 등록 (201 Created)
  - GET /api/products/{id}: 상품 조회 (200 OK)
  - GET /api/products: 목록 조회 (필터링: scope, placeId, roomId)
  - PUT /api/products/{id}: 상품 수정 (200 OK)
  - DELETE /api/products/{id}: 상품 삭제 (204 No Content)
- **GlobalExceptionHandler**: 예외 처리
  - NoSuchElementException → 404 NOT_FOUND
  - ConstraintViolationException → 400 BAD_REQUEST
  - MethodArgumentNotValidException → 400 BAD_REQUEST

## 관련 문서
- [도메인 모델](./domain.md)
- [데이터베이스 스키마](./database.md)
- [API 문서](./API.md)

## 관련 Issues
- Issue #11: Product Aggregate 구현 (완료)
- Issue #13: Product Repository 및 영속성 구현 (완료)
- Issue #14: Product API 구현 (완료)
- Epic #77: 룸별 상품 관리 시스템 (진행 중)
- Story #78: 룸별 허용 상품 설정 기능 (진행 중) 

## Product Scope
Product는 3가지 범위(Scope)로 구분됩니다:

### PLACE (플레이스 전체)
- 플레이스 내 룸에서 사용 가능
- PlaceId 필수, RoomId는 null
- 예시: 공용 빔 프로젝터, 공용 화이트보드
- **중요**: 룸별 허용 목록 설정 가능 (Epic #77)
  - 화이트리스트 방식: 허용된 룸에서만 사용 가능
  - 매핑 없으면 해당 룸에서 사용 불가

### ROOM (특정 룸)
- 특정 룸에서만 사용 가능
- PlaceId와 RoomId 모두 필수
- 예시: 룸 전용 장비, 룸 전용 비품

### RESERVATION (예약 단위)
- 모든 예약에서 사용 가능
- PlaceId와 RoomId 모두 null
- 예시: 음료수, 간식, 일회용품

## Pricing Type
Product는 3가지 가격 책정 방식을 지원합니다:

### INITIAL_PLUS_ADDITIONAL (초기 + 추가)
- 첫 개는 초기 가격, 그 이후는 추가 가격
- 예시: 첫 개 10,000원, 추가 개당 5,000원
- 사용 사례: 빔 프로젝터 (첫 대여는 설치비 포함)

### ONE_TIME (1회 대여)
- 수량과 무관하게 1회 가격만 적용
- 예시: 10,000원 (수량 상관없이)
- 사용 사례: 회의실 청소 서비스

### SIMPLE_STOCK (단순 재고)
- 개당 일정한 단가
- 예시: 개당 2,000원
- 사용 사례: 음료수, 간식

## 가격 계산 예시

### INITIAL_PLUS_ADDITIONAL
```
초기 가격: 10,000원
추가 가격: 5,000원
수량: 3개

계산:
- 1개: 10,000원 (초기)
- 2개: 10,000 + 5,000 = 15,000원
- 3개: 10,000 + 5,000 + 5,000 = 20,000원
```

### ONE_TIME
```
1회 가격: 15,000원
수량: 3개

계산:
- 수량과 무관하게 15,000원
```

### SIMPLE_STOCK
```
단가: 2,000원
수량: 5개

계산:
- 5개: 2,000 × 5 = 10,000원
```

## 비즈니스 규칙

### 1. Scope 검증
- PLACE: PlaceId 필수, RoomId null
- ROOM: PlaceId와 RoomId 모두 필수
- RESERVATION: PlaceId와 RoomId 모두 null

### 2. PricingStrategy 검증
- INITIAL_PLUS_ADDITIONAL: initialPrice와 additionalPrice 모두 필수
- ONE_TIME: initialPrice만 필수, additionalPrice null
- SIMPLE_STOCK: initialPrice만 필수, additionalPrice null

### 3. 수량 관리
- totalQuantity는 0 이상이어야 함
- 재고 차감 로직은 별도 UseCase에서 관리 (추후 구현)

### 4. 가격 계산
- ProductPriceBreakdown을 통해 불변 스냅샷 생성
- 단가 × 수량 = 총액 일관성 검증
- 예약 시점 가격을 스냅샷으로 저장하여 가격 정책 변경에도 불변성 보장

## 테스트 커버리지

### Domain Layer (Issue #11)
- **ProductTest**: 67개 테스트
  - 생성자 및 Factory Method 테스트
  - Scope별 검증 테스트
  - 업데이트 메서드 테스트
  - 가격 계산 테스트
- **PricingStrategyTest**: PricingStrategy Value Object 테스트
- **ProductPriceBreakdownTest**: 가격 스냅샷 일관성 테스트

### Repository Layer (Issue #13)
- **ProductRepositoryAdapterTest**: 13개 통합 테스트
  - CRUD 동작 테스트 (3가지 Scope별)
  - Scope별 조회 테스트 (findByPlaceId, findByRoomId, findByScope)
  - PricingStrategy 저장/조회 테스트 (3가지 타입별)

### API Layer (Issue #14)
- **ProductControllerTest**: 16개 통합 테스트
  - POST /api/products: 3가지 Scope 등록, Validation 테스트
  - GET /api/products/{id}: 조회 성공/실패 테스트
  - GET /api/products: 필터링 조회 테스트 (scope, placeId, roomId)
  - PUT /api/products/{id}: 수정 성공/실패 테스트
  - DELETE /api/products/{id}: 삭제 성공/실패 테스트

**총 테스트: 96개 케이스**
**테스트 커버리지: Domain 100%, Repository 100%, API 100%**

## 룸별 상품 허용 관리 (Epic #77)

### 개요
플레이스 어드민이 룸별로 PLACE Scope 상품의 허용 목록을 관리할 수 있는 기능입니다.

### 정책: 화이트리스트 방식
**"매핑이 없으면 PLACE 상품은 표시되지 않음 (명시적 허용만)"**

### 예시
```
플레이스 100의 PLACE 상품:
- Product 1: 빔프로젝터
- Product 2: 화이트보드
- Product 3: 아메리카노
- Product 4: 과자

룸 1 설정: [Product 3, 4] (음료/간식만 허용)
→ 룸 1 예약 시 상품 조회: 아메리카노, 과자만 표시

룸 2 설정: [] (매핑 없음)
→ 룸 2 예약 시 상품 조회: PLACE 상품 모두 제외

룸 3 설정: [Product 1, 2, 3, 4] (모두 허용)
→ 룸 3 예약 시 상품 조회: 모든 PLACE 상품 표시
```

### 관리자 API
```
POST   /api/admin/rooms/{roomId}/allowed-products
GET    /api/admin/rooms/{roomId}/allowed-products
DELETE /api/admin/rooms/{roomId}/allowed-products
```

### 영향받는 API
- `GET /api/products/availability`: PLACE 상품 필터링 적용

자세한 내용은 [도메인 모델](./domain.md#1-룸별-상품-허용-규칙-epic-77) 참고

---

## 향후 계획
- Epic #77: 룸별 상품 관리 시스템 (진행 중)
  - Story #78: 룸별 허용 상품 설정 기능
  - Task #79-83: 각 계층별 구현
  - CR #84: ProductAvailabilityService 필터링 추가
- Issue #12: ProductAvailabilityService 완성 (Place/Room Scope)
  - Issue #15 (ReservationPricing 도메인) 완료 후 구현
- Issue #17: 예약 가격 계산 Application Service 구현
- Issue #18: 예약 가격 관리 API 구현
