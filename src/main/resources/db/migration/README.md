# Flyway Migration Guide

이 디렉터리에는 운영 DB 스키마 변경 SQL을 추가합니다.

- 한 번 배포된 migration 파일은 수정하지 않습니다.
- 변경이 필요하면 새 버전의 migration 파일을 추가합니다.
- 파일명은 `V{version}__{description}.sql` 형식을 사용합니다.
- 엔티티 변경과 대응되는 DDL migration은 같은 PR에 포함합니다.
- destructive DDL은 컬럼 추가, 코드 배포, 데이터 백필, 제약 강화 순서로 나눠서 적용합니다.

예시:

```text
V2__add_review_image_url.sql
V3__create_hiking_record_table.sql
```
