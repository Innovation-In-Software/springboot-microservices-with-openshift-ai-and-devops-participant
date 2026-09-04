CREATE TABLE accounts (
    account_id   VARCHAR(36)  PRIMARY KEY,
    customer_id  VARCHAR(32)  NOT NULL,
    account_type VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    nickname     VARCHAR(80),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    closed_at    TIMESTAMPTZ
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX idx_accounts_status ON accounts (status);
