-- V76: Passagem de turno multi-operador
-- Tabela para registar reconciliações parciais (handover) entre operadores
-- dentro de uma mesma sessão de caixa.

CREATE TABLE shift_reconciliations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    till_session_id     BIGINT NOT NULL,
    outgoing_operator   VARCHAR(100) NOT NULL,
    incoming_operator   VARCHAR(100) NOT NULL,
    reconciled_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_cash       DECIMAL(14,2) NOT NULL,
    counted_cash        DECIMAL(14,2) NOT NULL,
    difference          DECIMAL(14,2) NOT NULL,
    cash_breakdown_json CLOB,
    notes               VARCHAR(500),
    created_by          VARCHAR(100) NOT NULL,
    company_id          BIGINT NOT NULL,
    CONSTRAINT fk_shift_recon_session FOREIGN KEY (till_session_id) REFERENCES till_sessions(id),
    CONSTRAINT fk_shift_recon_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE INDEX idx_shift_recon_session ON shift_reconciliations(till_session_id);
CREATE INDEX idx_shift_recon_company ON shift_reconciliations(company_id);

-- Campo para rastrear o operador actual da sessão (null = operator original)
ALTER TABLE till_sessions ADD COLUMN current_operator VARCHAR(100);
