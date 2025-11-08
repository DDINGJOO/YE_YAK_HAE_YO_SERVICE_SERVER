# 예약 가격 관리 서비스 (Reservation Pricing Service)

MSA 환경에서 시간대별 가격 정책 관리 및 예약 총 가격 계산을 담당하는 마이크로서비스

## 프로젝트 개요

이 서비스는 숙박 예약 시스템의 일부로, 다음 기능을 제공합니다:
- 시간대별(요일/시간) 가격 정책 관리
- 추가상품 재고 및 가격 관리
- 예약 시점의 가격 계산 및 스냅샷 저장
- 외부 서비스와의 이벤트 기반 통신 (Kafka)

## 기술 스택

### 백엔드
- **Java 21** - LTS 버전, Virtual Threads 지원
- **Spring Boot 3.2.5** - 안정성과 최신 기능 균형
- **Spring Data JPA** - 영속성 관리
- **Spring Kafka** - 이벤트 기반 통신

### 데이터베이스
- **PostgreSQL 16** - 메인 데이터베이스
- **Flyway** - 데이터베이스 마이그레이션

### 메시징
- **Apache Kafka 3.6** - 이벤트 스트리밍 플랫폼

### 빌드/테스트
- **Gradle 8.14** - 빌드 도구
- **JUnit 5** - 단위 테스트
- **H2 Database** - 테스트용 인메모리 DB

### 코드 품질
- **CheckStyle** - Google Java Style Guide 기반
- **PMD** - 코드 품질 검사
- **SpotBugs** - 버그 패턴 탐지

### CI/CD
- **GitHub Actions** - 자동화된 빌드 및 테스트
- **Docker Compose** - 로컬 개발 환경

## 아키텍처

### Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────┐
│                    Adapter Layer                        │
│  ┌──────────────┐              ┌──────────────┐        │
│  │   REST API   │              │  PostgreSQL  │        │
│  │  (Inbound)   │              │  (Outbound)  │        │
│  └──────┬───────┘              └──────▲───────┘        │
│         │                             │                 │
├─────────┼─────────────────────────────┼─────────────────┤
│         │    Application Layer        │                 │
│         │  ┌────────────────────┐     │                 │
│         ├─▶│  Use Case Service  │─────┤                 │
│         │  └────────────────────┘     │                 │
├─────────┼─────────────────────────────┼─────────────────┤
│         │      Domain Layer           │                 │
│         │  ┌────────────────────┐     │                 │
│         └─▶│    Aggregates      │◀────┘                 │
│            │  - PricingPolicy   │                       │
│            │  - Product         │                       │
│            │  - ReservationPrc  │                       │
│            └────────────────────┘                       │
└─────────────────────────────────────────────────────────┘
```

### Domain-Driven Design (DDD)

**핵심 Aggregate**:
- **PricingPolicy**: 시간대별 가격 정책
- **Product**: 추가상품 관리
- **ReservationPricing**: 예약 가격 스냅샷

자세한 아키텍처 설계는 [docs/architecture/](docs/architecture/) 참조

## 시작하기

### 사전 요구사항

- **Java 21** 설치 (JDK)
- **Docker Desktop** 설치
- **Git** 설치

### 1. 프로젝트 클론

```bash
git clone https://github.com/DDINGJOO/YE_YAK_HAE_YO_SERVICE_SERVER.git
cd YE_YAK_HAE_YO_SERVICE_SERVER
```

### 2. 인프라 실행 (Docker Compose)

```bash
cd springProject
docker-compose up -d
```

**실행되는 서비스**:
- PostgreSQL 16 (포트: 5432)
- Kafka 3.6 (포트: 9092)
- Zookeeper (포트: 2181)
- Kafka UI (포트: 8090)

**상태 확인**:
```bash
docker-compose ps
```

### 3. 환경 변수 설정

`.env` 파일이 이미 생성되어 있습니다:
```bash
cat .env
```

필요시 값 수정:
```env
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=reservation_pricing_db
DATABASE_USER_NAME=postgres
DATABASE_PASSWORD=postgres

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=reservation-pricing-service

SPRING_PROFILES_ACTIVE=dev
```

### 4. 애플리케이션 실행

#### Gradle 사용
```bash
./gradlew bootRun
```

#### IDE 사용 (IntelliJ IDEA)
1. `SpringProjectApplication.java` 실행
2. Active profiles: `dev`

### 5. 동작 확인

**애플리케이션 상태**:
```bash
curl http://localhost:8080/actuator/health
```

**API 문서 (Swagger UI)**: (작업 예정)
```
http://localhost:8080/swagger-ui.html
```

## 개발 가이드

### 프로젝트 구조

```
springProject/
├── src/
│   ├── main/
│   │   ├── java/com/teambind/springproject/
│   │   │   ├── domain/              # 도메인 계층
│   │   │   │   ├── pricingpolicy/
│   │   │   │   ├── product/
│   │   │   │   ├── reservationpricing/
│   │   │   │   └── shared/
│   │   │   ├── application/         # 애플리케이션 계층
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/          # Inbound Ports (Use Cases)
│   │   │   │   │   └── out/         # Outbound Ports
│   │   │   │   ├── service/
│   │   │   │   └── dto/
│   │   │   ├── adapter/             # 어댑터 계층
│   │   │   │   ├── in/
│   │   │   │   │   └── web/         # REST Controllers
│   │   │   │   └── out/
│   │   │   │       ├── persistence/ # JPA Repositories
│   │   │   │       └── messaging/   # Kafka Producers/Consumers
│   │   │   └── common/              # 공통 유틸리티
│   │   └── resources/
│   │       ├── application-dev.yaml
│   │       └── db/migration/        # Flyway scripts
│   └── test/
├── config/
│   ├── checkstyle/
│   ├── pmd/
│   └── spotbugs/
├── docker-compose.yml
└── build.gradle
```

### 코드 품질 검사

```bash
# 모든 품질 검사 실행
./gradlew codeQuality

