-- Via da encomenda declarada no documento (ver docs/ENCOMENDA_DUAS_VIAS_SPEC.md §5).
-- Antes disto, qual dos dois circuitos se aplicava era adivinhado a partir do estado.

alter table customer_orders add column if not exists kind varchar(20) not null default 'FORMAL_ORDER';

-- Backfill retroactivo conservador: nenhuma encomenda já emitida muda de comportamento.
--
-- Não classificamos pelo estado, que é ambíguo — CANCELLED existe nos dois circuitos. O marcador
-- preciso é a chave de idempotência: só o circuito de separação a grava (createAndReserve). Os
-- estados de separação entram apenas como rede de segurança.
update customer_orders set kind = 'PICKING_REQUEST'
 where idempotency_key is not null
    or status in ('AWAITING_SEPARATION', 'IN_SEPARATION', 'SEPARATED');
