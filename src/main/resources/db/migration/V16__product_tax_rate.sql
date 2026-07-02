-- IVA dinâmico por produto: cada produto pode apontar para uma taxa de IVA configurável.
-- Quando nulo, aplica-se a taxa-padrão do sistema (16%).
alter table products
    add column if not exists tax_rate_id bigint;

alter table products
    add constraint fk_product_tax_rate
    foreign key (tax_rate_id) references tax_rates (id);