# 개별 실행
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew spotbugsMain
```

**리포트 위치**:
- CheckStyle: `build/reports/checkstyle/main.html`
- PMD: `build/reports/pmd/main.html`
- SpotBugs: `build/reports/spotbugs/main/spotbugs.html`

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests "ClassName"

# 테스트 리포트
open build/reports/tests/test/index.html
```

### 빌드

```bash
# 전체 빌드 (테스트 + 코드 품질 검사 포함)
./gradlew build

# 빌드만 (테스트 제외)
./gradlew build -x test

# JAR 파일 위치
ls -la build/libs/
```

## 데이터베이스

### 마이그레이션 (Flyway)

마이그레이션 파일 위치: `src/main/resources/db/migration/`

```bash
# 마이그레이션 상태 확인
./gradlew flywayInfo

# 마이그레이션 실행 (애플리케이션 시작 시 자동 실행)
./gradlew bootRun
```

### PostgreSQL 접속

```bash
# psql 사용
docker exec -it reservation-pricing-postgres psql -U postgres -d reservation_pricing_db

# 마이그레이션 이력 확인
SELECT * FROM flyway_schema_history;

# 테이블 목록
\dt
```

## Kafka

### Kafka UI 접속

```
http://localhost:8090
```

### Kafka CLI 사용

```bash
# 토픽 생성
docker exec -it reservation-pricing-kafka kafka-topics \
  --create \
  --bootstrap-server localhost:9092 \
  --topic room.created \
  --partitions 3 \
  --replication-factor 1

# 토픽 목록
docker exec -it reservation-pricing-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092

# 메시지 수신
docker exec -it reservation-pricing-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic room.created \
  --from-beginning
```

## CI/CD

### GitHub Actions

**워크플로우**:
- PR 생성/업데이트 시 자동 실행
- main 브랜치 푸시 시 자동 실행

**실행 내용**:
1. Build and Test
   - Java 21 설치
   - PostgreSQL 서비스 시작
   - Gradle 빌드 및 테스트
2. Code Quality
   - CheckStyle, PMD, SpotBugs 실행

**실행 결과**: GitHub Actions 탭에서 확인

## 문서

### 전체 문서 목록
- [docs/INDEX.md](docs/INDEX.md) - 전체 문서 인덱스
- [docs/INFO.md](docs/INFO.md) - 프로젝트 상세 정보
- [docs/ISSUE_GUIDE.md](docs/ISSUE_GUIDE.md) - 이슈 작성 가이드
- [docs/PROJECT_SETUP.md](docs/PROJECT_SETUP.md) - 프로젝트 설정 가이드

### 요구사항
- [docs/requirements/PROJECT_REQUIREMENTS.md](docs/requirements/PROJECT_REQUIREMENTS.md)

### 아키텍처
- [docs/architecture/ARCHITECTURE_ANALYSIS.md](docs/architecture/ARCHITECTURE_ANALYSIS.md)
- [docs/architecture/DOMAIN_MODEL_DESIGN.md](docs/architecture/DOMAIN_MODEL_DESIGN.md)
- [docs/architecture/TECH_STACK_ANALYSIS.md](docs/architecture/TECH_STACK_ANALYSIS.md)

### ADR (Architecture Decision Records)
- [docs/adr/ADR_001_ARCHITECTURE_DECISION.md](docs/adr/ADR_001_ARCHITECTURE_DECISION.md)

## 문제 해결

### Docker 컨테이너 재시작

```bash
# 전체 재시작
docker-compose restart

# 특정 서비스만
docker-compose restart postgres
docker-compose restart kafka
```

### 포트 충돌

`docker-compose.yml`에서 포트 변경:
```yaml
ports:
  - "15432:5432"  # PostgreSQL
  - "19092:9092"  # Kafka
```

### 데이터 완전 초기화

```bash
# 컨테이너 및 볼륨 삭제
docker-compose down -v

# 재시작
docker-compose up -d
```

### Gradle 캐시 문제

```bash
# Gradle 캐시 삭제
./gradlew clean --refresh-dependencies
```

## 기여 가이드

1. 이슈 생성 ([ISSUE_GUIDE.md](docs/ISSUE_GUIDE.md) 참조)
2. 브랜치 생성 (`feature/이슈번호-작업내용`)
3. 코드 작성
4. 코드 품질 검사 (`./gradlew codeQuality`)
5. 테스트 실행 (`./gradlew test`)
6. PR 생성
7. CI 통과 확인
8. 코드 리뷰 후 머지

## 라이선스

이 프로젝트는 비공개 프로젝트입니다.

## 연락처

- GitHub Issues: 버그 리포트 및 기능 요청
- Pull Requests: 코드 기여

---

**Last Updated**: 2025-11-08
