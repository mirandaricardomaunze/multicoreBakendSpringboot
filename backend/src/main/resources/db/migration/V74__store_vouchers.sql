CREATE TABLE IF NOT EXISTS store_vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    initial_amount NUMERIC(14, 2) NOT NULL,
    remaining_amount NUMERIC(14, 2) NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    client_id BIGINT REFERENCES clients(id),
    client_name VARCHAR(150),
    credit_note_id BIGINT REFERENCES credit_notes(id),
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at DATE NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_vouchers_code ON store_vouchers(code);
CREATE INDEX IF NOT EXISTS idx_store_vouchers_company ON store_vouchers(company_id);
CREATE INDEX IF NOT EXISTS idx_store_vouchers_client ON store_vouchers(client_id);
CREATE INDEX IF NOT EXISTS idx_store_vouchers_status ON store_vouchers(status);
