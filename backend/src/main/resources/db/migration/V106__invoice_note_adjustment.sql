-- As notas aprovadas passam a corrigir o saldo da fatura, e a nota de crédito guarda o desconto da linha.
alter table invoices add column if not exists note_adjustment numeric(14,2) default 0 not null;
alter table credit_note_lines add column if not exists discount_percentage numeric(5,2) default 0 not null;

update invoices i
set note_adjustment = coalesce((select sum(d.total_amount) from debit_notes d
                                where d.invoice_id = i.id and d.status = 'APPROVED'), 0)
                    - coalesce((select sum(c.total_amount) from credit_notes c
                                where c.invoice_id = i.id and c.status = 'APPROVED'), 0);
