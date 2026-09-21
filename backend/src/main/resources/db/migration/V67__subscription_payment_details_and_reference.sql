-- Adiciona colunas para detalhes de pagamento e referência opcional
alter table subscription_payments add column reference varchar(100);
alter table subscription_payments add column payment_details varchar(500);
