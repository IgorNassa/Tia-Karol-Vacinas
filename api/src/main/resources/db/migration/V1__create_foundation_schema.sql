CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'ATTENDANT', 'APPLICATOR', 'FINANCIAL')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patients (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    identity_type VARCHAR(30) NOT NULL CHECK (identity_type IN ('CPF', 'FOREIGN_DOCUMENT', 'NEWBORN')),
    identity_number VARCHAR(50),
    birth_date DATE NOT NULL,
    phone VARCHAR(30) NOT NULL,
    allergies TEXT NOT NULL,
    allergies_confirmed BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    inactivated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_patient_allergies_confirmation CHECK (allergies_confirmed = TRUE),
    CONSTRAINT ck_patient_identity CHECK (
        (identity_type IN ('CPF', 'FOREIGN_DOCUMENT') AND identity_number IS NOT NULL)
        OR identity_type = 'NEWBORN'
    )
);

CREATE UNIQUE INDEX ux_patients_cpf
    ON patients (identity_number)
    WHERE identity_type = 'CPF';

CREATE TABLE patient_guardians (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    full_name VARCHAR(255) NOT NULL,
    identity_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patient_addresses (
    patient_id UUID PRIMARY KEY REFERENCES patients(id),
    postal_code VARCHAR(12) NOT NULL,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(30) NOT NULL,
    complement VARCHAR(255),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state CHAR(2) NOT NULL,
    ibge_code VARCHAR(20)
);

CREATE TABLE vaccine_lots (
    id UUID PRIMARY KEY,
    vaccine_name VARCHAR(255) NOT NULL,
    vaccine_type VARCHAR(100),
    lot_code VARCHAR(100) NOT NULL,
    expiration_date DATE NOT NULL,
    manufacturer VARCHAR(255),
    supplier VARCHAR(255),
    invoice_number VARCHAR(100),
    purchase_price NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (purchase_price >= 0),
    sale_price NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (sale_price >= 0),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_vaccine_lots_name_lot UNIQUE (vaccine_name, lot_code)
);

CREATE TABLE stock_balances (
    vaccine_lot_id UUID PRIMARY KEY REFERENCES vaccine_lots(id),
    physical_quantity INTEGER NOT NULL DEFAULT 0 CHECK (physical_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_stock_reserved_within_physical CHECK (reserved_quantity <= physical_quantity)
);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    vaccine_lot_id UUID NOT NULL REFERENCES vaccine_lots(id),
    movement_type VARCHAR(30) NOT NULL CHECK (movement_type IN ('ENTRY', 'RESERVATION', 'APPLICATION', 'CANCELLATION', 'RETURN', 'ADJUSTMENT', 'LOSS', 'EXPIRATION')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reason TEXT,
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_adjustment_has_reason CHECK (movement_type <> 'ADJUSTMENT' OR reason IS NOT NULL)
);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    vaccine_lot_id UUID NOT NULL REFERENCES vaccine_lots(id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'APPLIED', 'CANCELLED', 'NO_SHOW')),
    application_location VARCHAR(100),
    reactions TEXT,
    notes TEXT,
    gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (gross_amount >= 0),
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    final_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (final_amount >= 0),
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_entries (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('DEBIT_CARD', 'CREDIT_CARD', 'CASH', 'PENDING')),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES app_users(id),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    before_data JSONB,
    after_data JSONB,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_appointments_patient_id ON appointments(patient_id);
CREATE INDEX ix_appointments_vaccine_lot_id ON appointments(vaccine_lot_id);
CREATE INDEX ix_stock_movements_vaccine_lot_id ON stock_movements(vaccine_lot_id);
CREATE INDEX ix_audit_logs_entity ON audit_logs(entity_type, entity_id);
