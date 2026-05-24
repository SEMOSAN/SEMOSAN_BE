CREATE TABLE IF NOT EXISTS semo_feeds
(
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    image_url  VARCHAR(500) NOT NULL,
    is_public  BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_semo_feeds_user
    ON semo_feeds (user_id);

CREATE INDEX IF NOT EXISTS idx_semo_feeds_public
    ON semo_feeds (is_public)
    WHERE is_public = true;
