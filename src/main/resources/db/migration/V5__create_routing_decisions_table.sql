CREATE TABLE routing_decisions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    strategy VARCHAR(40) NOT NULL,
    selected_psp VARCHAR(40) NOT NULL,
    selected_score NUMERIC(10, 4) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    candidate_summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    modified_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_routing_decisions_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (id)
);

CREATE INDEX idx_routing_decisions_payment_id ON routing_decisions (payment_id);
CREATE INDEX idx_routing_decisions_selected_psp ON routing_decisions (selected_psp);
