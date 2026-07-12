-- Numeração de documentos POR EMPRESA (gapless por contribuinte / NUIT).
--
-- Até aqui a numeração era GLOBAL (partilhada entre todas as empresas), o que criava saltos na
-- sequência de cada empresa (a Empresa A ficava com FT-2026/1 e /3 se a B emitisse a /2). Cada empresa
-- é um contribuinte distinto e precisa da SUA própria sequência contínua.
--
-- Cutover seguro: cada empresa é semeada no MÁXIMO GLOBAL actual de cada série/ano. Assim nenhum número
-- já emitido é repetido, e cada empresa continua a numeração para cima a partir daí (sem salto para trás).
-- Empresas ainda sem documentos começam a partir desse máximo — não é um salto, apenas começa alto.

alter table document_sequences add column if not exists company_id bigint;

-- Largar a unique GLOBAL (série, ano) ANTES de semear as cópias por empresa: as cópias têm a mesma
-- série/ano (só muda a empresa), pelo que violariam a restrição antiga (que não inclui a empresa).
alter table document_sequences drop constraint if exists uk_document_sequence_series_year;

insert into document_sequences (company_id, series, doc_year, last_number)
select c.id, g.series, g.doc_year, g.last_number
from companies c
cross join (
    select series, doc_year, last_number
    from document_sequences
    where company_id is null
) g;

delete from document_sequences where company_id is null;

alter table document_sequences
    add constraint uk_document_sequence_company_series_year unique (company_id, series, doc_year);
alter table document_sequences alter column company_id set not null;
