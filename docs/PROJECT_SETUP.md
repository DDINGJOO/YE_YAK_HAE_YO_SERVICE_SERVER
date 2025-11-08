# 프로젝트 설정 가이드

이 문서는 프로젝트의 이슈/PR 관리 시스템에 대한 모든 정보를 담고 있습니다.
AI 어시스턴트(Claude Code)가 이 문서를 읽고 자동으로 이슈와 PR을 관리합니다.

---

## 📌 프로젝트 정보

### 기본 정보
- **저장소**: DDINGJOO/TEAMBIND_REPO_SETTUP
- **메인 브랜치**: main
- **개발 브랜치**: develop
- **기본 브랜치 전략**: Feature Branch Workflow

### 브랜치 네이밍 규칙
- `feature/기능명` - 새로운 기능 개발
- `bugfix/버그명` - 버그 수정
- `hotfix/긴급수정명` - 긴급 수정
- `refactor/리팩토링명` - 리팩토링

---

## 🏷️ 이슈 타입과 라벨

### 1. Epic (라벨: `epic`, 색상: #8B5CF6)
**정의**: 큰 기능 단위 (여러 Story로 구성)

**언제 사용?**
- 1~2주 이상 걸리는 큰 기능
- 여러 개의 하위 작업(Story)으로 쪼갤 수 있는 경우

**필수 항목**:
- 목표 및 성공 지표
- 범위 (포함/제외)
- 하위 스토리 체크리스트
- 마일스톤

**이슈 템플릿**: `epic.yml`

---

### 2. Story (라벨: `story`, 색상: #10B981)
**정의**: 사용자 관점의 완결된 기능 (2~5일 소요)

**언제 사용?**
- 사용자가 직접 사용하는 기능 하나
- Epic의 하위 작업으로 분해된 기능

**필수 항목**:
- 배경 (왜 필요한지)
- 수용 기준(AC) - 체크리스트 형식
- 연결된 Epic 번호

**이슈 템플릿**: `story.yml`

---

### 3. Task (라벨: `task`, 색상: #3B82F6)
**정의**: 실제 개발 작업 단위 (반나절~1일 소요)

**언제 사용?**
- 실제로 코드를 작성하는 작업
- Story의 구현을 위한 세부 작업
- 백엔드/프론트엔드 각각 별도 Task

**필수 항목**:
- 연결된 Story/Epic 번호
- 작업 범위 (구체적으로)
- Done 기준 체크리스트

**이슈 템플릿**: `task.yml`

---

### 4. Spike (라벨: `spike`, 색상: #F59E0B)
**정의**: 조사/실험 작업 (시간 제한 있음)

**언제 사용?**
- 기술 조사가 필요할 때
- 여러 방법 중 선택이 필요할 때
- POC(개념 증명)가 필요할 때

**필수 항목**:
- 타임박스 (예: 1일, 4시간)
- 핵심 질문
- 산출물 (문서, ADR 등)

**이슈 템플릿**: `spike.yml`

---

### 5. Change Request (라벨: `change-request`, 색상: #EF4444)
**정의**: 기존 계획/설계의 변경 제안

**언제 사용?**
- 디자인/기획이 변경됐을 때
- 더 나은 구현 방법을 발견했을 때
- AC(수용 기준) 수정이 필요할 때

**필수 항목**:
- 영향받는 Epic/Story/Task 번호
- 제안 변경 사항
- 영향도 분석
- 결정 근거

**이슈 템플릿**: `change_request.yml`

---

## 🔄 워크플로우

### 이슈 생성 플로우
```
Epic 생성
  ↓
Story 생성 (Epic과 연결)
  ↓
Task 생성 (Story와 연결)
  ↓
개발 진행
  ↓
PR 생성 (Task와 연결)
  ↓
PR 머지 → 이슈 자동 닫힘
```

### PR 자동 생성 플로우 (비활성화됨)
- **현재 상태**: `auto-pr.yml.disabled` (권한 문제로 비활성화)
- **대안**: AI 어시스턴트가 수동으로 PR 생성
- **사용법**: "PR 만들어줘" 요청 시 자동으로 관련 이슈 찾아서 생성

---

## 🤖 AI 어시스턴트 사용 가이드

### 이슈 자동 생성
**사용자 요청**:
```
"게시판 기능 이슈들을 발행해줘"
```

