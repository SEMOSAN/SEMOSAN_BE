ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_pk;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_active_oauth
    ON users (oauth_id, oauth_provider)
    WHERE is_deleted = false;
