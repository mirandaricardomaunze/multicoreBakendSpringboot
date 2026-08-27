-- Bloqueio de stock (contagem cega / visível só para admin).
-- Quando `stock_count_locked = true`, os utilizadores sem papel ADMIN deixam de ver as quantidades
-- de stock na aplicação (Níveis, Alertas, Lotes) — útil para inventário sem que o funcionário
-- "copie" o número do sistema. O ADMIN vê sempre tudo. Default preserva o comportamento actual.
alter table companies add column stock_count_locked boolean not null default false;
