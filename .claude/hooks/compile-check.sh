#!/bin/bash
# 변경된 .java 가 있으면 종료 전에 컴파일 확인. 실패 시 Claude가 계속 작업하도록 exit 2.
[ "$(jq -r '.stop_hook_active')" = "true" ] && exit 0
cd "$CLAUDE_PROJECT_DIR" || exit 0
git status --porcelain -- '*.java' | grep -q . || exit 0
if ! out=$(./gradlew compileJava compileTestJava -q 2>&1); then
  echo "컴파일 실패. 고친 뒤 종료할 것:" >&2
  echo "$out" | tail -30 >&2
  exit 2
fi
