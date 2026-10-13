#!/bin/bash
# 변경된 .java 가 있으면 종료 전에 컴파일 확인. 실패 시 exit 2로 Claude가 계속 고치게 한다.
# 재시도(stop_hook_active)에서도 Java가 바뀌었으면 다시 검사하고, 실패 때와 같으면 무한 루프 방지로 통과시킨다.
input=$(cat)
cd "$CLAUDE_PROJECT_DIR" || exit 0
git status --porcelain -- '*.java' | grep -q . || exit 0

hash=$({ git diff HEAD -- '*.java'; git ls-files -z -o --exclude-standard -- '*.java' | xargs -0 cat 2>/dev/null; } | shasum | cut -d' ' -f1)
state="${TMPDIR:-/tmp}/claude-compile-$(printf '%s' "$input" | jq -r '.session_id // "default"')"
if [ "$(printf '%s' "$input" | jq -r '.stop_hook_active')" = "true" ] && [ "$(cat "$state" 2>/dev/null)" = "$hash" ]; then
  exit 0
fi

if ! out=$(./gradlew compileJava compileTestJava -q 2>&1); then
  echo "$hash" > "$state"
  echo "컴파일 실패. 고친 뒤 종료할 것:" >&2
  echo "$out" | tail -30 >&2
  exit 2
fi
rm -f "$state"
