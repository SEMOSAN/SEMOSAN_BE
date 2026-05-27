# Gemini Code Assist Review Guide

## Language
- Always write review comments in Korean.
- Keep comments direct, practical, and specific to the current codebase.

## Review Priority
- Prioritize findings in this order: bugs, data consistency, security, performance, design, maintainability, readability.
- Focus on backend behavior, API compatibility, persistence behavior, integration risks, and missing tests.
- Avoid nitpicks and preference-only comments unless they hide a real maintainability risk.

## Project Context
- This is a Spring Boot backend project.
- Use Java 21 and Gradle.
- Follow the existing package structure, naming, and style.
- Prefer existing services, repositories, DTOs, response conventions, and package boundaries over new abstractions.
- Keep suggested changes narrowly scoped to the pull request.

## Backend Review Rules
- Verify Controller, Service, Repository, DTO, and Entity responsibilities are separated clearly.
- Business logic should live in the appropriate Service or domain model, not in controllers.
- Entity state changes should use clear behavior methods instead of uncontrolled public setters.
- Check soft delete behavior, nullable policies, enum expansion, and DB constraints for consistency.
- Avoid duplicating business rules, payload construction, validation checks, formatting, or helper logic across services.
- Suggest a shared service/helper only when it removes meaningful duplication, reduces real complexity, or matches an existing local pattern.
- Check exception handling for consistency between business exceptions and system exceptions.
- Avoid meaningless `RuntimeException` usage and unnecessary try-catch blocks.
- Review transaction boundaries, `readOnly` usage, exception propagation, and rollback behavior carefully.
- When reviewing notification, messaging, FCM, or external side effects, verify failures do not unintentionally roll back the main business operation unless intended.
- Check for unnecessary queries, N+1 risks, lazy loading issues, missing indexes, and inefficient count/existence checks.
- For user-specific state, prefer ID-based repository queries when loading the full entity is unnecessary.
- Check race condition risks, duplicate creation risks, and whether DB unique constraints support application-level validation.
- Verify authentication, JWT handling, refresh token storage, external token validation, and sensitive logging.
- If secrets, tokens, DB passwords, JWT secrets, or other sensitive values appear in code, logs, screenshots, or config, warn clearly.
- Verify request/response DTO usage and avoid returning entities directly from APIs.
- For backend/frontend integration changes, separate what the backend expects, what the frontend must send, and what environment/config values must match.

## Test Review
- Check whether new behavior has focused tests.
- Prefer focused tests before broad tests for narrow changes.
- Test method names must be written in English.
- Test comments should use `given`, `when`, `then` only when comments are necessary.
- If tests cannot be executed due to local infrastructure or existing unrelated failures, state that separately from issues caused by the PR.

## Review Style
- Lead with findings ordered by severity.
- Use severity labels such as `[P1]`, `[P2]`, and `[P3]` when helpful.
- For each finding, explain the problem, why it matters, and a concrete improvement.
- Include the affected file, method, or behavior whenever possible.
- Keep each review comment concise and actionable.
- Use one comment per issue.
- Do not repeat the same issue in multiple comments.
- If suggesting code, keep the snippet minimal.
- Do not wrap the entire review comment in a code block.
- If a suggestion is optional, say so explicitly.
- If there are no issues, say that clearly and mention any remaining test gaps or residual risk.

## Commenting Convention
- Use `FIX ME` only when the code has a real potential issue but immediate exception handling or implementation is intentionally deferred.
- Use `FIX ME` for unclear business requirements that must be revisited.
