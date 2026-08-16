# Vencimento e antiguidade de saldos — harness

Cenários de verificação da [VENCIMENTO_ANTIGUIDADE_SPEC.md](VENCIMENTO_ANTIGUIDADE_SPEC.md).
**VA-01..VA-25** automáticos; **VA-50..VA-57** manuais (com o backend de pé).

---

## Automáticos

### `InvoiceAgingTest` — regra de domínio (JUnit puro)

| ID | Cenário | Esperado |
|---|---|---|
| VA-01 | Cliente com prazo de 30 dias, fatura emitida hoje | vencimento = hoje + 30 |
| VA-02 | Cliente sem prazo (0) | vencimento = hoje (pronto pagamento) |
| VA-03 | Pedido com vencimento explícito a 7 dias, cliente a 30 | ganha o explícito (7) |
| VA-04 | Vencimento anterior à emissão | recusa com "anterior à data de emissão" |
| VA-05 | Fatura a vencer daqui a 5 dias | 0 dias de atraso, `CORRENTE` |
| VA-06 | Fatura que vence **hoje** | 0 dias de atraso — o dia do vencimento não é atraso |
| VA-07 | Cortes da escala: 1, 30, 31, 61, 91 dias | `ATE_30`, `ATE_30`, `DE_31_A_60`, `DE_61_A_90`, `MAIS_DE_90` |
| VA-08 | Fatura paga e fatura anulada, ambas vencidas há 120 dias | 0 dias de atraso — não são dívida |
| VA-09 | 400 pagos de 1000, vencida há 65 dias | saldo 600, atraso 65, `DE_61_A_90` |
| VA-10 | Documento legado sem `due_date` | usa a data de emissão |

### `ComercialServiceTest` — emissão

| ID | Cenário | Esperado |
|---|---|---|
| VA-11 | `createInvoice` sem vencimento no pedido, cliente a 30 dias | `dueDate` = hoje+30, `daysOverdue` = 0 |
| VA-12 | `createInvoice` com vencimento no pedido | respeita a data escolhida |
| VA-13 | Contas correntes com uma paga, uma a 2 dias e uma a 100 dias | só as 2 cobráveis, a mais atrasada primeiro |

### `ReceivablesServiceTest` — mapa de antiguidade

| ID | Cenário | Esperado |
|---|---|---|
| VA-20 | Cinco faturas, uma por escalão (100/200/300/400/500) | cada escalão com o seu valor; total 1500; em atraso 1400 |
| VA-21 | Empresa sem dívidas | os 5 escalões presentes a zero; sem clientes |
| VA-22 | 400 pagos de 1000, vencida há 5 dias | conta **600**, não 1000 |
| VA-23 | Paga + anulada + rascunho + aprovada (50) | total = 50 |
| VA-24 | Dois clientes, um com 800 vencidos + 200 correntes | ordenado pelo maior; `maxDaysOverdue` = 120; `oldestDueDate` correcto |
| VA-25 | Mesma fatura a três datas de referência | envelhece de `CORRENTE` → `ATE_30` → `DE_31_A_60` |

**Execução:** `mvn -o test -Dtest=InvoiceAgingTest,ReceivablesServiceTest,ComercialServiceTest,POSServiceTest`
→ 72 testes, 0 falhas (2026-08-14).

---

## Manuais (backend de pé)

| ID | Passos | Esperado |
|---|---|---|
| VA-50 | Cadastrar cliente com prazo 30 dias | tabela de Clientes mostra "30" na coluna Prazo |
| VA-51 | Cliente sem prazo | coluna mostra "Pronto pagamento" |
| VA-52 | Emitir fatura a esse cliente | Contas Correntes mostram vencimento = emissão + 30, "—" em Dias em Atraso, `Corrente (por vencer)` |
| VA-53 | `GET /api/comercial/receivables/aging` | 5 escalões, totais coerentes com a tabela |
| VA-54 | `GET .../aging?reference=<data futura>` | a mesma fatura muda de escalão |
| VA-55 | Filtrar Contas Correntes por "Mais de 90 dias" | só as faturas desse escalão |
| VA-56 | Receber parcialmente uma fatura vencida | mantém-se na lista, com o **saldo** e o mesmo atraso |
| VA-57 | Liquidar a fatura por completo | sai das Contas Correntes e do mapa de antiguidade |

**Estado:** por executar (exigem backend de pé + desktop).
