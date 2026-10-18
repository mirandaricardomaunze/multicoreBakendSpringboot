-- V90: Índices compostos de alto desempenho para escalabilidade multiutilizador
-- Suporta filtros por tenant, data e estado; confirmar planos com carga representativa.

-- 1. Faturas e Linhas de Fatura
CREATE INDEX IF NOT EXISTS idx_invoices_company_date ON invoices(company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_company_status ON invoices(company_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_company_client_status ON invoices(company_id, client_id, status);
CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_product ON invoice_lines(invoice_id, product_id);

-- 2. Histórico de Movimentos de Stock e Saldos Físicos
CREATE INDEX IF NOT EXISTS idx_stock_movements_wh_date ON stock_movements(warehouse_id, movement_date DESC);
CREATE INDEX IF NOT EXISTS idx_stock_movements_prod_date ON stock_movements(product_id, movement_date DESC);
CREATE INDEX IF NOT EXISTS idx_stocks_wh_prod ON stocks(warehouse_id, product_id);

-- 3. Tesouraria e Fluxo de Caixa
CREATE INDEX IF NOT EXISTS idx_treasury_tx_account_date ON treasury_transactions(account_id, transaction_date DESC);

-- 4. Encomendas de Cliente e Compras a Fornecedor
CREATE INDEX IF NOT EXISTS idx_customer_orders_company_date ON customer_orders(company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_orders_company_status ON customer_orders(company_id, status);
CREATE INDEX IF NOT EXISTS idx_purchases_company_date ON purchases(company_id, purchase_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchases_company_status ON purchases(company_id, status);

-- 5. Contabilidade (Diário / Lançamentos)
CREATE INDEX IF NOT EXISTS idx_journal_entries_company_date ON journal_entries(company_id, entry_date DESC);

-- 6. Catálogo de Produtos e Negócios
CREATE INDEX IF NOT EXISTS idx_product_companies_company_product ON product_companies(company_id, product_id);
