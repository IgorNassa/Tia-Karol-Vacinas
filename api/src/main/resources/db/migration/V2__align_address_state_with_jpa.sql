SET search_path TO app, public;

ALTER TABLE patient_addresses
    ALTER COLUMN state TYPE VARCHAR(2);
