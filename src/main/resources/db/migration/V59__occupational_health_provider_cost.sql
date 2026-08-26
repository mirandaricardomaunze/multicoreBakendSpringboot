-- Saúde ocupacional: o prestador deixa de ser texto livre e o exame passa a ter custo.
--
-- Duas lacunas distintas fechadas na mesma tabela porque são a mesma linha do papel:
--
-- 1. `clinic varchar(160)` não respondia a "quanto gastámos na Clínica X este ano" nem a
--    "que exames estão por pagar" — duas perguntas que a empresa faz. O prestador passa a
--    apontar para o registo de fornecedores, que já tem NUIT, contacto e é onde a factura
--    da clínica vive. O texto livre fica para as clínicas ainda não cadastradas e para os
--    registos anteriores a esta migração, que não têm fornecedor nenhum a que apontar.
--
-- 2. O custo do exame de aptidão é encargo do empregador (Lei n.º 13/2023 e regulamentação
--    sectorial; ver docs/CONFORMIDADE_LEGAL_MZ_SPEC.md §3). Não existia em lado nenhum:
--    nem no custo do trabalhador, nem na tesouraria, nem em relatório algum.
alter table occupational_health_exams add column if not exists provider_id     bigint references suppliers (id);
alter table occupational_health_exams add column if not exists cost            numeric(19, 2);
alter table occupational_health_exams add column if not exists invoice_number  varchar(60);
alter table occupational_health_exams add column if not exists paid_at         date;

create index if not exists idx_occupational_health_provider
    on occupational_health_exams (company_id, provider_id);

-- Os por pagar: a pergunta é sempre "o que é que esta empresa deve às clínicas".
create index if not exists idx_occupational_health_unpaid
    on occupational_health_exams (company_id, paid_at);
