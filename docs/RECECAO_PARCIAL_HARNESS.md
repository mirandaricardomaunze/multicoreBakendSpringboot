# Recepção Parcial de Encomenda — Harness

> Cenários de validação. Spec em [RECECAO_PARCIAL_SPEC.md](RECECAO_PARCIAL_SPEC.md).
> RP-01..RP-1x automáticos (`PurchaseOrderServiceTest`); RP-50+ manuais.

**Última actualização:** 2026-07-01

---

## Automáticos (`mvn test`)

| Id | Cenário | Espera-se |
|----|---------|-----------|
| RP-01 | `receivePartial` de 6 de 10 numa linha | Entrada de stock de **6**; `receivedQuantity=6`; estado `PARTIALLY_RECEIVED` |
| RP-02 | Segunda recepção dos 4 restantes | Entrada de **4**; `receivedQuantity=10`; estado `RECEIVED` + `receivedAt` |
| RP-03 | Receber mais do que o em falta (12 de 10) | `BusinessRuleException` (não excede o encomendado) |
| RP-04 | Receber quantidade ≤ 0 | `BusinessRuleException` |
| RP-05 | `receivePartial` sem permissão (EMPLOYEE) | `BusinessRuleException`; sem movimento de stock |
| RP-06 | `receivePartial` numa encomenda `RECEIVED` | `BusinessRuleException` (estado inválido) |
| RP-07 | `receivePartial` numa encomenda `CANCELLED` | `BusinessRuleException` |
| RP-08 | `receiveOrder` (tudo) a partir de `PARTIALLY_RECEIVED` | Recebe só o em falta; estado `RECEIVED`; sem dupla entrada |
| RP-09 | `receiveOrder` total a partir de `ORDERED` (regressão PO-03) | Entrada por linha pela qty total; `RECEIVED` |
| RP-10 | `cancelOrder` de `PARTIALLY_RECEIVED` com motivo | Estado `CANCELLED`; stock já recebido mantém-se (sem reversão) |
| RP-11 | Multi-linha: receber linha A toda e B parcial | Estado `PARTIALLY_RECEIVED` (B ainda em falta) |
| RP-12 | `lineId` inexistente na encomenda | `BusinessRuleException` |

---

## Manuais

| Id | Passo | Espera-se |
|----|-------|-----------|
| RP-50 | UI: «Encomendas a Fornecedor» → encomenda `ORDERED` → "Receber Parcial…" → receber parte | Lista mostra `PARTIALLY_RECEIVED`; stock subiu só pela parte recebida |
| RP-51 | Repetir até completar | Estado passa a `RECEIVED`; em falta = 0 em todas as linhas |
| RP-52 | Conferir no Stock/Lotes que entraram exactamente as quantidades recebidas | Quantidades e lotes/validade correctos |

---

## Critério de aceitação

- RP-01..RP-12 verdes em `mvn test` (inclui a regressão PO-03..PO-08 do ciclo já existente).
- RP-50..RP-52 executados uma vez com dados reais.
