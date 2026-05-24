-- =====================================================================
-- V13__add_free_post_search_index.sql
-- 목적: 자유게시판 제목/내용 검색 성능을 위한 pg_trgm GIN 인덱스
-- =====================================================================

-- 1) pg_trgm extension 활성화
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2) 제목 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_free_post_title_trgm
    ON free_posts
    USING GIN (title gin_trgm_ops);

-- 3) 본문 검색용 GIN 인덱스
CREATE INDEX IF NOT EXISTS idx_post_content_trgm
    ON posts
    USING GIN (content gin_trgm_ops);