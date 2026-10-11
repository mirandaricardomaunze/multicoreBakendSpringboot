CREATE TABLE IF NOT EXISTS ai_documents (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    document_number VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RASCUNHO',
    current_version INTEGER NOT NULL DEFAULT 1,
    author_username VARCHAR(120) NOT NULL,
    submitted_at TIMESTAMP WITHOUT TIME ZONE,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    reviewed_by VARCHAR(120),
    rejection_reason VARCHAR(500),
    archived_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(120),
    CONSTRAINT uk_ai_documents_company_number UNIQUE (company_id, document_number),
    CONSTRAINT ck_ai_documents_version CHECK (current_version >= 1)
);

CREATE TABLE IF NOT EXISTS ai_document_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    instructions TEXT NOT NULL,
    generated_content TEXT NOT NULL,
    pdf_data BYTEA NOT NULL,
    generated_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(120),
    CONSTRAINT uk_ai_document_version UNIQUE (document_id, version_number),
    CONSTRAINT ck_ai_document_version_number CHECK (version_number >= 1)
);

CREATE INDEX IF NOT EXISTS idx_ai_documents_company_status ON ai_documents(company_id, status);
CREATE INDEX IF NOT EXISTS idx_ai_documents_company_created ON ai_documents(company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_document_versions_document ON ai_document_versions(document_id, version_number DESC);
