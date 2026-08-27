-- CRM / Assistência: o pedido passa a ter ciclo de vida próprio e a folha de obra passa a poder
-- ser anulada.
--
-- Até aqui `crm_tickets.status` era texto livre com dois valores escritos à mão no serviço
-- ("OPEN" e "RESOLVED") e o único caminho para fechar um pedido era registar folha de obra —
-- um pedido resolvido ao telefone ficava aberto para sempre. Não havia prioridade nem técnico
-- responsável, ao contrário do módulo `support` (assistência à plataforma), que já os tinha.
--
-- As constantes do enum TicketStatus foram nomeadas para coincidir com as strings antigas, por
-- isso NÃO é preciso converter dados: as linhas existentes já contêm 'OPEN'/'RESOLVED'.

alter table crm_tickets add column if not exists priority            varchar(20);
alter table crm_tickets add column if not exists assigned_technician varchar(255);
alter table crm_tickets add column if not exists resolved_at         timestamp;
alter table crm_tickets add column if not exists closing_note        varchar(500);

-- Prioridade é NOT NULL no modelo: os pedidos antigos entram como NORMAL, que é o default de quem
-- abre um pedido sem pensar no assunto. Preencher antes de apertar a constraint.
update crm_tickets set priority = 'NORMAL' where priority is null;
alter table crm_tickets alter column priority set not null;

-- Data de resolução dos pedidos já fechados: não se inventa o momento exacto, usa-se a última
-- actualização conhecida da linha. É a melhor aproximação disponível e não fica a nulo num campo
-- que os relatórios vão ler.
update crm_tickets set resolved_at = coalesce(updated_at, created_at)
 where status = 'RESOLVED' and resolved_at is null;

-- Folha de obra anulável (por faturar apenas — a regra vive no CRMService) e com a tarifa horária
-- gravada. A tarifa era uma constante no código: as folhas antigas não sabem a que preço foram
-- calculadas, por isso herdam o valor que essa constante teve desde sempre (45.00).
alter table crm_work_sheets add column if not exists voided      boolean;
alter table crm_work_sheets add column if not exists void_reason varchar(500);
alter table crm_work_sheets add column if not exists hourly_rate numeric(19, 2);

update crm_work_sheets set voided = false where voided is null;
alter table crm_work_sheets alter column voided set not null;

update crm_work_sheets set hourly_rate = 45.00 where hourly_rate is null;
alter table crm_work_sheets alter column hourly_rate set not null;

-- Quem abre a assistência filtra por pedidos em aberto e por técnico responsável.
create index if not exists idx_crm_tickets_status
    on crm_tickets (company_id, status);
