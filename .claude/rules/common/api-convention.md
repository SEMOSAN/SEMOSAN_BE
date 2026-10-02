---
paths:
  - "src/main/java/**/controller/**"
  - "src/main/java/**/dto/**"
---
# API 규칙

- 모든 컨트롤러는 `ApiResponse<T>`로 응답한다.
  - `ApiResponse.success(SuccessStatus.XXX)` / `ApiResponse.success(SuccessStatus.XXX, data)`
- Swagger 어노테이션은 컨트롤러에 두지 않는다. `controller/docs/<Name>ControllerDocs` 인터페이스에 작성하고 컨트롤러가 구현한다.
- DTO 위치와 이름:
  - 요청: `dto/request/*Request.java` (또는 `dto/` 직하위)
  - 응답: `dto/response/*Response.java` (또는 `dto/` 직하위)
  - 서비스 간 전달: `dto/command/*Command.java`
- 새 엔드포인트가 인증 없이 열려야 하면 `SecurityConfig`의 permitAll 목록을 확인한다. 기본값은 JWT 필수다.
