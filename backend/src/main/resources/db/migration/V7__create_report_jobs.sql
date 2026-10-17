CREATE TABLE report_jobs
(
    id                  UUID PRIMARY KEY,
    requested_by        BIGINT NOT NULL,
    department_id       BIGINT NOT NULL,
    period_from         DATE NOT NULL,
    period_to_exclusive DATE NOT NULL,
    status              VARCHAR(255) NOT NULL,
    result_json         TEXT,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,

    CONSTRAINT chk_report_jobs_period
        CHECK (period_from < period_to_exclusive),
    CONSTRAINT chk_report_jobs_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_report_jobs_requested_by_created_at
    ON report_jobs (requested_by, created_at DESC);
