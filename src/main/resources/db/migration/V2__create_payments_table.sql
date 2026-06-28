CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    hold_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    amount NUMERIC(38, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE payment_seats (
    payment_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    PRIMARY KEY (payment_id, seat_id),
    CONSTRAINT fk_payment_seats_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (id) ON DELETE CASCADE
);
