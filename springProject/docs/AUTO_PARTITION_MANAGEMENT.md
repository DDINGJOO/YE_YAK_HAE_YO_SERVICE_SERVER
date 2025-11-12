# 자동 파티션 관리 (pg_partman)

## 개요

`product_time_slot_inventory` 테이블은 시간대별 재고를 관리하기 위해 월별 파티셔닝을 사용합니다.
pg_partman 확장을 사용하여 파티션을 자동으로 생성하고 관리합니다.

## 설정

### 1. pg_partman 설치 (운영환경)

```bash
# PostgreSQL에 pg_partman 확장 설치
sudo apt-get install postgresql-16-partman

# 또는 Docker 사용 시
docker exec -it <postgres-container> psql -U postgres -c "CREATE EXTENSION pg_partman;"
```

### 2. 자동 파티션 설정 (V12 Migration)

마이그레이션 파일 `V12__setup_pg_partman_auto_partition.sql`이 자동으로 실행되어 다음을 설정합니다:

- **premake**: 3개월 미리 생성
- **retention**: 12개월 후 자동 삭제
- **interval**: 1개월 단위

### 3. Spring 스케줄러 (자동 실행)

`PartitionMaintenanceScheduler`가 다음 작업을 자동으로 수행합니다:

- **매일 오전 3시**: 파티션 유지보수 실행
- **애플리케이션 시작 시**: 파티션 상태 확인

## 동작 방식

### 파티션 자동 생성

```
현재 날짜: 2025-11-12
├── 존재하는 파티션: 2025-11, 2025-12, 2026-01
└── premake=3 설정으로 3개월 미리 생성
    ├── 2026-02 (생성됨)
    ├── 2026-03 (생성됨)
    └── 2026-04 (생성됨)
```

### 파티션 자동 삭제

```
retention = 12개월 설정
├── 2024-10 파티션 (13개월 경과) → 삭제
├── 2024-11 파티션 (12개월 경과) → 삭제
└── 2024-12 파티션 (11개월) → 유지
```

## 모니터링

### 파티션 상태 확인

```sql
-- 현재 존재하는 모든 파티션 조회
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE tablename LIKE 'product_time_slot_inventory_%'
ORDER BY tablename;
```

### pg_partman 설정 확인

```sql
-- 파티션 관리 설정 조회
SELECT
    parent_table,
    partition_interval,
    premake,
    retention,
    retention_keep_table
FROM partman.part_config
WHERE parent_table = 'public.product_time_slot_inventory';
```

### 다음 생성될 파티션 확인

```sql
-- pg_partman이 다음에 생성할 파티션 목록
SELECT partman.show_partitions('public.product_time_slot_inventory');
```

## 수동 유지보수 (필요 시)

```sql
-- 수동으로 파티션 생성/정리 실행
SELECT partman.run_maintenance('public.product_time_slot_inventory');
```

## 로그 확인

Spring 애플리케이션 로그에서 다음 메시지를 확인할 수 있습니다:

```
[PartitionMaintenanceScheduler] Starting pg_partman maintenance for product_time_slot_inventory
[PartitionMaintenanceScheduler] Successfully completed pg_partman maintenance
```

## 트러블슈팅

### 문제: pg_partman 확장이 설치되지 않음

**증상**:
```
ERROR: extension "pg_partman" does not exist
```

**해결**:
```bash
# PostgreSQL에 pg_partman 설치
sudo apt-get install postgresql-16-partman
psql -U postgres -d your_database -c "CREATE EXTENSION pg_partman;"
```

### 문제: 파티션이 자동 생성되지 않음

**확인 사항**:
1. 스케줄러가 실행되는지 확인 (로그 확인)
2. pg_partman 설정 확인
   ```sql
   SELECT * FROM partman.part_config WHERE parent_table = 'public.product_time_slot_inventory';
   ```
3. 수동 실행으로 테스트
   ```sql
   SELECT partman.run_maintenance('public.product_time_slot_inventory');
   ```

### 문제: 오래된 파티션이 삭제되지 않음

**확인**:
```sql
-- retention 설정 확인
SELECT retention, retention_keep_table
FROM partman.part_config
WHERE parent_table = 'public.product_time_slot_inventory';
```

**수동 정리**:
```sql
-- 특정 파티션 수동 삭제
DROP TABLE product_time_slot_inventory_2024_01;
```

## 참고 자료

- [pg_partman GitHub](https://github.com/pgpartman/pg_partman)
- [pg_partman Documentation](https://github.com/pgpartman/pg_partman/blob/master/doc/pg_partman.md)

---

**Last Updated**: 2025-11-12