# DB Migration Scripts

본 폴더는 자동화된 마이그레이션 도구(Flyway/Liquibase)가 도입되기 전,
수동으로 실행해야 하는 일회성 DB 변경 스크립트를 보관합니다.

## 실행 원칙
- 파일명 prefix(`001_`, `002_`, ...) 순서대로 실행합니다.
- 모든 스크립트는 `IF NOT EXISTS` / `WHERE ... IS NULL` 등 idempotent 하게 작성합니다.
- 운영 적용은 백엔드 PR 머지 직후 인프라 담당자가 수행합니다.

---

## 001_postgis_setup.sql

### 목적
트래킹 기능(가까운 산 검색, GPS 좌표 처리)에 사용할 PostGIS extension 도입 및
기존 `mountains` 테이블의 `latitude/longitude` 값을 PostGIS `location` 컬럼으로
백필합니다.

### 사전 조건
- PostgreSQL 컨테이너가 PostGIS extension 이 설치된 이미지여야 합니다.
  - 권장: `postgis/postgis:16-3.4` (또는 호환 버전)
  - 일반 `postgres:XX` 이미지는 `CREATE EXTENSION postgis` 시 에러 발생

### 로컬 적용
```bash
# 1) PostGIS 이미지 컨테이너로 띄워져 있는지 확인
docker ps --filter "name=semosan-postgis"

# 2) SQL 실행
docker exec -i semosan-postgis psql -U semosan -d semosan \
  < src/main/resources/db/migration/001_postgis_setup.sql
```

### 운영(K8s) 적용 가이드

1. **PostgreSQL StatefulSet/Deployment 이미지 교체**
   - 현재 `k8s/postgres/deployment.yaml` 의 image 를 PostGIS 이미지로 변경
     (예: `postgis/postgis:16-3.4`).
   - PVC 는 그대로 유지 (데이터 보존).
   - 롤링 업데이트 또는 재기동.

2. **SQL 실행**
   - `kubectl exec -it <postgres-pod> -- psql -U <user> -d <db> -f /path/to/001_postgis_setup.sql`
   - 또는 Job 으로 일회성 실행.

3. **검증**
   ```sql
   SELECT extname, extversion FROM pg_extension WHERE extname = 'postgis';
   SELECT COUNT(*) FROM mountains WHERE location IS NULL;  -- 0 이어야 함
   ```

### 주의
- PostGIS extension 활성화는 DB 슈퍼유저 권한 필요할 수 있음.
- 운영 DB 백업 후 적용 권장.
