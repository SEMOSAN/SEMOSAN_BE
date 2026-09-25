#!/bin/bash
# 이미 커밋된 Flyway 마이그레이션 수정 차단. 새로 만든(untracked) 파일은 수정 허용.
f=$(jq -r '.tool_input.file_path // empty')
case "$f" in
  */src/main/resources/db/migration/V*.sql) ;;
  *) exit 0 ;;
esac
if git -C "$CLAUDE_PROJECT_DIR" ls-files --error-unmatch "$f" >/dev/null 2>&1; then
  echo "커밋된 마이그레이션($(basename "$f"))은 수정 금지: 체크섬이 바뀌면 Flyway validate가 실패한다. 새 V{N}__*.sql 파일을 추가할 것." >&2
  exit 2
fi
