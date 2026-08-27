-- Unicidade dos NÚMEROS de documento POR EMPRESA (não global).
--
-- A numeração já é gerada por empresa (ver DocumentNumberService + document_sequences com a chave
-- (company_id, series, doc_year), migração V30). Mas a coluna do número em cada tabela de documento
-- tinha uma UNIQUE GLOBAL — só a coluna do número. Consequência: duas empresas distintas (dois NUITs)
-- que cheguem ao mesmo número (ex.: ambas "FT-2026/5") colidiam na chave única e a 2.ª falhava com
-- erro 500, MESMO sem concorrência. Cada empresa é um contribuinte com a sua própria série; o número
-- só tem de ser único DENTRO da empresa.
--
-- Correcção: trocar UNIQUE(numero) por UNIQUE(company_id, numero) nas 8 tabelas de documento que têm
-- company_id (alinhado com o que employees/product_batches/clients já fazem). Seguro sobre dados
-- existentes: a UNIQUE global garantia que não há números repetidos, logo também não há repetidos
-- dentro de nenhuma empresa. NOTA: payslips fica de fora — não tem company_id (follow-up).

-- invoices
alter table invoices drop constraint if exists invoices_invoice_number_key;
alter table invoices drop constraint if exists uk_invoices_company_number;
alter table invoices add constraint uk_invoices_company_number unique (company_id, invoice_number);

-- credit_notes
alter table credit_notes drop constraint if exists credit_notes_note_number_key;
alter table credit_notes drop constraint if exists uk_credit_notes_company_number;
alter table credit_notes add constraint uk_credit_notes_company_number unique (company_id, note_number);

-- debit_notes
alter table debit_notes drop constraint if exists debit_notes_note_number_key;
alter table debit_notes drop constraint if exists uk_debit_notes_company_number;
alter table debit_notes add constraint uk_debit_notes_company_number unique (company_id, note_number);

-- customer_orders
alter table customer_orders drop constraint if exists customer_orders_order_number_key;
alter table customer_orders drop constraint if exists uk_customer_orders_company_number;
alter table customer_orders add constraint uk_customer_orders_company_number unique (company_id, order_number);

-- receipts
alter table receipts drop constraint if exists receipts_receipt_number_key;
alter table receipts drop constraint if exists uk_receipts_company_number;
alter table receipts add constraint uk_receipts_company_number unique (company_id, receipt_number);

-- purchases
alter table purchases drop constraint if exists purchases_purchase_number_key;
alter table purchases drop constraint if exists uk_purchases_company_number;
alter table purchases add constraint uk_purchases_company_number unique (company_id, purchase_number);

-- purchase_orders
alter table purchase_orders drop constraint if exists purchase_orders_order_number_key;
alter table purchase_orders drop constraint if exists uk_purchase_orders_company_number;
alter table purchase_orders add constraint uk_purchase_orders_company_number unique (company_id, order_number);

-- stock_transfers
alter table stock_transfers drop constraint if exists stock_transfers_transfer_number_key;
alter table stock_transfers drop constraint if exists uk_stock_transfers_company_number;
alter table stock_transfers add constraint uk_stock_transfers_company_number unique (company_id, transfer_number);
