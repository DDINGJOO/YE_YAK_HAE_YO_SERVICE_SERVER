# Spring Project - 예약 가격 관리 서비스

이 디렉토리는 예약 가격 관리 서비스의 Spring Boot 애플리케이션입니다.

**전체 프로젝트 문서는 [루트 README.md](../README.md)를 참조하세요.**

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

## 개발 명령어

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

## 상세 문서

모든 상세 문서는 `docs/` 디렉토리로 이동되었습니다:

- **[패키지 구조](../docs/implementation/PACKAGE_STRUCTURE.md)** - Hexagonal Architecture 패키지 구조
- **[데이터베이스 스키마](../docs/implementation/DATABASE_SCHEMA.md)** - 데이터베이스 스키마 및 ERD
- **[Docker 설정](../docs/implementation/DOCKER_SETUP.md)** - Docker Compose 설정

**전체 문서 인덱스는 [docs/INDEX.md](../docs/INDEX.md)를 참조하세요.**
