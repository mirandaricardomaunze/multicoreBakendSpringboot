-- Documento "Saldo Inicial" (série SI) das dívidas de clientes importadas (docs/IMPORTACAO_DADOS_SPEC.md §5.2,
-- IMP-SI-01).
--
-- Porquê na tabela das facturas: o SI cobra-se e liquida-se como uma factura (recibos, extracto, antiguidade,
-- crédito). document_kind distingue-o das vendas; original_number guarda o número do sistema antigo e, com o
-- cliente, é a chave que impede importar a mesma dívida duas vezes; document_date é a data da factura antiga.
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS document_kind VARCHAR(20) DEFAULT 'SALE' NOT NULL;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS original_number VARCHAR(60);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS document_date DATE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS description VARCHAR(300);
ALTER TABLE invoices ADD CONSTRAINT uk_invoices_company_client_original
    UNIQUE (company_id, client_id, original_number);
