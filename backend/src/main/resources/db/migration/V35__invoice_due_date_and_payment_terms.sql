-- Vencimento das faturas e prazo de pagamento do cliente.
--
-- Porquê: até aqui o sistema sabia o que estava POR RECEBER (saldo em dívida, V-anterior),
-- mas não o que estava EM ATRASO — não havia data-limite com que comparar. Sem isto não há
-- antiguidade de saldos nem cobrança priorizada. Ver docs/VENCIMENTO_ANTIGUIDADE_SPEC.md.
--
-- payment_terms_days: prazo acordado, em dias, a contar da emissão. Zero = pronto pagamento,
-- que é exactamente o comportamento anterior — nenhum cliente existente muda de regra.
alter table clients add column if not exists payment_terms_days integer not null default 0;

-- due_date: data-limite de pagamento gravada no documento, não recalculada a partir do cliente.
-- O prazo do cliente pode mudar amanhã; o vencimento de uma fatura já emitida não pode.
alter table invoices add column if not exists due_date date;

-- Retroactivo: os documentos anteriores ficam com vencimento igual à data de emissão (pronto
-- pagamento). É a leitura conservadora — nenhuma dívida antiga aparece como "ainda no prazo".
update invoices set due_date = cast(created_at as date) where due_date is null;
