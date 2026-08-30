-- =====================================================================
-- V42__add_mountain_search_index.sql
-- 목적: 산 이름/주소 검색(LIKE '%keyword%') 성능을 위한 pg_trgm GIN 인덱스
--       (자유게시판 V13 선례와 동일한 패턴)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_mountains_name_trgm
    ON mountains
    USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_mountains_address_trgm
    ON mountains
    USING GIN (address gin_trgm_ops);
