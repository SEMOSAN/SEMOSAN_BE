#!/bin/bash
# HEAD에 커밋된 Flyway 마이그레이션 수정 차단. 새 파일(스테이징만 된 파일 포함)은 수정 허용.
f=$(jq -r '.tool_input.file_path // empty')
case "$f" in
  */src/main/resources/db/migration/V*.sql) ;;
  *) exit 0 ;;
esac
rel=${f#"$CLAUDE_PROJECT_DIR"/}
if git -C "$CLAUDE_PROJECT_DIR" cat-file -e "HEAD:$rel" 2>/dev/null; then
  echo "커밋된 마이그레이션($(basename "$f"))은 수정 금지: 체크섬이 바뀌면 Flyway validate가 실패한다. 새 V{N}__*.sql 파일을 추가할 것." >&2
  exit 2
fi
