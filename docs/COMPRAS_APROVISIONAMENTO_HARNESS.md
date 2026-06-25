# Harness — Compras & Aprovisionamento

> Cenários verificáveis da [spec](COMPRAS_APROVISIONAMENTO_SPEC.md). Automatizados em
> `PurchaseServiceTest` / `PurchaseOrderServiceTest`; manuais validam a UI desktop.

**Última actualização:** 2026-06-25

---

## Fornecedores (PurchaseServiceTest)

| ID    | Cenário                                                  | Esperado                                                     |
|-------|---------------------------------------------------------|-------------------------------------------------------------|
| FN-01 | `updateSupplier` altera nome/telefone/contacto          | Devolve DTO actualizado; persistido.                        |
| FN-02 | `setSupplierActive(false)` sem MANAGER/ADMIN            | `BusinessRuleException` (permissão).                         |
| FN-03 | `setSupplierActive(false)` com MANAGER                   | Fornecedor fica inactivo; auditado.                         |
| FN-04 | `searchSuppliers("ace")` (substring, case-insensitive)  | Só fornecedores cujo nome/NUIT contém o termo.              |
| FN-05 | Guarda de empresa em update/search                       | Empresa ≠ activa → `BusinessRuleException`.                  |

## Encomenda de Fornecedor (PurchaseOrderServiceTest)

| ID    | Cenário                                                  | Esperado                                                     |
|-------|---------------------------------------------------------|-------------------------------------------------------------|
| PO-01 | `createOrder` válida                                     | Estado `ORDERED`, nº série `EC-F`, **sem** movimento de stock.|
| PO-02 | `createOrder` com fornecedor inactivo                    | `BusinessRuleException`.                                     |
| PO-03 | `receiveOrder` em encomenda `ORDERED` (MANAGER)         | Estado `RECEIVED`; **uma entrada de stock por linha** (`registerMovement`). |
| PO-04 | `receiveOrder` sem MANAGER/ADMIN                         | `BusinessRuleException`; stock intacto.                      |
| PO-05 | `receiveOrder` em encomenda já `RECEIVED`/`CANCELLED`   | `BusinessRuleException` (sem dupla entrada).                 |
| PO-06 | `cancelOrder` em `ORDERED` (MANAGER) com motivo         | Estado `CANCELLED`; auditado; stock intacto.                |
| PO-07 | `cancelOrder` sem motivo / em encomenda já recebida     | `BusinessRuleException`.                                     |
| PO-08 | `searchOrders` por nº/fornecedor                         | Filtra por substring case-insensitive.                      |

## Manuais (UI desktop)

| ID    | Passos                                                              | Esperado                                            |
|-------|--------------------------------------------------------------------|-----------------------------------------------------|
| UI-01 | Compras › Fornecedores → Editar / Desactivar / Pesquisar           | Operações reflectem-se na tabela.                   |
| UI-02 | Compras › Encomendas a Fornecedor → criar, **Receber**             | Stock do armazém sobe pelas quantidades recebidas.  |
| UI-03 | Compras › Encomendas → **Cancelar** (com motivo)                   | Estado CANCELLED; stock inalterado.                 |
| UI-04 | Stock/Compras › Categorias → criar/editar/activar                  | Lista actualiza; categorias activas surgem no produto. |

## Verificação

```
mvn clean test    # inclui PurchaseServiceTest + PurchaseOrderServiceTest
```
