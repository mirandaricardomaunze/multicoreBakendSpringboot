-- Reposição interna: a encomenda da loja vira transferência entre armazéns.
-- Ver docs/REPOSICAO_INTERNA_SPEC.md §7.

-- A via nova não cabia na coluna: 'INTERNAL_REPLENISHMENT' tem 22 caracteres e a V43 criou
-- varchar(20), quando a via mais longa tinha 15. Confirmado ao vivo: gravar a primeira reposição
-- rebentava com erro de base de dados ("Value too long for column KIND"), e não com uma regra de
-- negócio. Folga até 40 para a próxima via não repetir isto.
alter table customer_orders alter column kind set data type varchar(40);

-- Destino do pedido (a loja que pediu). A origem continua a ser warehouse_id.
alter table customer_orders add column if not exists destination_warehouse_id bigint;
alter table customer_orders add column if not exists stock_transfer_id bigint;
alter table customer_orders add column if not exists transfer_number varchar(40);

-- Ligação no sentido inverso: da transferência para a encomenda que a originou.
alter table stock_transfers add column if not exists order_id bigint;
alter table stock_transfers add column if not exists order_number varchar(40);

-- Retroactivo conservador: todas as colunas nascem nulas e nenhum documento existente muda de via,
-- de estado ou de comportamento. A via só é INTERNAL_REPLENISHMENT em documentos criados como tal —
-- por isso não há backfill nenhum a fazer aqui.

create index if not exists idx_customer_orders_transfer on customer_orders(stock_transfer_id);
create index if not exists idx_stock_transfers_order on stock_transfers(order_id);
