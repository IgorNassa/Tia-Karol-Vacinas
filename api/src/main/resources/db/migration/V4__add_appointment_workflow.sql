ALTER TABLE appointments
    ADD COLUMN reservation_status VARCHAR(30) NOT NULL DEFAULT 'RESERVED',
    ADD COLUMN cancellation_reason TEXT,
    ADD COLUMN reservation_resolution_reason TEXT,
    ADD COLUMN applied_at TIMESTAMPTZ;

ALTER TABLE appointments
    ADD CONSTRAINT ck_appointment_reservation_status
        CHECK (reservation_status IN ('RESERVED', 'CONSUMED', 'RELEASED', 'PENDING_DECISION'));

CREATE INDEX ix_appointments_pending_decision
    ON appointments(status, reservation_status)
    WHERE status = 'NO_SHOW' AND reservation_status = 'PENDING_DECISION';
