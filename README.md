# SEMOSAN_BE

세모산 백엔드 레포지토리입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.13
- PostgreSQL + PostGIS
- Redis

---

## 브랜치 전략

```
feat/#이슈번호-기능명
fix/#이슈번호-버그명
```

**예시**
```
feat/#12-mountain-search
fix/#34-trail-query-bug
```

---

## 커밋 컨벤션

| 타입 | 설명 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩토링 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 |
| `chore` | 빌드, 설정 변경 |

**예시**
```
feat: 산 검색 API 추가
fix: 등산로 조회 쿼리 오류 수정
```

---

## PR 규칙

- 리뷰어: 본인 제외 **2명 전원 승인** 후 머지
- 셀프 머지 금지

---

## Claude Code 사용 가이드

### 팀 공유 스킬 위치

팀에서 공유하는 커스텀 명령어(스킬)는 `.claude/commands/` 폴더에서 관리합니다.

```
.claude/
└── commands/
    └── {스킬명}.md
```

### 스킬 추가하는 법

1. `.claude/commands/` 폴더에 `스킬명.md` 파일 생성
2. 파일 안에 프롬프트 작성
3. git에 커밋 후 push → 팀원들이 pull하면 자동으로 공유됨

**예시** `.claude/commands/review.md`
```markdown
이 PR의 코드를 리뷰해줘.
- 보안 취약점 확인
- 네이밍 컨벤션 확인
- 불필요한 코드 확인
```

터미널에서 `/review` 로 호출 가능.

### CLAUDE.md 관리

프로젝트 루트의 `CLAUDE.md`는 Claude Code가 자동으로 읽는 팀 공통 가이드입니다.
기술 스택, 컨벤션, 주의사항 등 Claude에게 알려줄 내용을 여기에 작성합니다.
