# Contabilidade (PGC-NIRF) — harness

Cenários de verificação da [CONTABILIDADE_SPEC.md](CONTABILIDADE_SPEC.md).
**CT-01..CT-46** automáticos; **CT-50..CT-60** manuais (com o backend de pé).

---

## Automáticos

### `JournalEntryTest` — partida dobrada (JUnit puro)

| ID | Cenário | Esperado |
|---|---|---|
| CT-01 | D 232 / C 200 + C 32 | passa; equilibrado |
| CT-02 | D 100 / C 90 | recusa; a mensagem diz **os dois totais** |
| CT-03 | Uma só partida | recusa — não é partida dobrada |
| CT-04 | Partida numa conta-mãe | recusa com "conta-mãe" |
| CT-05 | Partida com débito **e** crédito | recusa — ambígua |
| CT-06 | Partida sem valor | recusa |
| CT-07 | Valores negativos | recusa — "inverta o lado" |
| CT-08 | Lançamento de valor zero | recusa — não movimenta nada |
| CT-09 | Saldo por natureza (devedora vs credora) | sinais correctos nos dois casos |
| CT-10 | Classe pelo 1.º dígito | 1101→classe 1, 2101→2, 7101→7; código 9999 e vazio rejeitados |
| CT-11 | Código da conta-mãe | 2101→210, 21→2, 2→null |

### `AutomaticPostingServiceTest` — tradução dos factos

| ID | Cenário | Esperado |
|---|---|---|
| CT-20 | Venda a fiado de 232 (200+32) | D Clientes 232 / C Vendas 200 / C IVA 32 |
| CT-21 | Venda paga em numerário | entra em **Caixa**, não em Clientes |
| CT-22 | Venda paga por banco | entra em **Depósitos** |
| CT-23 | Venda de 116 com 40 pagos | D Caixa 40 + D Clientes 76 |
| CT-24 | Venda com custo 120 conhecido | + D CMVMC 120 / C Mercadorias 120; continua equilibrado |
| CT-25 | Venda sem custo conhecido | **não inventa** custo |
| CT-26 | Recebimento de 232 | D Caixa / C Clientes; **Vendas fica a zero** |
| CT-27 | Empresa sem plano de contas | não lança e **não estoira** |
| CT-28 | Documento já lançado | não lança segunda vez |
| CT-29 | Venda de valor zero | não gera lançamento |

### `AccountingReportServiceTest` — razão e balancete

| ID | Cenário | Esperado |
|---|---|---|
| CT-40 | Venda + recebimento no mesmo dia | balancete fecha; 464 = 464; Vendas 200; Caixa 232 |
| CT-41 | Cliente que vendeu e recebeu | saldo de Clientes = **0,00** |
| CT-42 | Contas credoras (Vendas, IVA) | saldo positivo do lado do crédito |
| CT-43 | Período sem lançamentos | vazio, mas `balanced = true` |
| CT-44 | Extracto de Março com venda de Janeiro | **saldo de abertura 500**; fecho 732 |
| CT-45 | Venda 232 + recebimento 100 | saldo acumulado 232 → 132 |
| CT-46 | Razão de Vendas | só os movimentos dessa conta; documento de origem visível |

> CT-42 apanhou um erro real durante o desenvolvimento: o teste derivava a natureza da **classe**
> e o IVA liquidado (classe 2, credora) saía com o sinal trocado. Confirmou a decisão de gravar
> a natureza em cada conta — ver spec §2.

**Execução:** `mvn -o test -Dtest=JournalEntryTest,AutomaticPostingServiceTest,AccountingReportServiceTest`
→ 28 testes, 0 falhas (2026-08-15).

---

## Manuais (backend de pé)

| ID | Passos | Esperado |
|---|---|---|
| CT-50 | Contabilidade → Plano de Contas → "Semear PGC-NIRF" | ~35 contas criadas |
| CT-51 | Semear outra vez | "já tem plano de contas — nada foi alterado" |
| CT-52 | Emitir fatura de 232,00 a fiado | Diário mostra `LC-…` com origem **Fatura** e o nº FT |
| CT-53 | Razão da conta 2101 | o cliente aparece a dever 232,00 |
| CT-54 | Emitir recibo de 232,00 | novo `LC-…` com origem **Recibo**; razão de 2101 volta a zero |
| CT-55 | Balancete do mês | fecha (débito = crédito); mostra "Balancete fecha" |
| CT-56 | Venda no POS paga em numerário | D Caixa 1101, sem passar por Clientes |
| CT-57 | Lançamento manual desequilibrado (D 100 / C 90) | recusa com a mensagem dos dois totais |
| CT-58 | Lançamento manual válido (D 6201 / C 1101) | aparece no diário com origem **Manual** |
| CT-59 | Repetir o POST da mesma fatura | **não** cria segundo lançamento |
| CT-60 | Empresa nova, sem plano, a vender | venda passa; nada é lançado; sem erro ao operador |

**Estado:** por executar (exigem backend de pé + desktop).
