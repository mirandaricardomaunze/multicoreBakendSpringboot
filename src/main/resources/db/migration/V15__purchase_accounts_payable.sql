-- Contas a pagar: valor já pago por fatura de compra (resto = em dívida ao fornecedor).
alter table purchases add column if not exists amount_paid numeric(12, 2) not null default 0;

-- Compras históricas foram pagas integralmente no acto; alinhar o pago com o total.
update purchases set amount_paid = total_amount where amount_paid = 0;
