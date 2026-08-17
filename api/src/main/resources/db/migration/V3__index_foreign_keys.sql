SET search_path TO app, public;

CREATE INDEX ix_appointments_created_by ON appointments(created_by);
CREATE INDEX ix_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX ix_patient_guardians_patient_id ON patient_guardians(patient_id);
CREATE INDEX ix_payment_entries_appointment_id ON payment_entries(appointment_id);
CREATE INDEX ix_payment_entries_created_by ON payment_entries(created_by);
CREATE INDEX ix_stock_movements_created_by ON stock_movements(created_by);
