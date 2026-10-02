---
paths:
  - "src/test/**"
---
# 테스트 규칙

- 기본은 `@ExtendWith(MockitoExtension.class)` 단위 테스트다. 컨트롤러도 MockMvc 없이 `@InjectMocks`로 직접 호출하고 `ResponseEntity<ApiResponse<T>>`의 상태와 data를 검증한다.
- `@SpringBootTest`는 실제 Postgres(PostGIS), Redis가 필요한 쿼리와 통합 검증에만 쓴다 (예: `*QueryRepositoryTest`, `*RedisIntegrationTest`).
- 테스트 경로는 main과 같은 패키지 구조를 따른다.
- 커버리지 게이트는 `jacocoTestCoverageVerification`(최소 0.90)이며 `check`에 걸려 있다. 새 로직에는 테스트를 함께 추가한다.
- 좁은 범위부터 실행한다: `./gradlew test --tests <FQCN>` → `./gradlew test`.
- 인프라(Postgres, Redis, MinIO)가 없어 실패한 것과 코드 때문에 실패한 것을 구분해서 보고한다.
