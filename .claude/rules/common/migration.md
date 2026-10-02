---
paths:
  - "src/main/resources/db/migration/**"
  - "src/main/java/**/entity/**"
---
# DB 마이그레이션 규칙

- 스키마는 Flyway로만 바꾼다. local, test, prod 모두 `ddl-auto: validate`라서 엔티티만 바꾸고 마이그레이션을 빠뜨리면 기동이 실패한다.
- 파일명: `V{N}__snake_case_description.sql`. N은 `ls src/main/resources/db/migration | sort -V | tail -1`로 확인한 마지막 번호 + 1.
- **이미 커밋된 V 파일은 수정하지 않는다** (훅으로 차단됨). 수정이 필요하면 새 버전 파일로 ALTER한다.
- enum 컬럼 값을 추가하면 해당 CHECK 제약도 함께 갱신한다 (예: V45 `transportations_type_check`).
- 공간 컬럼은 `geography(Point, 4326)`처럼 SRID 4326을 명시한다.
