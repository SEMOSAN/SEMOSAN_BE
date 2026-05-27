# AGENTS.md

## Communication
- Always reply in Korean unless the user explicitly asks for another language.
- Keep answers direct and practical. Avoid repeating the same explanation after the user has already acknowledged it.
- When the user asks "확인해줘", "봐줘", "어떻게 돼", "뭐가 문제야", or similar, treat it as analysis-only unless they explicitly ask for code changes.
- Do not edit files unless the user clearly says something like "수정해줘", "고쳐줘", "반영해줘", "만들어줘", "추가해줘", or "삭제해줘".
- If a fix is likely, explain the cause, affected files, and proposed patch first. Wait for explicit approval before applying it.
- If you accidentally changed code without approval, say exactly which files changed and offer to revert only your own changes.

## User Preferences
- The user wants investigation before implementation.
- The user dislikes unrequested code edits.
- The user prefers concrete answers based on the current codebase, not generic guesses.
- When diagnosing backend/frontend integration issues, clearly separate:
  - what the backend currently expects
  - what the frontend must send
  - what environment/config values must match
  - what is only an assumption
- If sensitive values such as secrets, tokens, DB passwords, or JWT secrets appear in screenshots or logs, warn briefly and recommend rotation if they may have been shared.

## Project
- This is a Spring Boot backend project.
- Use Java 21 and Gradle.
- Follow the existing package structure, naming, and style.
- Prefer existing services, repositories, DTOs, and response conventions over introducing new abstractions.
- When adding code, avoid duplicating business rules, payload construction, validation checks, formatting, or helper logic across services. Extract shared behavior into an appropriate existing service/helper or a narrowly scoped new component, and keep callers focused on orchestration.

## Commands
- Use `rg` first for searching.
- Run focused tests before broad tests when checking a narrow change.
- Common commands:
  - `./gradlew test --tests <fully.qualified.TestClass>`
  - `./gradlew test`
  - `./gradlew build`
- If full tests fail because local infrastructure such as PostgreSQL or Redis is unavailable, report that clearly and distinguish it from failures caused by the code change.

## Editing Rules
- Never revert or overwrite user changes unless explicitly requested.
- Keep changes narrowly scoped to the requested task.
- Do not commit secrets, environment values, generated local files, or unrelated formatting churn.
- Before editing, state what files will be touched and why.
- After editing, summarize the exact files changed and verification performed.
