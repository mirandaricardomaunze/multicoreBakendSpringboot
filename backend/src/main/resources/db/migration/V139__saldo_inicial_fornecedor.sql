-- Documento "Saldo Inicial Fornecedor" (série SIF) das dívidas a fornecedores importadas
-- (docs/IMPORTACAO_DADOS_SPEC.md §5.3, IMP-SIF-01).
--
-- Porquê na tabela das compras: o SIF aparece nas contas a pagar e paga-se com a mesma acção de pagamento a
-- fornecedor. document_kind distingue-o das compras; original_number guarda o número do sistema antigo e, com o
-- fornecedor, é a chave que impede importar a mesma dívida duas vezes; due_date é o vencimento da factura antiga.
-- O SIF não movimenta stock, por isso não tem armazém.
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS document_kind VARCHAR(20) DEFAULT 'PURCHASE' NOT NULL;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS original_number VARCHAR(60);
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS due_date DATE;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS description VARCHAR(300);
ALTER TABLE purchases ALTER COLUMN warehouse_id DROP NOT NULL;
ALTER TABLE purchases ADD CONSTRAINT uk_purchases_company_supplier_original
    UNIQUE (company_id, supplier_id, original_number);
