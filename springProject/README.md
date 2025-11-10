# Spring Project - 예약 가격 관리 서비스

이 디렉토리는 예약 가격 관리 서비스의 Spring Boot 애플리케이션입니다.

전체 프로젝트 문서는 [루트 README.md](../README.md)를 참조하세요.

---

## 빠른 시작

### 1. 인프라 실행

```bash
docker-compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 동작 확인

```bash
curl http://localhost:8080/actuator/health
```

---

## 주요 디렉토리

```
springProject/
├── src/main/java/com/teambind/springproject/
│   ├── domain/              # 도메인 계층 (순수 Java)
│   ├── application/         # 애플리케이션 계층 (Use Cases)
│   ├── adapter/             # 어댑터 계층 (REST, JPA, Kafka)
│   └── common/              # 공통 유틸리티
├── src/main/resources/
│   ├── db/migration/        # Flyway 마이그레이션 스크립트
│   └── application-dev.yaml # 설정 파일
├── src/test/                # 테스트
├── config/                  # 코드 품질 설정
│   ├── checkstyle/
│   ├── pmd/
│   └── spotbugs/
└── docker-compose.yml       # 로컬 개발 인프라
```

---

## 개발 가이드

### 코드 품질 검사

```bash
./gradlew codeQuality
```

### 테스트 실행

```bash
./gradlew test
```

### 빌드

```bash
./gradlew build
```

---

## 추가 문서

- [PACKAGE_STRUCTURE.md](PACKAGE_STRUCTURE.md) - Hexagonal Architecture 패키지 구조
- [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) - 데이터베이스 스키마 및 ERD
- [DOCKER_SETUP.md](DOCKER_SETUP.md) - Docker Compose 설정

---

**자세한 내용은 [루트 README.md](../README.md)를 참조하세요.**