**AI가 하는 일**:
1. Epic, Story, Task 계층 구조로 이슈 생성
2. 각 이슈를 연결 (#번호로 참조)
3. 적절한 라벨 자동 할당
4. 이슈 번호 반환

---

### PR 자동 생성 with 이슈 연결

**사용자 요청**:
```
"PR 만들어줘"
```

**AI가 하는 일**:
1. 현재 브랜치의 커밋 메시지 분석
2. 변경된 파일 확인
3. 열린 이슈 목록 조회 (`gh issue list --state open`)
4. 커밋 내용과 이슈 매칭:
   - 커밋 메시지 키워드 매칭
   - 파일 경로 매칭 (backend/frontend)
   - 이슈의 작업 범위와 비교
5. 관련 이슈 찾으면 PR 본문에 `Closes #이슈번호` 자동 추가
6. PR 생성

**예시**:
```bash
# 현재 브랜치: feature/board-api
# 커밋: "게시글 목록 조회 API 구현"
# 변경 파일: BoardController.java, BoardService.java

→ AI가 자동으로 찾음:
   - #23 [TASK] 게시글 목록 조회 API 개발

→ PR 생성 with "Closes #23"
```

---

### 이슈 번호 찾기 가이드 (AI용)

**AI가 이슈를 찾는 방법**:

```bash
# 1. 모든 열린 Task 이슈 조회
gh issue list --state open --label task --json number,title,body

# 2. 커밋 메시지 확인
git log develop..HEAD --pretty=format:"%s"

# 3. 변경된 파일 확인
git diff --name-status develop..HEAD

# 4. 매칭 로직
# - 커밋 메시지에 "게시글 목록" → 이슈 제목에 "게시글 목록"
# - 파일에 "Controller" → 백엔드 Task
# - 파일에 "Component" → 프론트엔드 Task
```

**매칭 우선순위**:
1. 커밋 메시지와 이슈 제목의 키워드 일치 (최우선)
2. 변경 파일 경로와 이슈 작업 범위 일치
3. 브랜치 이름에 이슈 번호 포함 (예: `feature/23-board-api`)

---

## 📋 PR 템플릿 구조

```markdown
## Summary
작업 내용 요약 (1~2문장)

## Changes
- 변경 사항 1
- 변경 사항 2

## Related Issues
Closes #이슈번호

## Test Plan
- [ ] 테스트 항목 1
- [ ] 테스트 항목 2

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

**중요**: `Closes #이슈번호`, `Fixes #이슈번호`, `Resolves #이슈번호` 중 하나를 사용하면 PR 머지 시 해당 이슈가 자동으로 닫힙니다.

---

## 🔧 자동화 워크플로우

### 1. Auto Label (활성화됨)
- **파일**: `.github/workflows/auto-label.yml`
- **트리거**: PR 생성/업데이트
- **기능**: 변경된 파일 경로에 따라 자동으로 라벨 추가

**라벨 규칙** (`.github/labeler.yml`):
```yaml
backend:
  - "src/main/java/**"
docs:
  - "docs/**"
infra:
  - ".github/**"
```

---

### 2. Auto Close Issues (활성화됨)
- **파일**: `.github/workflows/auto-close-issues.yml`
- **트리거**: PR이 `develop` 또는 `main`에 머지될 때
- **기능**:
  - PR 본문에서 `Closes #N`, `Fixes #N`, `Resolves #N` 패턴 찾기
  - 해당 이슈들을 자동으로 닫기
  - 이슈에 "Closed by PR #N" 코멘트 추가

**중요**: 이 워크플로우 덕분에 `develop` 브랜치로의 PR 머지에서도 이슈가 자동으로 닫힙니다!

---

### 3. Auto PR (비활성화됨)
- **파일**: `.github/workflows/auto-pr.yml.disabled`
- **상태**: 권한 문제로 비활성화
- **대안**: AI 어시스턴트가 수동으로 PR 생성

---

## 📊 프로젝트 현황 파악 (AI용)

### 현재 열린 이슈 확인
```bash
# Epic 확인
gh issue list --state open --label epic --json number,title

# Story 확인
gh issue list --state open --label story --json number,title

# Task 확인 (가장 중요!)
gh issue list --state open --label task --json number,title,body
```

### 특정 이슈 상세 조회
```bash
gh issue view 23 --json title,body,labels,state
```

### PR 상태 확인
```bash
# 열린 PR 확인
gh pr list --state open

# 특정 브랜치의 PR 확인
gh pr list --head feature/board-api
```

---

## 🎯 실전 시나리오

### 시나리오 1: 새로운 기능 개발 시작

**사용자**:
```
"사용자 인증 기능 이슈들을 만들어줘"
```

**AI 처리 순서**:
1. Epic 생성: `[EPIC] 사용자 인증 기능 구현`
2. Story 생성:
   - `[STORY] 로그인 기능`
   - `[STORY] 회원가입 기능`
   - `[STORY] 비밀번호 재설정`
3. Task 생성 (각 Story마다):
   - `[TASK] 로그인 API 개발`
   - `[TASK] 로그인 화면 개발`
   - ...
4. 생성된 이슈 번호와 링크 반환

---

### 시나리오 2: 작업 완료 후 PR 생성

**사용자 작업**:
```bash
git checkout -b feature/login-api
# 코딩...
git add .
git commit -m "로그인 API 구현 완료"
git push -u origin feature/login-api
```

**사용자 요청**:
```
"PR 만들어줘"
```

**AI 처리 순서**:
1. 커밋 메시지 분석: "로그인 API 구현"
2. 열린 Task 이슈 조회
3. 매칭: `#45 [TASK] 로그인 API 개발` 발견
4. PR 생성:
   ```
   제목: [TASK] 로그인 API 개발
   본문:
   ## Summary
   로그인 API 구현 완료

   ## Related Issues
   Closes #45
   ```
5. PR 링크 반환

---

### 시나리오 3: 여러 이슈를 동시에 작업한 경우

**커밋 내역**:
```
- "로그인 API 구현"
- "회원가입 API 구현"
```

**AI 처리**:
1. 커밋 메시지 전체 분석
2. 관련 이슈 찾기:
   - #45 [TASK] 로그인 API 개발
   - #46 [TASK] 회원가입 API 개발
3. PR 본문에 여러 이슈 연결:
   ```
   Closes #45, #46
   ```

---

### 시나리오 4: 이슈 번호를 모를 때

**사용자**:
```
"게시글 목록 API 작업했는데 이슈 번호가 뭐였지?"
```

**AI 처리**:
```bash
gh issue list --state open --label task --json number,title | grep "게시글 목록"
```

**AI 응답**:
```
#23 [TASK] 게시글 목록 조회 API 개발 입니다.
PR 만들어드릴까요?
```

---

## 🚨 문제 해결

### Q1: PR을 머지했는데 이슈가 안 닫혀요
**원인**: `develop` 브랜치로 머지한 경우, 기본적으로 GitHub는 이슈를 자동으로 닫지 않습니다.

**해결**: `auto-close-issues.yml` 워크플로우가 활성화되어 있으므로 자동으로 닫힙니다.
- 워크플로우 실행 확인: Actions 탭
- 워크플로우 실패 시: 수동으로 이슈 닫기

---

### Q2: AI가 잘못된 이슈를 연결했어요
**해결**:
1. PR 본문 수정해서 올바른 이슈 번호로 변경
2. 또는 AI에게 명시적으로 알려주기:
   ```
   "이건 #50 이슈 작업이야. PR 다시 만들어줘"
   ```

---

### Q3: 라벨이 자동으로 안 붙어요
**확인**:
1. `.github/labeler.yml` 파일의 경로 패턴 확인
2. 변경된 파일이 패턴과 일치하는지 확인
3. `auto-label.yml` 워크플로우가 v4를 사용하는지 확인

---

## 📝 AI 어시스턴트를 위한 체크리스트

**PR 생성 시 AI가 확인해야 할 것**:
- [ ] 현재 브랜치 확인
- [ ] 커밋 메시지 분석
- [ ] 변경된 파일 확인
- [ ] 열린 Task 이슈 조회
- [ ] 키워드 매칭으로 관련 이슈 찾기
- [ ] PR 본문에 `Closes #N` 형식으로 추가
- [ ] 이슈와 관련된 정보를 PR 본문에 포함
- [ ] PR 생성 후 URL 반환

**이슈 생성 시 AI가 확인해야 할 것**:
- [ ] Epic → Story → Task 계층 구조 유지
- [ ] 각 이슈에 연결된 상위 이슈 번호 포함
- [ ] 적절한 라벨 할당
- [ ] 체크리스트 형식의 AC(수용 기준) 포함
- [ ] 생성된 이슈 번호 기록 및 반환

---

## 🔗 참고 링크

- [GitHub 이슈 작성 가이드](ISSUE_GUIDE.md)
- [GitHub CLI 문서](https://cli.github.com/manual/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)

---

## 📅 마지막 업데이트

- **날짜**: 2025-10-14
- **작성자**: AI Assistant (Claude Code)
- **버전**: 1.0
