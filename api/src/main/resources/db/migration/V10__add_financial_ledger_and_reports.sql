SET search_path TO app, public;

ALTER TABLE payment_entries
    ADD COLUMN received_at TIMESTAMPTZ,
    ADD COLUMN legacy_method VARCHAR(500);

UPDATE payment_entries
SET received_at = created_at
WHERE payment_method <> 'PENDING';

CREATE INDEX ix_payment_entries_active_received
    ON payment_entries (received_at, payment_method)
    WHERE active = TRUE AND payment_method <> 'PENDING';

CREATE TABLE recurring_expenses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    default_amount NUMERIC(12, 2) NOT NULL CHECK (default_amount > 0),
    variable_amount BOOLEAN NOT NULL DEFAULT FALSE,
    due_day SMALLINT NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE financial_entries (
    id UUID PRIMARY KEY,
    entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('INCOME', 'EXPENSE')),
    category VARCHAR(40) NOT NULL CHECK (category IN (
        'OTHER_INCOME', 'OTHER_EXPENSE', 'CASH_DEPOSIT', 'CASH_WITHDRAWAL',
        'RECURRING_EXPENSE', 'LEGACY_IMPORT'
    )),
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    occurred_on DATE NOT NULL,
    payment_method VARCHAR(30) CHECK (payment_method IN ('DEBIT_CARD', 'CREDIT_CARD', 'CASH')),
    notes TEXT,
    recurring_expense_id UUID REFERENCES recurring_expenses(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    voided_at TIMESTAMPTZ,
    void_reason TEXT,
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_financial_entry_void CHECK (
        (active = TRUE AND voided_at IS NULL AND void_reason IS NULL)
        OR (active = FALSE AND voided_at IS NOT NULL AND void_reason IS NOT NULL)
    ),
    CONSTRAINT ck_financial_entry_recurring CHECK (
        category <> 'RECURRING_EXPENSE' OR recurring_expense_id IS NOT NULL
    )
);

CREATE INDEX ix_recurring_expenses_created_by ON recurring_expenses(created_by);
CREATE INDEX ix_recurring_expenses_active_due ON recurring_expenses(due_day) WHERE active = TRUE;
CREATE INDEX ix_financial_entries_created_by ON financial_entries(created_by);
CREATE INDEX ix_financial_entries_recurring_expense ON financial_entries(recurring_expense_id)
    WHERE recurring_expense_id IS NOT NULL;
CREATE INDEX ix_financial_entries_active_date_type ON financial_entries(occurred_on, entry_type)
    WHERE active = TRUE;
