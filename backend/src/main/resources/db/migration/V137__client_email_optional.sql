-- Email do cliente passa a opcional (docs/IMPORTACAO_DADOS_SPEC.md §4.1, IMP-CL-03).
--
-- Porquê: quem importa clientes de outro ERP (outro programa de gestão ou uma folha de cálculo) raramente tem o email de todos,
-- e o registo manual também deixa de o exigir. O NUIT continua a ser a chave obrigatória.
ALTER TABLE clients ALTER COLUMN email DROP NOT NULL;
