ALTER TABLE payment_entries
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN voided_at TIMESTAMPTZ,
    ADD COLUMN void_reason TEXT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX ix_payment_entries_active_appointment
    ON payment_entries(appointment_id, created_at)
    WHERE active = TRUE;

CREATE UNIQUE INDEX ux_payment_entries_one_active_pending
    ON payment_entries(appointment_id)
    WHERE active = TRUE AND payment_method = 'PENDING';
