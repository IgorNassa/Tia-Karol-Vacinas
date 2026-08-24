SET search_path TO app, public;

CREATE TABLE vaccines (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    vaccine_type VARCHAR(100),
    manufacturer VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_vaccines_business_key
    ON vaccines (LOWER(name), COALESCE(LOWER(vaccine_type), ''), COALESCE(LOWER(manufacturer), ''));
CREATE INDEX ix_vaccines_active_name ON vaccines (active, LOWER(name));

INSERT INTO vaccines (id, name, vaccine_type, manufacturer)
SELECT gen_random_uuid(), source.vaccine_name, source.vaccine_type, source.manufacturer
FROM (
    SELECT DISTINCT ON (LOWER(vaccine_name), COALESCE(LOWER(vaccine_type), ''), COALESCE(LOWER(manufacturer), ''))
           vaccine_name, vaccine_type, manufacturer
    FROM vaccine_lots
    ORDER BY LOWER(vaccine_name), COALESCE(LOWER(vaccine_type), ''), COALESCE(LOWER(manufacturer), ''), created_at
) source;

ALTER TABLE vaccine_lots ADD COLUMN vaccine_id UUID;

UPDATE vaccine_lots lot
SET vaccine_id = vaccine.id
FROM vaccines vaccine
WHERE LOWER(vaccine.name) = LOWER(lot.vaccine_name)
  AND COALESCE(LOWER(vaccine.vaccine_type), '') = COALESCE(LOWER(lot.vaccine_type), '')
  AND COALESCE(LOWER(vaccine.manufacturer), '') = COALESCE(LOWER(lot.manufacturer), '');

ALTER TABLE vaccine_lots
    ALTER COLUMN vaccine_id SET NOT NULL,
    ADD CONSTRAINT fk_vaccine_lots_vaccine FOREIGN KEY (vaccine_id) REFERENCES vaccines(id);

ALTER TABLE vaccine_lots DROP CONSTRAINT ux_vaccine_lots_name_lot;
CREATE UNIQUE INDEX ux_vaccine_lots_vaccine_lot_code ON vaccine_lots (vaccine_id, LOWER(lot_code));
CREATE INDEX ix_vaccine_lots_vaccine_id ON vaccine_lots (vaccine_id);
