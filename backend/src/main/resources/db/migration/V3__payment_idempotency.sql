ALTER TABLE payments ADD COLUMN client_request_id VARCHAR(80);

CREATE UNIQUE INDEX uq_payments_client_request ON payments(client_request_id);
