# Docker Setup Guide

예약 가격 관리 서비스의 로컬 개발 환경 구성 가이드입니다.

---

## 사전 요구사항

- Docker Desktop 설치 (https://www.docker.com/products/docker-desktop/)
- Docker Compose 설치 (Docker Desktop에 포함)

**버전 확인:**
```bash
docker --version
docker-compose --version
```

---

## 구성 요소

### 1. PostgreSQL 16
- **이미지**: `postgres:16-alpine`
- **포트**: `5432`
- **데이터베이스**: `reservation_pricing_db`
- **사용자**: `postgres` / `postgres`
- **볼륨**: `postgres_data` (데이터 영속화)

### 2. Kafka 3.6
- **이미지**: `confluentinc/cp-kafka:7.5.0` (Kafka 3.6.x 호환)
- **포트**: `9092` (외부 접속), `29092` (내부 통신)
- **Zookeeper**: `confluentinc/cp-zookeeper:7.5.0`
- **자동 토픽 생성**: 활성화

### 3. Kafka UI (Optional)
- **이미지**: `provectuslabs/kafka-ui:latest`
- **포트**: `8090`
- **URL**: http://localhost:8090
- **용도**: Kafka 토픽/메시지 모니터링

---

## 빠른 시작

### 1. 컨테이너 시작
```bash
cd springProject
docker-compose up -d
```

### 2. 상태 확인
```bash
# 모든 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f postgres
docker-compose logs -f kafka
```

### 3. 헬스체크 확인
```bash
# PostgreSQL 연결 테스트
docker exec -it reservation-pricing-postgres psql -U postgres -d reservation_pricing_db -c "SELECT version();"

# Kafka 브로커 확인
docker exec -it reservation-pricing-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### 4. 컨테이너 중지
```bash
# 컨테이너 중지 (데이터 유지)
docker-compose stop

# 컨테이너 중지 및 삭제 (데이터 유지)
docker-compose down

# 컨테이너 + 볼륨 삭제 (데이터 삭제)
docker-compose down -v
```

---

## PostgreSQL 접속

### psql CLI
```bash
docker exec -it reservation-pricing-postgres psql -U postgres -d reservation_pricing_db
```

### DBeaver / DataGrip
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `reservation_pricing_db`
- **Username**: `postgres`
- **Password**: `postgres`

### Flyway 마이그레이션 확인
```sql
-- 마이그레이션 이력 확인
SELECT * FROM flyway_schema_history;

-- 테이블 목록 확인
\dt

-- pricing_policies 테이블 확인
SELECT * FROM pricing_policies;
```

---

## Kafka 사용

### Kafka UI 접속
```
http://localhost:8090
```

**주요 기능**:
- 토픽 목록 및 생성
- 메시지 조회 및 발행
- 컨슈머 그룹 모니터링

### Kafka CLI (컨테이너 내부)

**토픽 생성**:
```bash
docker exec -it reservation-pricing-kafka kafka-topics \
  --create \
  --bootstrap-server localhost:9092 \
  --topic room.created \
  --partitions 3 \
  --replication-factor 1
```

**토픽 목록 조회**:
```bash
docker exec -it reservation-pricing-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

**메시지 발행 (Producer)**:
```bash
docker exec -it reservation-pricing-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic room.created
```

**메시지 수신 (Consumer)**:
```bash
docker exec -it reservation-pricing-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic room.created \
  --from-beginning
```

---

## 애플리케이션 연동

### application.yml 설정

프로젝트에 `src/main/resources/application.yml` 파일을 생성하고 다음 내용을 추가합니다:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reservation_pricing_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: reservation-pricing-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      acks: all
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

server:
  port: 8080

logging:
  level:
    com.teambind.springproject: INFO
    org.hibernate.SQL: DEBUG
```

### 애플리케이션 실행
```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IDE에서 SpringProjectApplication 실행
```

---

## 문제 해결

### PostgreSQL 연결 실패
```bash
# 컨테이너 재시작
docker-compose restart postgres

# 로그 확인
docker-compose logs postgres

# 헬스체크 확인
docker exec -it reservation-pricing-postgres pg_isready -U postgres
```

### Kafka 연결 실패
```bash
# Zookeeper 및 Kafka 재시작
docker-compose restart zookeeper kafka

# Kafka 브로커 확인
docker exec -it reservation-pricing-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### 포트 충돌
이미 사용 중인 포트가 있다면 `docker-compose.yml`에서 포트 변경:
```yaml
ports:
  - "15432:5432"  # PostgreSQL
  - "19092:9092"  # Kafka
  - "18090:8080"  # Kafka UI
```

### 볼륨 초기화 (데이터 완전 삭제)
```bash
# 모든 컨테이너 및 볼륨 삭제
docker-compose down -v

# 재시작
docker-compose up -d
```

---

## 개발 워크플로우

### 1. 초기 설정
```bash
# Docker 컨테이너 시작
docker-compose up -d

# PostgreSQL 헬스체크 대기
sleep 10

# 애플리케이션 실행 (Flyway 자동 마이그레이션)
./gradlew bootRun
```

### 2. 데이터베이스 스키마 변경
```bash
# 1. 새 마이그레이션 파일 생성
# src/main/resources/db/migration/V2__add_new_feature.sql

# 2. 애플리케이션 재시작 (Flyway 자동 실행)
./gradlew bootRun

# 3. 마이그레이션 확인
docker exec -it reservation-pricing-postgres psql -U postgres -d reservation_pricing_db -c "SELECT * FROM flyway_schema_history;"
```

### 3. Kafka 이벤트 테스트
```bash
# 1. Kafka UI 접속
open http://localhost:8090

# 2. 토픽 생성 (room.created)
# 3. 메시지 발행 (Test Data)
# 4. 애플리케이션 로그 확인
```

---

## 운영 환경 고려사항

### PostgreSQL
- **백업**: 정기적인 pg_dump 백업
- **복제**: Primary-Replica 구성 검토
- **연결 풀**: HikariCP 설정 최적화

### Kafka
- **파티션**: 처리량에 따라 파티션 수 조정
- **복제**: 운영 환경에서는 replication-factor >= 3
- **보안**: SASL/SSL 인증 설정

---

## 참고 자료

- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Platform Documentation](https://docs.confluent.io/)
- [Spring Boot with Docker Compose](https://spring.io/guides/gs/spring-boot-docker/)

---

### 보안 설정 (운영 환경)

**PostgreSQL**:
```yaml
# 운영 환경에서는 강력한 비밀번호 사용
POSTGRES_PASSWORD: ${DB_PASSWORD}  # 환경변수로 관리

# 외부 접속 제한
# docker-compose.yml에서 ports 제거하고 내부 네트워크만 사용
```

**Kafka**:
```yaml
# SASL/SSL 인증 설정
KAFKA_SECURITY_PROTOCOL: SASL_SSL
KAFKA_SASL_MECHANISM: PLAIN
KAFKA_SASL_JAAS_CONFIG: ${KAFKA_JAAS_CONFIG}
```

### 리소스 제한 (운영 환경)

```yaml
services:
  postgres:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G

  kafka:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
```

---

**Last Updated**: 2025-11-12
