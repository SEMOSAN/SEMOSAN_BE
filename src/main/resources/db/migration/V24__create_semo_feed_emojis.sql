CREATE TABLE IF NOT EXISTS semo_feed_emojis
(
    id           BIGSERIAL    PRIMARY KEY,
    semo_feed_id BIGINT       NOT NULL REFERENCES semo_feeds (id) ON DELETE CASCADE,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    emoji_type   VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_semo_feed_emoji_feed_user_type UNIQUE (semo_feed_id, user_id, emoji_type),
    CONSTRAINT ck_semo_feed_emojis_type CHECK (emoji_type IN ('FIRE', 'HEART', 'CONGRATS', 'LAUGH'))
);

CREATE INDEX IF NOT EXISTS idx_semo_feed_emojis_feed
    ON semo_feed_emojis (semo_feed_id);

CREATE INDEX IF NOT EXISTS idx_semo_feed_emojis_user
    ON semo_feed_emojis (user_id);
