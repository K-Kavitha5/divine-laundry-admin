CREATE TABLE payment_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(40) NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(12) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    provider VARCHAR(80),
    provider_reference VARCHAR(160),
    provider_payment_id VARCHAR(160),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    created_by VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    failure_reason VARCHAR(500),
    CONSTRAINT fk_payment_request_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id)
);

CREATE INDEX idx_payment_requests_order_status ON payment_requests(order_id, status);
CREATE UNIQUE INDEX idx_payment_requests_provider_reference ON payment_requests(provider_reference);
CREATE UNIQUE INDEX idx_payment_requests_provider_payment_id ON payment_requests(provider_payment_id);
