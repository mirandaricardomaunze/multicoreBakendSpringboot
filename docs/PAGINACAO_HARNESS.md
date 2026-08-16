# Paginação e leituras limitadas — harness

Cenários de verificação da [PAGINACAO_SPEC.md](PAGINACAO_SPEC.md).
**PG-01..PG-11** automáticos; **PG-50..PG-56** manuais.

---

## Automáticos

### `PageQueryTest` — contrato de paginação

| ID | Cenário | Esperado |
|---|---|---|
| PG-01 | Sem parâmetros | página 0, tamanho 50 |
| PG-02 | Página −5 | página 0 |
| PG-03 | `size=1000000` | encaixado em 200 — o cliente não manda no servidor |
| PG-04 | `size=0` / `size=-10` | 50 (default) |
| PG-05 | `page=3&size=25` | passa intacto |
| PG-06 | Conversão de `Page` do Spring Data | itens mapeados, metadados correctos |
| PG-07 | Primeira / meio / última página | `hasNext`/`hasPrevious` certos |
| PG-08 | Página vazia | sem seguinte nem anterior; total 0 |

### `ReportServiceTest` — as perguntas vão à base de dados

| ID | Cenário | Esperado |
|---|---|---|
| PG-10 | Dashboard | usa consulta por intervalo + por estados; **nunca** chama `findByCompanyId` |
| PG-11 | Relatório diário de 10/08 | pede só o intervalo desse dia |

> Ao migrar, **7 testes de regra falharam** (RP-10..14, MC-01..03) porque fixavam
> `findByCompanyId`. Foram reescritos com um `stubInvoices(...)` que simula a **base de dados** a
> responder a cada consulta, em vez de fixar a consulta que o serviço usa hoje — assim voltam a
> falar de faturas e não de queries.

**Execução:** `mvn -o test -Dtest=PageQueryTest,ReportServiceTest` → 18 testes, 0 falhas
(2026-08-15).

---

## Manuais (backend de pé)

| ID | Passos | Esperado |
|---|---|---|
| PG-50 | Abrir Vendas → Faturação | tabela com 50 linhas; rodapé "Página 1 de N · X registo(s)" |
| PG-51 | Carregar em "seguinte" | linhas mudam; contador avança; setas de trás activam |
| PG-52 | Mudar "Por página" para 200 | volta à página 1 com 200 linhas |
| PG-53 | Na última página | setas de avanço desactivadas |
| PG-54 | POS → Histórico de Vendas | resumo diz "vendas **nesta página** (de N) — total da página" |
| PG-55 | `GET /api/comercial/invoices/page?companyId=1&size=999999` | devolve no máximo 200 itens |
| PG-56 | Empresa sem faturas | "Sem registos"; setas todas desactivadas |

**Estado:** por executar (exigem backend de pé + desktop).
