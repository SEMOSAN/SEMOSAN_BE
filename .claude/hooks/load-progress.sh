#!/bin/bash
# 브랜치명(feat/#123-xxx)의 이슈 번호로 .claude/progress/<번호>.md 를 세션 컨텍스트에 주입
branch=$(git -C "$CLAUDE_PROJECT_DIR" branch --show-current 2>/dev/null)
issue=$(echo "$branch" | sed -nE 's|.*#([0-9]+).*|\1|p')
[ -z "$issue" ] && exit 0
f="$CLAUDE_PROJECT_DIR/.claude/progress/$issue.md"
if [ -f "$f" ]; then
  echo "## 이슈 #$issue 진행 상황 (.claude/progress/$issue.md)"
  cat "$f"
else
  echo "이슈 #$issue 브랜치. 진행 상황 파일(.claude/progress/$issue.md)이 아직 없다. 여러 세션에 걸칠 작업이면 만들어서 기록할 것."
fi
