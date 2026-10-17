ALTER TABLE report_jobs
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN attempt_id UUID;

ALTER TABLE report_jobs
    ADD CONSTRAINT chk_report_jobs_attempt_count
        CHECK (attempt_count >= 0);

ALTER TABLE report_jobs
    DROP CONSTRAINT chk_report_jobs_status;

ALTER TABLE report_jobs
    ADD CONSTRAINT chk_report_jobs_status
        CHECK (status IN (
            'PENDING', 'PROCESSING', 'COMPLETED', 'RETRY_PENDING', 'FAILED'
        ));

CREATE INDEX idx_report_jobs_retry_due
    ON report_jobs (next_attempt_at)
    WHERE status = 'RETRY_PENDING';
