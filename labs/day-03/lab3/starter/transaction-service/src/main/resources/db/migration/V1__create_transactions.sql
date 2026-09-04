CREATE TABLE transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    account_id     VARCHAR(36) NOT NULL,
    amount         NUMERIC(18, 2) NOT NULL,
    currency       VARCHAR(3)  NOT NULL,
    type           VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    event_id       VARCHAR(64) NOT NULL UNIQUE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_status ON transactions (status);

CREATE TABLE processed_events (
    event_id       VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL
);
