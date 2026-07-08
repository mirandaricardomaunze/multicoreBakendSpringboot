# Harness — Alertas de Stock (esgotados e validades)

> Cenários para [STOCK_ALERTAS_SPEC.md](STOCK_ALERTAS_SPEC.md).
> AL-01 automático (`InventoryServiceTest`); AL-50..53 manuais (Stock a correr).

## Automático (`InventoryServiceTest`)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| AL-01 | `findOutOfStockProducts`: p1 saldo 5; p2 sem stock; p3 serviço; p4 saldo agregado 2 + (−3) = −1. | Devolve `{p2, p4}` (esgotados). p1 (com stock) e p3 (serviço) ficam de fora. |

## Manuais (StockPanel → aba "Alertas")

| ID    | Passos | Esperado |
|-------|--------|----------|
| AL-50 | Ter um produto com controlo de stock e saldo 0. Abrir Stock → **Alertas → Esgotados**. | O produto aparece na lista de esgotados; o resumo conta-o. |
| AL-51 | Dar entrada de stock desse produto. **Atualizar**. | Sai da lista de esgotados. |
| AL-52 | Ter um lote com validade no passado (com stock). **Alertas → Validade**. | Aparece como **Expirado** (texto vermelho). |
| AL-53 | Ter um lote a expirar em ≤ 30 dias. **Alertas → Validade**. | Aparece como **A expirar** (texto amarelo); >30 dias não aparece. |

## Verificação

- `mvn -o test -Dtest=InventoryServiceTest` → verde (inclui AL-01 e os anteriores).
