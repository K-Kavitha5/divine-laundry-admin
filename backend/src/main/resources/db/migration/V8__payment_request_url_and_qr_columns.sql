ALTER TABLE payment_requests
    ADD COLUMN payment_url VARCHAR(500) NULL;

ALTER TABLE payment_requests
    ADD COLUMN qr_payload VARCHAR(2000) NULL;
