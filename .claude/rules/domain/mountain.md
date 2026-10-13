---
paths:
  - "src/main/java/**/domain/mountain/**"
  - "src/test/java/**/domain/mountain/**"
  - "src/main/java/**/domain/admin/**/AdminMountain*"
  - "src/main/java/**/domain/admin/**/AdminCourse*"
---
# mountain 도메인 (PostGIS)

- `Mountain.location`은 `geography(Point, 4326)`, `Course.polyline`은 LineString이다. JTS 객체를 만들 때 SRID 4326을 빠뜨리면 geography 컬럼과 맞지 않아 저장이나 비교가 실패한다.
- 좌표 순서는 **경도(lng), 위도(lat)**다: `ST_MakePoint(:lng, :lat)`, JTS `new Coordinate(lng, lat)`. 파라미터 순서가 바뀌어도 에러 없이 틀린 결과가 나오므로 특히 주의한다.
- 거리 쿼리는 기존 기준을 따른다: 최근접은 `<->` + `::geography`, 반경은 `ST_DWithin`(geography), 지도 bbox는 `&& ST_MakeEnvelope(...)`. 새 쿼리가 다른 기준을 쓰면 정렬이나 반경 결과가 기존 API와 달라진다.
- 코스 경로는 `ST_AsGeoJSON` 문자열을 그대로 응답에 넘긴다 (역직렬화하지 않음).
- 공간 쿼리를 바꾸면 `MountainNearbyQueryRepositoryTest` 같은 실제 DB 테스트로 검증한다. 단위 테스트로는 검증되지 않는다.
