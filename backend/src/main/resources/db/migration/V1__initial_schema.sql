CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    alternate_phone VARCHAR(20),
    email VARCHAR(160),
    address_line VARCHAR(255),
    area VARCHAR(120),
    notes VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE laundry_services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    catalog_group VARCHAR(80),
    pricing_unit VARCHAR(20) NOT NULL,
    unit_rate DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE laundry_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    order_number VARCHAR(40) UNIQUE,
    invoice_number VARCHAR(40) UNIQUE,
    client_request_id VARCHAR(80) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    work_status VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'WALK_IN',
    placed_at TIMESTAMP NOT NULL,
    pickup_at TIMESTAMP,
    delivery_at TIMESTAMP,
    notes VARCHAR(800),
    subtotal DECIMAL(12,2) NOT NULL,
    discount DECIMAL(12,2) NOT NULL,
    tax DECIMAL(12,2) NOT NULL,
    round_off DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    service_id BIGINT,
    service_code VARCHAR(40) NOT NULL,
    service_name VARCHAR(160) NOT NULL,
    pricing_unit VARCHAR(20) NOT NULL,
    unit_rate DECIMAL(12,2) NOT NULL,
    billable_quantity DECIMAL(12,3) NOT NULL,
    piece_count INT NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    no_print BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id),
    CONSTRAINT fk_item_service FOREIGN KEY (service_id) REFERENCES laundry_services(id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_number VARCHAR(40) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    mode VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(120),
    amount DECIMAL(12,2) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id)
);

CREATE TABLE garment_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_number VARCHAR(60) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    piece_sequence INT NOT NULL,
    print_count INT NOT NULL DEFAULT 0,
    last_printed_at TIMESTAMP,
    CONSTRAINT fk_tag_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id),
    CONSTRAINT fk_tag_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
);

CREATE TABLE invoice_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    image_storage_key VARCHAR(255),
    pdf_storage_key VARCHAR(255),
    content_sha256 VARCHAR(64),
    generated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_invoice_asset_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id)
);

CREATE TABLE whatsapp_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deduplication_key VARCHAR(120) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    template_name VARCHAR(120),
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    media_storage_key VARCHAR(255),
    provider_message_id VARCHAR(160),
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    CONSTRAINT fk_whatsapp_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id)
);

CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(80) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    CONSTRAINT fk_status_history_order FOREIGN KEY (order_id) REFERENCES laundry_orders(id)
);

CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_date DATE NOT NULL,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    payment_mode VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor VARCHAR(80) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    details VARCHAR(1000),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_customer_status ON laundry_orders(customer_id, work_status);
CREATE INDEX idx_orders_delivery ON laundry_orders(delivery_at);
CREATE INDEX idx_items_service ON order_items(service_code);
CREATE INDEX idx_whatsapp_order_status ON whatsapp_messages(order_id, delivery_status);
CREATE INDEX idx_expenses_date ON expenses(expense_date);

INSERT INTO laundry_services(code, name, category, pricing_unit, unit_rate) VALUES
('WASH_IRON_KG', 'Wash & Iron', 'Laundry by KG', 'KG', 120.00),
('WASH_FOLD_KG', 'Wash & Fold', 'Laundry by KG', 'KG', 90.00),
('WASH_FOLD_EXPRESS_KG', 'Wash & Fold - Express', 'Laundry by KG', 'KG', 100.00),
('WASH_IRON_EXPRESS_KG', 'Wash & Iron - Express', 'Laundry by KG', 'KG', 150.00),
('SHIRT_IRON', 'Shirt - Ironing', 'Ironing', 'PIECE', 14.00),
('TSHIRT_IRON', 'T-Shirt - Ironing', 'Ironing', 'PIECE', 14.00),
('PANT_IRON', 'Pant / Trouser - Ironing', 'Ironing', 'PIECE', 14.00),
('LONG_DRESS_IRON', 'Long Dress - Ironing', 'Ironing', 'PIECE', 20.00),
('PILLOW_COVER_IRON', 'Pillow Cover - Ironing', 'Ironing', 'PIECE', 10.00),
('COAT_BLAZER_IRON', 'Coat / Blazer - Ironing', 'Ironing', 'PIECE', 100.00),
('OVERCOAT_IRON', 'Over Coat - Ironing', 'Ironing', 'PIECE', 50.00),
('SPORT_SHOE', 'Sports Shoe Cleaning', 'Shoe Cleaning', 'PIECE', 249.00),
('CANVAS_SHOE', 'Canvas Shoe Cleaning', 'Shoe Cleaning', 'PIECE', 249.00),
('LEATHER_SHOE', 'Leather Shoe Cleaning', 'Shoe Cleaning', 'PIECE', 249.00),
('SUEDE_SHOE', 'Suede Shoe Cleaning', 'Shoe Cleaning', 'PIECE', 249.00),
('CROCS_SANDALS', 'Crocs / Sandals Cleaning', 'Shoe Cleaning', 'PIECE', 120.00),
('SLIPPERS', 'Slippers Cleaning', 'Shoe Cleaning', 'PIECE', 199.00),
('SOFA_SEAT', 'Sofa Cleaning - 1 Seater', 'Sofa Cleaning', 'PIECE', 190.00);
