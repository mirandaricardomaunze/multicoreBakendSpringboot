-- Modelo de documento por empresa (docs/MODELOS_DOCUMENTO_SPEC.md §3.1, DT-02).
-- Estende document_column_config com cor, posição do logótipo, notas e QR; as contas bancárias
-- (até 3, ordenadas) ficam numa tabela filha.
ALTER TABLE document_column_config ADD COLUMN IF NOT EXISTS accent_color VARCHAR(7);
ALTER TABLE document_column_config ADD COLUMN IF NOT EXISTS logo_position VARCHAR(10) NOT NULL DEFAULT 'LEFT';
ALTER TABLE document_column_config ADD COLUMN IF NOT EXISTS template_notes VARCHAR(500);
ALTER TABLE document_column_config ADD COLUMN IF NOT EXISTS show_qr_code BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE document_column_config DROP CONSTRAINT IF EXISTS ck_doc_col_logo_position;
ALTER TABLE document_column_config ADD CONSTRAINT ck_doc_col_logo_position
    CHECK (logo_position IN ('LEFT', 'CENTER', 'RIGHT'));

CREATE TABLE IF NOT EXISTS document_template_bank_accounts (
    config_id      BIGINT       NOT NULL REFERENCES document_column_config(id) ON DELETE CASCADE,
    sort_order     INTEGER      NOT NULL,
    bank_name      VARCHAR(100) NOT NULL,
    holder         VARCHAR(150) NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    PRIMARY KEY (config_id, sort_order)
);
