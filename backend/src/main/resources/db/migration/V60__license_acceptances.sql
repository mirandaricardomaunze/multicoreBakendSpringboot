CREATE TABLE license_acceptances (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    license_version VARCHAR(40) NOT NULL,
    license_sha256 VARCHAR(64) NOT NULL,
    accepted_by VARCHAR(120) NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    declaration VARCHAR(500) NOT NULL,
    ip_address VARCHAR(64),
    client_version VARCHAR(40),
    user_agent VARCHAR(300),
    CONSTRAINT uk_license_acceptance_company_version UNIQUE (company_id, license_version)
);

CREATE INDEX idx_license_acceptance_company ON license_acceptances(company_id);
