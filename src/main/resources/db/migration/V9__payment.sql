-- 결제/예약 도메인 스키마
-- Mock 항공권/숙소 결제 플로우를 먼저 완성하되, 외부 공급자 API 연동으로 확장할 수 있게
-- offer snapshot -> order -> PG transaction -> supplier booking 순서로 저장한다.

CREATE TABLE travel_offer (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_plan_id      BIGINT       NOT NULL,
    provider          VARCHAR(30)  NOT NULL,
    offer_type        VARCHAR(20)  NOT NULL,
    external_offer_id VARCHAR(120),
    name              VARCHAR(200) NOT NULL,
    description       VARCHAR(1000),
    price             BIGINT       NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    valid_until       TIMESTAMP,
    raw_payload       TEXT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    deleted_at        TIMESTAMP,
    CONSTRAINT fk_travel_offer_trip_plan FOREIGN KEY (trip_plan_id) REFERENCES trip_plan (id)
);
CREATE INDEX idx_travel_offer_trip_plan_id ON travel_offer (trip_plan_id);
CREATE INDEX idx_travel_offer_provider_type ON travel_offer (provider, offer_type);

CREATE TABLE payment_order (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    trip_plan_id   BIGINT       NOT NULL,
    order_id       VARCHAR(100) NOT NULL,
    order_name     VARCHAR(200) NOT NULL,
    total_amount   BIGINT       NOT NULL,
    currency       VARCHAR(3)   NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    deleted_at     TIMESTAMP,
    CONSTRAINT uk_payment_order_order_id UNIQUE (order_id),
    CONSTRAINT fk_payment_order_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payment_order_trip_plan FOREIGN KEY (trip_plan_id) REFERENCES trip_plan (id)
);
CREATE INDEX idx_payment_order_user_id ON payment_order (user_id);
CREATE INDEX idx_payment_order_trip_plan_id ON payment_order (trip_plan_id);
CREATE INDEX idx_payment_order_status ON payment_order (status);

CREATE TABLE payment_order_item (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT       NOT NULL,
    travel_offer_id  BIGINT       NOT NULL,
    item_name        VARCHAR(200) NOT NULL,
    item_type        VARCHAR(20)  NOT NULL,
    amount           BIGINT       NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    deleted_at       TIMESTAMP,
    CONSTRAINT fk_payment_order_item_order FOREIGN KEY (payment_order_id) REFERENCES payment_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_order_item_offer FOREIGN KEY (travel_offer_id) REFERENCES travel_offer (id)
);
CREATE INDEX idx_payment_order_item_order_id ON payment_order_item (payment_order_id);
CREATE INDEX idx_payment_order_item_offer_id ON payment_order_item (travel_offer_id);

CREATE TABLE payment_transaction (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT      NOT NULL,
    payment_key      VARCHAR(200),
    method           VARCHAR(50),
    status           VARCHAR(30) NOT NULL,
    amount           BIGINT      NOT NULL,
    currency         VARCHAR(3)  NOT NULL,
    receipt_url      VARCHAR(500),
    approved_at      TIMESTAMP,
    fail_code        VARCHAR(100),
    fail_message     VARCHAR(500),
    raw_payload      TEXT,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,
    deleted_at       TIMESTAMP,
    CONSTRAINT uk_payment_transaction_payment_key UNIQUE (payment_key),
    CONSTRAINT fk_payment_transaction_order FOREIGN KEY (payment_order_id) REFERENCES payment_order (id) ON DELETE CASCADE
);
CREATE INDEX idx_payment_transaction_order_id ON payment_transaction (payment_order_id);
CREATE INDEX idx_payment_transaction_status ON payment_transaction (status);

CREATE TABLE supplier_booking (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_order_id        BIGINT      NOT NULL,
    provider                VARCHAR(30) NOT NULL,
    external_reservation_id VARCHAR(120),
    status                  VARCHAR(30) NOT NULL,
    paid_amount             BIGINT      NOT NULL,
    currency                VARCHAR(3)  NOT NULL,
    requested_at            TIMESTAMP   NOT NULL,
    confirmed_at            TIMESTAMP,
    fail_reason             VARCHAR(1000),
    raw_payload             TEXT,
    created_at              TIMESTAMP   NOT NULL,
    updated_at              TIMESTAMP   NOT NULL,
    deleted_at              TIMESTAMP,
    CONSTRAINT fk_supplier_booking_order FOREIGN KEY (payment_order_id) REFERENCES payment_order (id) ON DELETE CASCADE
);
CREATE INDEX idx_supplier_booking_order_id ON supplier_booking (payment_order_id);
CREATE INDEX idx_supplier_booking_provider_status ON supplier_booking (provider, status);
