# 프로젝트 설정 가이드

이 문서는 프로젝트의 이슈/PR 관리 시스템과 워크플로우에 대한 가이드입니다.

---

## 프로젝트 정보

### 기본 정보
- **프로젝트명**: 예약 가격 관리 서비스 (Reservation Pricing Service)
- **아키텍처**: Hexagonal Architecture + DDD
- **기술 스택**: Java 21, Spring Boot 3.2, PostgreSQL 16, Kafka 3.6
- **메인 브랜치**: main
- **브랜치 전략**: Feature Branch Workflow

### 브랜치 네이밍 규칙
- `feature/기능명` - 새로운 기능 개발
- `bugfix/버그명` - 버그 수정
- `hotfix/긴급수정명` - 긴급 수정
- `refactor/리팩토링명` - 리팩토링

---

## 이슈 타입과 라벨

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

**이슈 템플릿**: `.github/ISSUE_TEMPLATE/epic.yml`

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

**이슈 템플릿**: `.github/ISSUE_TEMPLATE/story.yml`

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

**이슈 템플릿**: `.github/ISSUE_TEMPLATE/task.yml`

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

**이슈 템플릿**: `.github/ISSUE_TEMPLATE/spike.yml`

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

**이슈 템플릿**: `.github/ISSUE_TEMPLATE/change_request.yml`

---

## 워크플로우

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

### 일반적인 개발 워크플로우

1. **이슈 선택**
   - GitHub Issues에서 작업할 Task 선택
   - 자신에게 Assign

2. **브랜치 생성**
   ```bash
   git checkout -b feature/작업명
   ```

3. **개발 진행**
   - 코드 작성
   - 로컬 테스트

4. **커밋**
   ```bash
   git add .
   git commit -m "[TYPE] 작업 내용"
   git push -u origin feature/작업명
   ```

5. **PR 생성**
   - GitHub에서 New Pull Request
   - PR 본문에 `Closes #이슈번호` 포함
   - 리뷰어 지정

6. **코드 리뷰**
   - 팀원 리뷰
   - 피드백 반영

7. **머지**
   - PR 승인 후 머지
   - 연결된 이슈 자동 닫힘

---

## PR 템플릿 구조

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
```

**중요**: `Closes #이슈번호`, `Fixes #이슈번호`, `Resolves #이슈번호` 중 하나를 사용하면 PR 머지 시 해당 이슈가 자동으로 닫힙니다.

---

## 자동화 워크플로우

### 1. Auto Label
- **파일**: `.github/workflows/auto-label.yml`
- **트리거**: PR 생성/업데이트
- **기능**: 변경된 파일 경로에 따라 자동으로 라벨 추가

**라벨 규칙** (`.github/labeler.yml` 참고):
- `backend`: Java 코드 변경 시
- `database`: DB 마이그레이션 파일 변경 시
- `docs`: 문서 변경 시
- `infra`: CI/CD, Docker 파일 변경 시
- `layer:*`: 백엔드 레이어별 세분화 라벨

