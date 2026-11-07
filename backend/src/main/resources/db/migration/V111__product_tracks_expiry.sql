-- Produto perecível: compras e entradas manuais passam a exigir validade.
-- Produtos não perecíveis (loiça, mobília) continuam a entrar sem validade.
alter table products add column if not exists tracks_expiry boolean default false not null;

-- Produtos que já receberam um lote com validade real ficam marcados como perecíveis.
-- 9999-12-31 é a data interna de "sem validade" (ProductBatchMutationService.LEGACY_EXPIRATION).
update products set tracks_expiry = true
where exists (
    select 1 from product_batches b
    where b.product_id = products.id
      and b.expiration_date < date '9999-01-01'
);
