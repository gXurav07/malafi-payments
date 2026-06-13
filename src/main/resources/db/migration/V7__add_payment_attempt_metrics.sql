ALTER TABLE payment_attempts
    ADD COLUMN latency_ms BIGINT,
    ADD COLUMN cost NUMERIC(10, 2);
