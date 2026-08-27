-- Encomenda profissional: origem (cotação) e condições acordadas gravadas no documento.
-- Ver docs/ENCOMENDA_PROFISSIONAL_SPEC.md.
--
-- A ligação cotação↔encomenda era de sentido único: a cotação guardava a encomenda gerada, a
-- encomenda não sabia de onde vinha. Quem abre a encomenda (armazém, aprovador, cobrança) não
-- tinha como chegar à proposta que a justificou.
--
-- TODAS as colunas são nullable, de propósito: encomendas criadas à mão não têm acordo prévio, e
-- as anteriores a esta versão ficam com tudo a nulo e comportam-se exactamente como sempre.
-- Nenhuma linha existente é alterada.

alter table customer_orders add column if not exists quotation_id           bigint;
alter table customer_orders add column if not exists quotation_number       varchar(255);
alter table customer_orders add column if not exists payment_terms          varchar(200);
alter table customer_orders add column if not exists delivery_terms         varchar(200);

-- Data prometida ao cliente, calculada na CONFIRMAÇÃO (a conversão) e gravada — molde do
-- invoices.due_date (V35). Se a aprovação interna demorar, a data prometida não se mexe.
alter table customer_orders add column if not exists expected_delivery_date date;

-- Quem pergunta "o que é que prometemos entregar e está a passar do prazo?" filtra por aqui.
create index if not exists idx_customer_orders_expected_delivery
    on customer_orders (company_id, expected_delivery_date);
