# GitHub Actions Workflows

이 디렉토리는 GitHub Actions CI/CD 워크플로우 설정 파일들을 포함합니다.

## 워크플로우 목록

### 1. CI (ci.yml)
**트리거**: PR 생성/업데이트, main 브랜치 푸시

**작업**:
- **Build and Test**: 빌드 및 테스트 실행
  - Java 21 설치
  - PostgreSQL 16 서비스 실행
  - Gradle 빌드 (테스트 제외)
  - 단위 테스트 실행
  - 테스트 결과 업로드

- **Code Quality**: 코드 품질 검사
  - CheckStyle 실행
  - PMD 실행
  - SpotBugs 실행
  - 품질 검사 리포트 업로드

**비용 최적화**:
- `concurrency` 설정으로 동일 브랜치의 이전 실행 자동 취소
- Gradle 캐싱으로 빌드 시간 단축
- 병렬 실행으로 전체 실행 시간 최소화

### 2. Auto Label (auto-label.yml)
PR 자동 라벨링

### 3. Auto Close Issues (auto-close-issues.yml)
이슈 자동 종료

## 로컬에서 워크플로우 테스트

```bash
# act 설치 (macOS)
brew install act

# CI 워크플로우 테스트
act pull_request

# 특정 job만 실행
act -j build-and-test
```

## 아티팩트 다운로드

CI 실행 후 생성되는 리포트:
- **테스트 결과**: `test-results`
- **빌드 리포트**: `build-reports`
- **CheckStyle 리포트**: `checkstyle-report`
- **PMD 리포트**: `pmd-report`
- **SpotBugs 리포트**: `spotbugs-report`

GitHub Actions 탭에서 각 실행의 아티팩트를 다운로드할 수 있습니다.

## 문제 해결

### Gradle 캐시 무효화
```yaml
# ci.yml에서 캐시 키 변경
cache-key: gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}-v2
```

### PostgreSQL 연결 실패
서비스 컨테이너 health check 설정 확인:
```yaml
options: >-
  --health-cmd pg_isready
  --health-interval 10s
  --health-timeout 5s
  --health-retries 5
```
