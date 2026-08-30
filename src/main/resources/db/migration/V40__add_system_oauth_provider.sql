-- 새 enum 값은 추가한 트랜잭션 안에서 사용할 수 없어 시드(V41)와 파일을 분리한다.
ALTER TYPE oauth_provider_enum ADD VALUE IF NOT EXISTS 'SYSTEM';
