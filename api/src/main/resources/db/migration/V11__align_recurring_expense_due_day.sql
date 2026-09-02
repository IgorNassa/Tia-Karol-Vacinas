SET search_path TO app, public;

ALTER TABLE recurring_expenses
    ALTER COLUMN due_day TYPE INTEGER;
