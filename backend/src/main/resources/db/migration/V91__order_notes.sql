-- V91: Adiciona suporte a notas e observações em pedidos de cliente (encomendas)
alter table customer_orders add column if not exists notes varchar(1000);
