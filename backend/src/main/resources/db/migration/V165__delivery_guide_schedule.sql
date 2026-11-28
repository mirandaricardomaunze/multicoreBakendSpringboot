-- Agenda de entregas das guias de remessa (docs/ENTREGAS_AGENDA_PROVA_SPEC.md §3.1, EN-01): estado da
-- entrega separado do estado da guia, dia, período (manhã/tarde) e viatura da frota com a matrícula impressa.
ALTER TABLE delivery_guides ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(20) NOT NULL DEFAULT 'UNSCHEDULED';
ALTER TABLE delivery_guides ADD COLUMN IF NOT EXISTS scheduled_date DATE;
ALTER TABLE delivery_guides ADD COLUMN IF NOT EXISTS scheduled_period VARCHAR(20);
ALTER TABLE delivery_guides ADD COLUMN IF NOT EXISTS delivery_vehicle_id BIGINT;
ALTER TABLE delivery_guides ADD COLUMN IF NOT EXISTS delivery_vehicle_label VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_delivery_guides_schedule
    ON delivery_guides (company_id, scheduled_date, delivery_vehicle_id);
