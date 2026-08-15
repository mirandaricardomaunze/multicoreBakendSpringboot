# Paginação e leituras limitadas — especificação

**Criado em:** 2026-08-15
**Estado:** fundação implementada + 2 listagens e 2 relatórios migrados; adopção incremental
**Origem:** lacuna levantada na auditoria de gestão de 2026-08-09 (`tasks/current.md`): *"**zero
paginação** em todo o sistema (o dashboard carrega todas as faturas da empresa)"*.

---

## 1. O problema

Todas as leituras eram do tipo "traz tudo e filtra em memória":

```java
List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId);   // a tabela inteira
List<Invoice> salesToday = invoices.stream().filter(...).toList();       // …para ficar com um dia
```

Duas consequências distintas, ambas más:

1. **Ecrãs de listagem** (faturas, histórico de vendas do POS) traziam todos os registos por
   HTTP e enchiam a tabela Swing com eles. Com um ano de operação são dezenas de milhares de
   linhas que ninguém vai ler.
2. **Dashboard e relatório diário** liam o histórico completo **a cada abertura do ecrã** só
   para responder sobre *hoje*. É o pior dos dois: cresce sem limite e corre a toda a hora.

Numa loja com pouco movimento isto não se nota. É exactamente por isso que é perigoso: só dá
sinal quando o negócio já cresceu.

---

## 2. A regra canónica

### `PageQuery.of(page, size)` — normalização, com tecto
| Entrada | Resultado |
|---|---|
| sem parâmetros | página 0, tamanho 50 |
| página negativa | 0 |
| tamanho ≤ 0 | 50 |
| tamanho > 200 | **200** (encaixado, não recusado) |

O tecto é do servidor. Um cliente que peça `size=1000000` não pode obrigar o servidor a
materializar a tabela inteira — seria repor exactamente o problema que a paginação veio
resolver.

### `PageResponse<T>` — a página na fronteira HTTP
Record simples (`items`, `page`, `size`, `totalElements`, `totalPages` + `hasNext`/`hasPrevious`).
**Não** se devolve o `Page` do Spring Data: o seu JSON é instável entre versões e cheio de campos
internos, e o desktop cliente-fino teria de o conhecer.

### As perguntas vão à base de dados
O dashboard e o relatório diário deixaram de varrer a tabela:

| Pergunta | Antes | Agora |
|---|---|---|
| vendas de hoje | tudo + filtro em memória | `findByCompanyIdAndCreatedAtBetween(...)` |
| por cobrar | tudo + filtro em memória | `findByCompanyIdAndStatusIn(..., collectableStatuses())` |

`InvoiceStatus.collectableStatuses()` e `realisedSaleStatuses()` **derivam dos predicados**
(`isCollectable`/`isRealisedSale`) em vez de repetirem a lista — senão passavam a existir duas
definições de "cobrável", que é o bug que este projecto já fechou duas vezes.

---

## 3. O que foi migrado

| Superfície | Estado |
|---|---|
| `GET /api/comercial/invoices/page` | ✅ paginado |
| `GET /api/comercial/pos-sales/page` | ✅ paginado |
| Dashboard (`buildStoreDashboard`) | ✅ consultas com filtro |
| Relatório diário (`buildDailyStoreReport`) | ✅ consulta por intervalo |
| Aba Faturação (desktop) | ✅ `TablePager` |
| Histórico de vendas do POS (desktop) | ✅ `TablePager` |
| Restantes ~55 tabelas | ⏳ adopção incremental |

As listagens completas (`/invoices`, `/pos-sales`) **mantêm-se** para os ecrãs ainda não
migrados — mesmo padrão de adopção incremental usado no `loadAsync`.

### `TablePager` (desktop)
Componente único: primeira/anterior/seguinte/última, selector de tamanho (25/50/100/200) e
"Página X de Y · N registo(s)". Não sabe falar HTTP — recebe um `loader (página, tamanho)`, para
que o painel continue a ser o único a fazer pedidos.

**Nota de honestidade nos totais:** o resumo do histórico do POS passou a dizer *"vendas nesta
página … total da página"*. Um total que só soma a página visível não pode ser apresentado como
o total da loja.

---

## 4. Limites conhecidos (v1)

- Os filtros do `TableFilter` (pesquisa, estado, período) continuam a ser aplicados **do lado do
  cliente**, ou seja, à página carregada. Numa listagem paginada isso significa "filtrar dentro
  desta página". Filtrar no servidor é o passo seguinte e implica levar os critérios ao endpoint.
- Sem ordenação escolhida pelo utilizador: a ordem é fixa (mais recente primeiro).
- `getAllInvoices`, `searchInvoices` e as contas correntes continuam sem limite.
