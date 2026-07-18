# Harness — POS: catálogo sem produtos esgotados

> Cenários para [POS_CATALOGO_SEM_ESGOTADOS_SPEC.md](POS_CATALOGO_SEM_ESGOTADOS_SPEC.md).
> PE-01 automático (`InventoryServiceTest`); PE-50..52 manuais (POS a correr).

## Automático (`InventoryServiceTest`)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| PE-01 | `getInStockProductIdsForSale` com armazém de venda (produto 1 qty 5, produto 2 qty 0, produto 3 qty −1) + um depósito que não vende. | Devolve só `{1}`; o depósito nem é consultado. |

## Manuais (POS)

| ID    | Passos | Esperado |
|-------|--------|----------|
| PE-50 | Ter um produto com stock 0 num armazém de venda. Abrir POS. | O produto **não** aparece no catálogo. |
| PE-51 | Dar entrada de stock desse produto (Compras/Ajuste). Reabrir/atualizar POS. | O produto **volta** a aparecer. |
| PE-52 | Ter um serviço (`stockTracked = false`, sem stock). Abrir POS. | O serviço **aparece** sempre (não depende de stock). |

## Verificação

- `mvn -o test -Dtest=InventoryServiceTest` → verde (inclui PE-01).
- Confirmar que a **facturação** (ComercialPanel) continua a listar todos os produtos — só o POS filtra.
