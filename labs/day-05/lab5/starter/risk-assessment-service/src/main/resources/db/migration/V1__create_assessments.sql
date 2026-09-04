CREATE TABLE assessments (
    assessment_id      VARCHAR(36) PRIMARY KEY,
    transaction_id     VARCHAR(36) NOT NULL UNIQUE,
    account_id         VARCHAR(36) NOT NULL,
    amount             NUMERIC(18, 2) NOT NULL,
    currency           VARCHAR(3)  NOT NULL,
    model_name         VARCHAR(80),
    model_version      VARCHAR(40),
    model_score        INTEGER,
    model_status       VARCHAR(20) NOT NULL,
    policy_version     VARCHAR(40) NOT NULL,
    disposition        VARCHAR(20) NOT NULL,
    policy_reason      VARCHAR(40) NOT NULL,
    review_status      VARCHAR(20) NOT NULL,
    reviewer_decision  VARCHAR(20),
    reviewer_reason    VARCHAR(200),
    correlation_id     VARCHAR(64) NOT NULL,
    event_id           VARCHAR(64) NOT NULL UNIQUE,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_assessments_disposition ON assessments (disposition);
CREATE INDEX idx_assessments_review_status ON assessments (review_status);

CREATE TABLE processed_events (
    event_id       VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL
);
