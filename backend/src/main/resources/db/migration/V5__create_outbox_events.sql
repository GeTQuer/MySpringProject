CREATE TABLE outbox_events
(
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    task_id      BIGINT NOT NULL,
    payload      JSONB NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;