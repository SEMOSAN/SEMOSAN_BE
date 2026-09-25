# Git 규칙

- Claude는 커밋하지 않는다 (`settings.json`에서 `git commit` 차단). 작업이 끝나면 커밋 메시지만 추천한다.
- 커밋 메시지: `<type>: <한글 메시지>`. type은 `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.
- DB 마이그레이션 변경은 애플리케이션 코드와 **별도 커밋**으로 추천한다.
- 브랜치명: `feat/#이슈번호-기능명`, `fix/#이슈번호-버그명`.
- PR: 변경 타입 라벨(`bug`/`enhancement`/`refactor`/`test`/`documentation`/`chore`) 지정, `gh pr create --assignee @me`. 리뷰어 2명 승인 후 머지.
- 시크릿, `application-local.yaml`, 생성 파일, 관련 없는 포맷 변경은 커밋 대상에 넣지 않는다.

## 진행 기록 (`.claude/progress/<이슈번호>.md`)
여러 세션에 걸치는 작업이면 이 파일에 기록한다. 이슈 브랜치에서 세션을 시작하면 자동으로 로드된다. gitignore 대상이다.

```markdown
# #123 기능명
## 목표
## 완료
## 남은 일
## 결정 사항 / 주의점
```
