# YE YAK HAE YO Service Server

예약 및 가격 관리 시스템

## 프로젝트 개요

본 프로젝트는 공간 예약 서비스의 가격 정책 및 추가상품 관리를 담당하는 마이크로서비스입니다.

**주요 기능**:
- 룸별 가격 정책 관리
- 시간대별 차등 가격 설정
- 추가상품 관리 (예정)
- 예약 가격 계산 (예정)

**아키텍처**: Hexagonal Architecture (Ports & Adapters)
**설계 원칙**: Domain-Driven Design (DDD)

## 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Database**: PostgreSQL
- **Migration**: Flyway
- **Messaging**: Kafka
- **Build Tool**: Gradle 8.14.3
- **Testing**: JUnit 5, Mockito, AssertJ

## 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose (PostgreSQL, Kafka 실행용)
- Gradle 8.14.3+

### 환경 설정

1. 저장소 클론
```bash
git clone https://github.com/DDINGJOO/YE_YAK_HAE_YO_SERVICE_SERVER.git
cd YE_YAK_HAE_YO_SERVICE_SERVER/springProject
```

2. Docker Compose로 인프라 실행
```bash
docker-compose up -d
```

3. 애플리케이션 빌드 및 실행
```bash
./gradlew clean build
./gradlew bootRun
```

4. API 접속 확인
```bash
curl http://localhost:8080/actuator/health
```

### 환경 변수

`application.yml`에서 설정 가능:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pricing_db
    username: postgres
    password: postgres

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: pricing-policy-service
```

## 프로젝트 구조

```
src/main/java/com/teambind/springproject/
├── domain/                    # 도메인 계층
│   ├── pricingpolicy/        # 가격 정책 Aggregate
│   └── shared/               # 공유 Value Objects
├── application/              # 애플리케이션 계층
│   ├── port/in/              # Use Case 인터페이스
│   ├── port/out/             # Repository 인터페이스
│   ├── service/              # Use Case 구현
│   └── dto/                  # DTO
├── adapter/                  # 어댑터 계층
│   ├── in/web/              # REST Controller
│   ├── in/messaging/        # Event Listener
│   └── out/persistence/     # JPA Repository
└── common/                   # 공통 인프라
```

자세한 내용은 [아키텍처 문서](docs/architecture/ARCHITECTURE.md) 참조

## API 문서

### 가격 정책 관리 API

**Base URL**: `http://localhost:8080/api/pricing-policies`

주요 엔드포인트:
- `GET /{roomId}`: 가격 정책 조회
- `PUT /{roomId}/default-price`: 기본 가격 업데이트
- `PUT /{roomId}/time-range-prices`: 시간대별 가격 업데이트
- `POST /{targetRoomId}/copy`: 가격 정책 복사

자세한 API 문서는 [API 문서](../docs/features/pricing-policy/API.md) 참조

## 테스트

### 전체 테스트 실행
```bash
./gradlew test
```

### 특정 테스트 실행
```bash
./gradlew test --tests "PricingPolicyControllerTest"
```

### 테스트 커버리지 확인
```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### 테스트 구조

- **단위 테스트**: 도메인 로직 및 서비스 계층
  - `domain/` 테스트: 순수 도메인 로직
  - `application/service/` 테스트: Mock을 사용한 Use Case 테스트

- **통합 테스트**: Controller 및 Repository
  - `@WebMvcTest`: REST API 테스트
  - `@DataJpaTest`: Repository 테스트

## 빌드 및 배포

### 빌드
```bash
./gradlew clean build
```

### JAR 생성
```bash
./gradlew bootJar
java -jar build/libs/springProject-0.0.1-SNAPSHOT.jar
```

### Docker 이미지 생성 (예정)
```bash
./gradlew bootBuildImage
```

## 코드 품질

### 정적 분석 도구

- **Checkstyle**: 코드 스타일 검사
- **PMD**: 코드 품질 분석
- **SpotBugs**: 버그 패턴 검출

```bash
# 정적 분석 실행
./gradlew check

# 리포트 확인
open build/reports/checkstyle/main.html
open build/reports/pmd/main.html
open build/reports/spotbugs/main.html
```

## 이벤트 기반 아키텍처

### 구독 이벤트

**RoomCreatedEvent**
- **Topic**: `room-events`
- **처리**: 룸 생성 시 자동으로 기본 가격 정책 생성

```json
{
  "eventType": "RoomCreated",
  "roomId": 1,
  "placeId": 100,
  "timeSlot": "HOUR"
}
```

### 발행 이벤트 (예정)

- PricingPolicyUpdatedEvent
- ProductStockChangedEvent

## 주요 문서

- [아키텍처 개요](docs/architecture/ARCHITECTURE.md)
- [도메인 모델](docs/architecture/DOMAIN_MODEL.md)
- [API 문서](../docs/features/pricing-policy/API.md)

## 개발 가이드

### 브랜치 전략

- `main`: 프로덕션 브랜치
- `feature/*`: 새 기능 개발
- `fix/*`: 버그 수정
- `refactor/*`: 리팩터링
- `docs/*`: 문서 작업

### 커밋 메시지 규칙

```
<type>: <subject>

<body>

Related to #<issue-number>
```

**Type**:
- `FEAT`: 새 기능
- `FIX`: 버그 수정
- `REFACTOR`: 리팩터링
- `TEST`: 테스트 추가/수정
- `DOCS`: 문서 작업
- `CHORE`: 빌드/설정 변경

### 코딩 컨벤션

- Java Code Conventions 준수
- Checkstyle 규칙 준수
- 모든 public 메서드에 Javadoc 작성
- 단위 테스트 작성 필수

## 트러블슈팅

### PostgreSQL 연결 실패
```bash
# PostgreSQL 컨테이너 재시작
docker-compose restart postgres

# 로그 확인
docker-compose logs postgres
```

### Kafka 연결 실패
```bash
# Kafka 컨테이너 재시작
docker-compose restart kafka

# Topic 확인
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 빌드 실패
```bash
# 캐시 정리 후 재빌드
./gradlew clean build --refresh-dependencies
```

## 기여하기

1. Issue 생성 또는 선택
2. Feature 브랜치 생성
3. 변경사항 커밋
4. PR 생성
5. 코드 리뷰 후 머지

## 라이선스

TBD

## 연락처

- Project Lead: @DDINGJOO
- Repository: https://github.com/DDINGJOO/YE_YAK_HAE_YO_SERVICE_SERVER
