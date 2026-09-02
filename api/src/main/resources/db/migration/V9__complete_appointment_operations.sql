SET search_path TO app, public;

ALTER TABLE appointments
    ADD COLUMN rescheduled_at TIMESTAMPTZ,
    ADD COLUMN reschedule_reason TEXT;

CREATE INDEX ix_appointments_scheduled_at ON appointments (scheduled_at);
CREATE INDEX ix_appointments_status_scheduled_at ON appointments (status, scheduled_at);