자세한 라벨 규칙은 [INFO.md](INFO.md#자동-라벨링-시스템)를 참고하세요.

---

### 2. Auto Close Issues
- **파일**: `.github/workflows/auto-close-issues.yml`
- **트리거**: PR이 `develop` 또는 `main`에 머지될 때
- **기능**:
  - PR 본문에서 `Closes #N`, `Fixes #N`, `Resolves #N` 패턴 찾기
  - 해당 이슈들을 자동으로 닫기
  - 이슈에 "Closed by PR #N" 코멘트 추가

**중요**: 이 워크플로우 덕분에 `develop` 브랜치로의 PR 머지에서도 이슈가 자동으로 닫힙니다.

---

### 3. CI (Continuous Integration)
- **파일**: `.github/workflows/ci.yml`
- **트리거**: PR 생성/업데이트, main 브랜치 푸시
- **기능**:
  - 코드 빌드
  - 테스트 실행
  - 코드 품질 검증

---

## GitHub CLI 활용

### 이슈 관리

```bash
# 열린 이슈 목록 조회
gh issue list --state open

# Task 이슈만 조회
gh issue list --state open --label task

# 특정 이슈 상세 조회
gh issue view 23

# 이슈 생성
gh issue create --title "[TASK] 작업명" --body "작업 내용" --label task

# 이슈 닫기
gh issue close 23
```

### PR 관리

```bash
# 열린 PR 목록 조회
gh pr list --state open

# PR 생성
gh pr create --title "PR 제목" --body "PR 내용"

# PR 상태 확인
gh pr status

# PR 머지
gh pr merge 45
```

---

## 실전 시나리오

### 시나리오 1: 새로운 Task 작업 시작

1. GitHub Issues에서 Task 선택 (예: #23 "게시글 목록 조회 API 개발")
2. 브랜치 생성
   ```bash
   git checkout -b feature/board-list-api
   ```
3. 개발 진행
4. 커밋 및 푸시
   ```bash
   git add .
   git commit -m "FEAT 게시글 목록 조회 API 구현"
   git push -u origin feature/board-list-api
   ```
5. PR 생성
   - GitHub에서 New Pull Request
   - PR 본문에 `Closes #23` 추가
6. 리뷰 및 머지

---

### 시나리오 2: 여러 이슈를 동시에 작업한 경우

**상황**: 로그인 API(#45)와 회원가입 API(#46)를 함께 작업

**PR 본문**:
```markdown
## Summary
사용자 인증 API 구현

## Changes
- 로그인 API 구현
- 회원가입 API 구현

## Related Issues
Closes #45, #46
```

**결과**: PR 머지 시 #45, #46 모두 자동으로 닫힘

---

### 시나리오 3: 작업 중 설계 변경 필요

1. Change Request 이슈 생성
   ```bash
   gh issue create --title "[CR] 게시글 작성 시 카테고리 추가" \
     --body "카테고리 선택 기능 추가 제안" \
     --label change-request
   ```
2. 팀 논의 및 승인
3. 관련 Task 이슈 업데이트
4. 개발 진행

---

## 문제 해결

### Q1: PR을 머지했는데 이슈가 안 닫혀요
**원인**: PR 본문에 `Closes #N` 형식이 없거나, 워크플로우 실패

**해결**:
1. PR 본문 확인 (`Closes #N` 형식 확인)
2. GitHub Actions 탭에서 워크플로우 실행 확인
3. 워크플로우 실패 시: 수동으로 이슈 닫기

---

### Q2: 라벨이 자동으로 안 붙어요
**확인**:
1. `.github/labeler.yml` 파일의 경로 패턴 확인
2. 변경된 파일이 패턴과 일치하는지 확인
3. `auto-label.yml` 워크플로우 실행 상태 확인 (Actions 탭)

---

### Q3: 브랜치를 잘못 만들었어요
**해결**:
```bash
# 브랜치 이름 변경
git branch -m 기존이름 새이름

# 원격에 새 이름으로 푸시
git push origin -u 새이름

# 기존 원격 브랜치 삭제
git push origin --delete 기존이름
```

---

## 커밋 메시지 컨벤션

### 형식
```
[TYPE] 간단한 설명

상세 설명 (선택사항)
```

### TYPE 종류
- `FEAT`: 새로운 기능 추가
- `FIX`: 버그 수정
- `REFACTOR`: 코드 리팩토링
- `TEST`: 테스트 코드 추가/수정
- `DOCS`: 문서 수정
- `CHORE`: 빌드, 설정 파일 수정
- `PERF`: 성능 개선

### 예시
```bash
git commit -m "FEAT PricingPolicy Aggregate 도메인 모델 구현"
git commit -m "FIX 시간대 중복 검증 로직 수정"
git commit -m "REFACTOR PricingPolicyService 메서드 분리"
git commit -m "TEST PricingPolicy 단위 테스트 추가"
git commit -m "DOCS ADR-001 아키텍처 결정 문서 작성"
```

---

## 코드 리뷰 가이드

### 리뷰어 체크리스트
- [ ] 코드가 SOLID 원칙을 따르는가?
- [ ] Google Java Style Guide를 준수하는가?
- [ ] 테스트 코드가 충분한가?
- [ ] 도메인 규칙(Invariants)을 위반하지 않는가?
- [ ] 성능 이슈가 없는가?
- [ ] 보안 취약점이 없는가? (SQL Injection, XSS 등)

### 리뷰 코멘트 예시

**Good**:
```
이 부분에서 Value Object를 사용하는 게 어떨까요?
불변성을 보장하고 도메인 개념을 명확히 표현할 수 있을 것 같습니다.
```

**Better**:
```
TimeSlot을 Value Object로 추출하면:
1. 시간대 유효성 검증을 캡슐화 가능
2. 불변성 보장
3. 재사용성 향상

예시 코드:
public record TimeSlot(LocalTime startTime, LocalTime endTime) {
    public TimeSlot {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
    }
}
```

---

## 문서 참조

### 프로젝트 이해
- [INDEX.md](INDEX.md) - 전체 문서 인덱스
- [INFO.md](INFO.md) - 프로젝트 개요 및 GitHub 자동화
- [ISSUE_GUIDE.md](ISSUE_GUIDE.md) - 이슈 작성 상세 가이드

### 요구사항 및 설계
- [requirements/PROJECT_REQUIREMENTS.md](requirements/PROJECT_REQUIREMENTS.md) - 비즈니스 요구사항
- [architecture/ARCHITECTURE_ANALYSIS.md](architecture/ARCHITECTURE_ANALYSIS.md) - 아키텍처 패턴 비교
- [architecture/DOMAIN_MODEL_DESIGN.md](architecture/DOMAIN_MODEL_DESIGN.md) - 도메인 모델 설계
- [architecture/TECH_STACK_ANALYSIS.md](architecture/TECH_STACK_ANALYSIS.md) - 기술 스택 분석
- [adr/ADR_001_ARCHITECTURE_DECISION.md](adr/ADR_001_ARCHITECTURE_DECISION.md) - 최종 아키텍처 결정

---

## 참고 링크

- [GitHub CLI 문서](https://cli.github.com/manual/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

**Last Updated**: 2025-11-12