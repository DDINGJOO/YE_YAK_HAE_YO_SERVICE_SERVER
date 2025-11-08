# 프로젝트 자동화 시스템

이 프로젝트는 GitHub Issues, Pull Requests, 그리고 라벨링을 자동화하는 시스템입니다.

## 📋 목차

- [주요 기능](#주요-기능)
- [자동 라벨링 시스템](#자동-라벨링-시스템)
- [이슈 관리](#이슈-관리)
- [PR 자동화](#pr-자동화)
- [사용 가이드](#사용-가이드)

---

## 🎯 주요 기능

1. **자동 라벨링**: PR 생성 시 변경된 파일 경로에 따라 자동으로 라벨 부여
2. **이슈 템플릿**: Epic, Story, Task, Spike, Change Request 템플릿 제공
3. **이슈 자동 닫기**: PR 머지 시 연결된 이슈 자동으로 닫힘
4. **AI 어시스턴트 지원**: Claude Code가 커밋 분석 후 자동으로 관련 이슈 찾아서 PR 생성

---

## 🏷️ 자동 라벨링 시스템

PR이 생성되거나 업데이트되면 변경된 파일 경로를 분석하여 자동으로 라벨을 부여합니다.

### 전체 영역 라벨

| 라벨 | 색상 | 매칭 규칙 | 설명 |
|------|------|-----------|------|
| `backend` | - | `src/main/java/**` | 백엔드 Java 코드 |
| `frontend` | #06B6D4 | `src/main/resources/static/**`<br>`src/main/resources/templates/**`<br>`frontend/**`, `client/**` | 프론트엔드 코드 |
| `database` | #DC2626 | `src/main/resources/db/**`<br>`src/main/resources/migration/**`<br>`**/*migration*.sql`<br>`**/*schema*.sql` | DB 마이그레이션/스키마 |
| `docs` | - | `docs/**`<br>`*.md`<br>`README*` | 문서 파일 |
| `infra` | - | `.github/**`<br>`Dockerfile`<br>`docker-compose*.yml`<br>`k8s/**`, `kubernetes/**` | 인프라/CI/CD |

### 백엔드 레이어 라벨

백엔드 개발자를 위한 세분화된 레이어별 라벨입니다.

| 라벨 | 색상 | 매칭 규칙 | 설명 |
|------|------|-----------|------|
| `layer:entity` | #E11D48 | `**/entity/**`<br>`**/domain/**`<br>`**/model/**` | 엔티티/도메인 모델 |
| `layer:controller` | #3B82F6 | `**/controller/**`<br>`**/api/**` | 컨트롤러/API 엔드포인트 |
| `layer:dto` | #8B5CF6 | `**/dto/**`<br>`**/request/**`<br>`**/response/**` | DTO/요청-응답 모델 |
| `layer:repository` | #F59E0B | `**/repository/**`<br>`**/dao/**` | 리포지토리/데이터 접근 |
| `layer:service` | #10B981 | `**/service/**` | 서비스/비즈니스 로직 |
| `layer:util` | #6B7280 | `**/util/**`<br>`**/helper/**`<br>`**/common/**` | 유틸리티/헬퍼 함수 |
| `layer:config` | #EC4899 | `**/config/**`<br>`**/configuration/**` | 설정/Configuration |
| `layer:test` | #14B8A6 | `src/test/**`<br>`**/*Test.java`<br>`**/*Tests.java` | 테스트 코드 |
| `layer:resource` | #A855F7 | `src/main/resources/**/*.yml`<br>`src/main/resources/**/*.properties`<br>`src/main/resources/**/*.xml`<br>`src/main/resources/**/*.json` | 리소스 파일 |

### 라벨링 예시

```
변경된 파일:
- src/main/java/com/example/user/entity/User.java
- src/main/java/com/example/user/repository/UserRepository.java
- src/main/java/com/example/user/service/UserService.java
- src/main/java/com/example/user/controller/UserController.java
- src/test/java/com/example/user/service/UserServiceTest.java

자동으로 붙는 라벨:
✅ backend
✅ layer:entity
✅ layer:repository
✅ layer:service
✅ layer:controller
✅ layer:test
```

---

## 📝 이슈 관리

### 이슈 타입

| 타입 | 라벨 | 용도 | 소요 시간 |
|------|------|------|----------|
| **Epic** | `epic` | 큰 기능 (여러 Story로 구성) | 1~2주 이상 |
| **Story** | `story` | 사용자 관점의 완결된 기능 | 2~5일 |
| **Task** | `task` | 실제 개발 작업 단위 | 반나절~1일 |
| **Spike** | `spike` | 조사/실험 (시간 제한) | 설정한 타임박스 |
| **Change Request** | `change-request` | 설계/AC 변경 제안 | - |

### 이슈 계층 구조

```
Epic #1: 사용자 관리 기능
  ↓
Story #2: 사용자 로그인
  ↓
Task #3: 로그인 API 개발
Task #4: 로그인 화면 개발
```

자세한 사용법은 [ISSUE_GUIDE.md](ISSUE_GUIDE.md)를 참고하세요.

---

## 🔄 PR 자동화

### 1. 이슈 자동 닫기

PR 본문에 다음 키워드를 포함하면 PR 머지 시 해당 이슈가 자동으로 닫힙니다:

```markdown
Closes #23
Fixes #45
Resolves #67
```

**작동 방식:**
- PR이 `develop` 또는 `main` 브랜치에 머지되면
- `auto-close-issues.yml` 워크플로우가 실행되어
- 본문에서 `Closes #N` 패턴을 찾아서
- 해당 이슈들을 자동으로 닫고
- "Closed by PR #N" 코멘트를 추가합니다

### 2. AI 어시스턴트를 통한 PR 생성

Claude Code를 사용하면 자동으로 관련 이슈를 찾아서 PR을 생성합니다:

```bash
# 1. 작업 브랜치에서 개발
git checkout -b feature/user-login
# 코딩...
git commit -m "로그인 API 구현"
git push

# 2. Claude Code에게 요청
"PR 만들어줘"

# 3. AI가 자동으로:
# - 커밋 메시지 분석: "로그인 API 구현"
# - 열린 이슈 검색
# - 관련 이슈 매칭: #45 [TASK] 로그인 API 개발
# - PR 생성 with "Closes #45"
```

---

## 📖 사용 가이드

### 처음 시작하기

1. **이슈 생성**
   - GitHub Issues 탭 → New Issue
   - 템플릿 선택 (Epic/Story/Task/Spike/Change Request)
   - 필수 항목 작성 후 제출

2. **작업 시작**
   ```bash
   git checkout -b feature/작업명
   # 개발 작업...
   git add .
   git commit -m "작업 내용"
   git push -u origin feature/작업명
   ```

3. **PR 생성**
   - Claude Code 사용: "PR 만들어줘"
   - 또는 수동: GitHub에서 New Pull Request
   - PR 본문에 `Closes #이슈번호` 포함

4. **코드 리뷰 & 머지**
   - 리뷰어가 승인
   - PR 머지
   - 연결된 이슈 자동으로 닫힘 ✅

### 일반적인 워크플로우

```
1. Epic 이슈 생성 (#1)
   ↓
2. Story 이슈 생성 (#2) - Epic과 연결
   ↓
3. Task 이슈 생성 (#3, #4) - Story와 연결
   ↓
4. feature 브랜치에서 작업
   ↓
5. PR 생성 with "Closes #3"
   ↓
6. 코드 리뷰
   ↓
7. PR 머지 → 이슈 #3 자동 닫힘
```

---

## 🔧 설정 파일

### 워크플로우
- `.github/workflows/auto-label.yml` - 자동 라벨링
- `.github/workflows/auto-close-issues.yml` - 이슈 자동 닫기

### 설정 파일
- `.github/labeler.yml` - 라벨 매칭 규칙
- `.github/ISSUE_TEMPLATE/` - 이슈 템플릿들
  - `epic.yml`
  - `story.yml`
  - `task.yml`
  - `spike.yml`
  - `change_request.yml`

### 문서
- `PROJECT_SETUP.md` - AI 어시스턴트를 위한 프로젝트 설정 가이드
- `ISSUE_GUIDE.md` - 이슈 작성 상세 가이드

---

## 💡 팁

1. **라벨 커스터마이징**
   - `.github/labeler.yml` 수정하여 라벨 규칙 추가/변경 가능

2. **이슈 연결**
   - Task는 항상 Story나 Epic과 연결
   - 추적성을 위해 `#이슈번호` 형식으로 참조

3. **커밋 메시지**
   - 명확하게 작성하면 AI가 관련 이슈를 더 잘 찾음
   - 예: "로그인 API 구현" > "코드 수정"

4. **브랜치 네이밍**
   - `feature/기능명` 형식 권장
   - 예: `feature/user-login`, `feature/board-api`

---

## 📚 추가 문서

- [프로젝트 설정 가이드](PROJECT_SETUP.md) - AI 어시스턴트용 상세 가이드
- [이슈 작성 가이드](ISSUE_GUIDE.md) - 이슈 타입별 작성 예시

---

## 🤝 기여

이슈나 개선 사항이 있으시면 GitHub Issues로 등록해주세요!
