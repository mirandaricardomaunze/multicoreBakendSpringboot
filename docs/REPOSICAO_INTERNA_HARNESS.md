# Harness — Reposição interna (encomenda da loja ⇄ transferência entre armazéns)

**Spec:** [REPOSICAO_INTERNA_SPEC.md](REPOSICAO_INTERNA_SPEC.md)

Cada caso diz **o que se perde se a regra não existir**. Os marcados 🔴 devem ser confirmados a
falhar contra o código anterior — é o que separa um teste que prova de um teste que acompanha.

Os casos 💰 protegem stock ou dinheiro. Se algum deles ficar vermelho, não se entrega.

---

## 1. A terceira via (RI-01..08)

| ID | Cenário | Esperado |
|----|---------|----------|
| RI-01 | `INTERNAL_REPLENISHMENT.requiresApproval()` | `false` — quem aprova é a transferência (R3) |
| RI-02 | `INTERNAL_REPLENISHMENT.isThermal()` | `true` — é trabalho de armazém |
| RI-03 | `INTERNAL_REPLENISHMENT.usesSeparationFlow()` | `true` |
| RI-04 | `INTERNAL_REPLENISHMENT.isBillable()` | `false` (R1) |
| RI-05 | `FORMAL_ORDER.isBillable()` / `PICKING_REQUEST.isBillable()` | `true` — as vias de venda não mudam |
| RI-06 | Rótulo PT-MZ | "Reposição interna"; sem `_` nem código interno |
| RI-07 | Criar reposição sem armazém de destino | recusa: falta dizer para que loja é (R5) |
| RI-08 | Criar reposição com destino igual à origem | recusa (R5) |

## 2. A trava do stock (RI-09..14)

| ID | Cenário | Esperado |
|----|---------|----------|
| RI-09 🔴💰 | `billOrder` sobre uma reposição interna | **recusa**; a mensagem diz que não há cliente a quem facturar (R1) |
| RI-10 🔴💰 | Guia de remessa ao cliente a partir de uma reposição | **recusa** (R2) |
| RI-11 💰 | Criar a encomenda de reposição | **nenhum** movimento de stock (R4) |
| RI-12 💰 | Converter em transferência | **nenhum** movimento de stock — só na aprovação (R4) |
| RI-13 💰 | Aprovar a transferência | stock sai da origem e entra no destino, **uma só vez** |
| RI-14 | Mensagem de recusa de RI-09 | sem `INTERNAL_REPLENISHMENT` no texto |

## 3. Encomenda → Transferência (RI-15..22)

| ID | Cenário | Esperado |
|----|---------|----------|
| RI-15 | Converter uma reposição `PENDING` | transferência criada em `PENDING_APPROVAL` (R6) |
| RI-16 | Converter uma reposição `SEPARATED` | idem — é o fim do circuito do armazém (R6) |
| RI-17 | Converter uma reposição `AWAITING_SEPARATION` | recusa; a mensagem diz o passo que falta |
| RI-18 🔴 | Converter uma encomenda de **venda** | recusa: só a reposição interna vira transferência |
| RI-19 | Linhas da transferência | artigo e quantidade iguais aos da encomenda, sem preços (R7) |
| RI-20 | Estado da encomenda após converter | `TRANSFER_PENDING` (R8) |
| RI-21 | Converter duas vezes a mesma encomenda | recusa (R8) |
| RI-22 | Ligação gravada | `Order.stockTransferId` e `StockTransfer.orderId` apontam um para o outro |

## 4. O que a transferência decide, a encomenda segue (RI-23..26)

| ID | Cenário | Esperado |
|----|---------|----------|
| RI-23 | Transferência aprovada | encomenda → `TRANSFERRED` (R9) |
| RI-24 🔴 | Transferência rejeitada | encomenda volta a `PENDING`, convertível de novo (R9) |
| RI-25 | Transferência cancelada | encomenda volta a `PENDING` (R9) |
| RI-26 | Transferência sem encomenda de origem | aprovar/rejeitar não rebenta |

## 5. Transferência → Encomenda (RI-27..30)

| ID | Cenário | Esperado |
|----|---------|----------|
| RI-27 | Registar a encomenda de uma transferência aprovada | encomenda `INTERNAL_REPLENISHMENT` já `TRANSFERRED` (R10) |
| RI-28 💰 | O registo retroactivo | **não** move stock — a mercadoria já mudou de armazém (R10) |
| RI-29 | Registar duas vezes na mesma transferência | recusa (R11) |
| RI-30 | Registar a partir de transferência por aprovar | recusa (R12) |

---

## 6. Manuais, com o sistema de pé (RI-50..60)

| ID | Passo | Esperado |
|----|-------|----------|
| RI-50 | Criar encomenda escolhendo **Reposição interna** | pede o armazém de destino; não aparece em aprovações |
| RI-51 | Imprimir o pedido | talão com o cabeçalho da empresa, origem e **destino** |
| RI-52 | Tentar facturá-la | recusa a explicar que é reposição interna |
| RI-53 | Separar e converter em transferência | transferência criada, encomenda travada |
| RI-54 | Ver a transferência em Stock | mostra o nº da encomenda de origem |
| RI-55 | Aprovar a transferência | stock sai da origem, entra no destino; encomenda "Transferida" |
| RI-56 💰 | Conferir os níveis de stock nos dois armazéns | a mercadoria saiu de um e entrou no outro, **uma só vez** |
| RI-57 | Rejeitar uma transferência convertida | a encomenda volta a poder ser convertida |
| RI-58 | Fazer transferência à mão e registar a encomenda | encomenda criada já transferida |
| RI-59 💰 | Stock depois de RI-58 | **inalterado** — o registo não move nada |
| RI-60 | Encomendas criadas antes desta versão | continuam a facturar-se e a comportar-se como sempre |
