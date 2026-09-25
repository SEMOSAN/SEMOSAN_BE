#!/bin/bash
# Bash 명령 전체 텍스트에서 금지된 git 작업 차단.
# settings.json deny 규칙이 못 잡는 형태(git -C . commit, git push origin --force, sh -c '...')까지 검사한다.
cmd=$(jq -r '.tool_input.command // empty')
# git + 전역 옵션(-C 경로, -c k=v, --xxx) 뒤의 서브커맨드 위치
g="(^|[^[:alnum:]_.-])git([[:space:]]+(-[Cc][[:space:]]+[^[:space:]]+|-[^[:space:]]+))*[[:space:]]+['\"]?"
has() { printf '%s' "$cmd" | grep -qE "$1"; }

if has "${g}commit(['\"]|[[:space:]]|$|[;&|)])"; then
  reason="git commit 금지 (커밋은 사용자가 직접 한다. 메시지만 추천할 것)"
elif has "${g}push(['\"])?[^;&|]*[[:space:]](--force|-f([[:space:]]|$)|\+[^[:space:]])"; then
  reason="force push 금지"
elif has "${g}reset(['\"])?[[:space:]][^;&|]*--hard"; then
  reason="git reset --hard 금지"
elif has "${g}clean(['\"]|[[:space:]]|$)"; then
  reason="git clean 금지"
else
  exit 0
fi
echo "차단됨: $reason" >&2
exit 2
