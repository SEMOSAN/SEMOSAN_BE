# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication

- 한국어로 답변한다.
- "확인해줘", "봐줘", "뭐가 문제야" 같은 표현은 분석만 요청하는 것으로 간주하고 코드를 수정하지 않는다.
- "수정해줘", "고쳐줘", "반영해줘", "만들어줘", "추가해줘", "삭제해줘" 같은 명시적 요청이 있을 때만 파일을 편집한다.
- 수정 전에 어떤 파일을 왜 건드릴지 먼저 설명하고, 명시적 승인을 기다린다.

## Workflow

작업은 항상 아래 순서로 진행한다.

1. **분석**: 관련 코드·설정·로그를 먼저 파악한다.
2. **제안**: 원인과 해결 방향을 설명한다.
3. **비교**: 대안이 있으면 트레이드오프를 비교해 추천안을 제시한다.
4. **작업**: 사용자 승인 후에만 실제로 파일을 수정한다.
5. **마무리**: 변경 파일과 검증 내역을 요약하고, 커밋 메시지를 추천한다 (커밋은 사용자가 직접 한다).

## Build & Test Commands

```bash
./gradlew build                                        # 전체 빌드
./gradlew test                                         # 전체 테스트
./gradlew test --tests <fully.qualified.TestClass>     # 단일 클래스 테스트
./gradlew bootRun                                      # 애플리케이션 실행 (local 프로파일)
```

로컬 인프라(PostgreSQL, Redis, MinIO)가 없으면 테스트가 실패할 수 있다. 인프라 부재로 인한 실패와 코드 문제로 인한 실패를 명확히 구분해서 보고한다.

## Harness 구성 (`.claude/`)

| 경로 | 역할 | 로드 방식 |
|---|---|---|
| `rules/common/` | 전 도메인 공통 규칙 (API, 계층, 마이그레이션, 테스트, git) | 자동. `paths`가 있으면 해당 파일 작업 시에만 |
| `rules/domain/` | 고유 규칙이 있는 도메인만 (tracking, mountain, notification) | 해당 도메인 파일 작업 시 자동 |
| `docs/` | 참고 문서 | 필요할 때 직접 읽는다 (아래 목록) |
| `progress/` | 이슈별 작업 기록 (gitignore) | 이슈 브랜치에서 세션 시작 시 자동 |
| `commands/` | `/branch`, `/commit`, `/issue`, `/pr`, `/review`, `/refactor` | 사용자가 호출 |
| `settings.json`, `hooks/` | 가드레일 (아래) | 항상 |

필요할 때 읽을 문서:
- 패키지 구조, 인증, 인프라, 환경변수, 배포: `.claude/docs/architecture.md`
- GPS 트래킹 파이프라인 상세: `.claude/docs/tracking-flow.md`

가드레일 (우회하지 말 것):
- `git commit`, force push, `reset --hard`, `git clean` 차단
- 커밋된 Flyway 마이그레이션 파일 수정 차단
- `.env`, Firebase 키, k8s secret 읽기 차단
- 변경된 Java가 있으면 종료 시 컴파일 검사

도메인에 반복해서 설명하게 되는 함정이 생기면 `rules/domain/<도메인>.md`를 추가하자고 제안한다.
