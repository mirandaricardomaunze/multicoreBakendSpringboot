-- RH: o colaborador passa a saber que conta de utilizador é a dele.
-- Ver docs/RH_COMPLETO_SPEC.md §3 B7.
--
-- Sem esta ligação, "o próprio" não era identificável: submeter férias ou uma despesa recebia o
-- employeeId no corpo do pedido, pelo que qualquer utilizador autenticado da empresa o fazia em
-- nome de um colega. A alternativa — exigir MANAGER/ADMIN em tudo — fechava o furo mas matava o
-- self-service, que é o ponto de um módulo de RH para quem trabalha na loja.
--
-- Coluna NULLABLE de propósito: colaboradores sem conta (a maioria numa loja) continuam a existir
-- exactamente como antes, e nenhuma linha existente é alterada. Quem não tem conta não faz
-- self-service; quem tem, só age sobre si próprio.

alter table employees add column if not exists app_user_id bigint;

-- Uma conta de utilizador não pode ser dois colaboradores DA MESMA EMPRESA — senão "o próprio"
-- volta a ser ambíguo, que é exactamente o que esta migração existe para resolver.
-- Índice parcial: várias linhas com app_user_id nulo continuam permitidas.
create unique index if not exists uk_employees_company_app_user
    on employees (company_id, app_user_id)
    where app_user_id is not null;

-- Resolver "o colaborador deste utilizador" é feito a cada pedido de férias/despesa.
create index if not exists idx_employees_app_user
    on employees (app_user_id);
