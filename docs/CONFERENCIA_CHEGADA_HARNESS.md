# Conferência à chegada — harness

Cenários de verificação da [CONFERENCIA_CHEGADA_SPEC.md](CONFERENCIA_CHEGADA_SPEC.md).
**CC-01..CC-17** automáticos; **CC-50..CC-55** manuais.

---

## Automáticos

### `PurchaseOrderServiceTest` — a conferência no acto da recepção

| ID | Cenário | Esperado |
|---|---|---|
| CC-01 | Encomendadas 10: chegam 7 boas, 2 danificadas, 1 em falta | stock recebe **7** — nunca 9 nem 10 |
| CC-02 | O mesmo | **dois** registos (danificada 2, em falta 1); valor da danificada = 2 × 25 = **50** |
| CC-03 | Recepção normal, sem divergências | **nada** é gravado — quem recebia como antes não muda |
| CC-04 | 8 boas + 5 danificadas numa linha com 10 por receber | recusa ("excedem"); sem stock e sem registo |
| CC-05 | 7 boas + 2 danificadas + 1 em falta | linha fica com **8** recebidas (as boas + o fecho curto) |
| CC-06 | 7 boas + 2 danificadas | linha fica com **7** — a danificada não fecha a encomenda |

### `GoodsReceiptDiscrepancyServiceTest` — o relatório

| ID | Cenário | Esperado |
|---|---|---|
| CC-10 | 2 danificadas + 1 em falta do mesmo fornecedor | separa 50 (danificado) de 25 (falta); total 75 |
| CC-11 | Um fornecedor com 1.000 e outro com 10 | o de 1.000 aparece **primeiro** |
| CC-12 | Uma resolvida (50) e uma por resolver (100) | total 150, **por reclamar 100** |
| CC-13 | Período sem divergências | lista vazia |
| CC-14 | Período inválido (sem datas, ou fim antes do início) | recusa |
| CC-15 | Resolver sem explicação | recusa; continua por resolver |
| CC-16 | Resolver o que já está resolvido | recusa |
| CC-17 | Resolver com explicação | fica resolvida e guarda o texto |

**Execução:** `mvn -o test -Dtest=PurchaseOrderServiceTest,GoodsReceiptDiscrepancyServiceTest`
→ 36 testes, 0 falhas. Suite completa: **558 testes, 0 falhas** (2026-08-16).

> CC-01/02 apanharam um erro nos próprios testes antes de apanharem qualquer coisa no código: a
> encomenda de teste não dava `id` às linhas, e a conferência identifica as linhas por id. O
> auxiliar passou a criar linhas como elas existem depois de gravadas.

---

## Manuais (backend de pé)

| ID | Passos | Esperado |
|---|---|---|
| CC-50 | Encomendar 10, receber 7 boas + 2 danificadas + 1 em falta | stock sobe 7 |
| CC-51 | `GET /api/purchases/discrepancies/open` | duas ocorrências, com valores |
| CC-52 | `GET /api/purchases/discrepancies/by-supplier` | fornecedor com o total e o por reclamar |
| CC-53 | Resolver uma com "nota de crédito NC-12" | sai do "por reclamar", fica no total |
| CC-54 | Repetir a recepção da mesma encomenda já fechada | recusa (linha sem quantidade por receber) |
| CC-55 | Recepção antiga (só quantidade, sem os campos novos) | funciona como antes, sem divergências |

**Estado:** por executar.
