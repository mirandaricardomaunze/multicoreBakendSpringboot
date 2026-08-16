# Limite de crédito do cliente — harness

Cenários de verificação da [LIMITE_CREDITO_SPEC.md](LIMITE_CREDITO_SPEC.md).
**LC-01..LC-32** automáticos; **LC-50..LC-56** manuais (com o backend de pé).

---

## Automáticos

### `ClientCreditLimitTest` — aritmética do domínio (JUnit puro)

| ID | Cenário | Esperado |
|---|---|---|
| LC-01 | Cliente sem limite, dívida de 999.999 | passa; `creditAvailable` = `null` |
| LC-02 | Limite 10.000, dívida 3.000, venda 2.000 | passa; disponível 7.000 |
| LC-03 | Limite 10.000, dívida 6.000, venda 4.000 | passa — o tecto é inclusivo |
| LC-04 | O mesmo com venda de 4.000,01 | recusa |
| LC-05 | Limite 0,00 e venda de 0,01 | recusa — zero é "não vende fiado" |
| LC-06 | Já estourado, venda com dívida nova 0 (ou nula) | passa — pagar a pronto não consome crédito |
| LC-07 | Dívida 2.500 sobre limite 1.000 | disponível = 0, nunca negativo |

### `ReceivablesServiceTest` — exposição e trava

| ID | Cenário | Esperado |
|---|---|---|
| LC-10 | Parcial (600 de saldo) + fiado 300 + paga + anulada + outro cliente | dívida do cliente = **900** |
| LC-11 | Sem limite, dívida 50.000, venda 90.000 | passa |
| LC-12 | Limite 10.000, dívida 3.000, venda 7.000 | passa |
| LC-13 | Limite 10.000, dívida 8.000, venda 3.000 | recusa; mensagem diz nome, limite, dívida e disponível |
| LC-14 | Limite estourado, venda paga na hora | passa |
| LC-15 | Recusada; cliente paga 5.000; repete a venda | passa — receber liberta crédito |

### `ComercialServiceTest` — porta da faturação

| ID | Cenário | Esperado |
|---|---|---|
| LC-20 | `createInvoice` com o limite estourado | recusa; **sem** número FT consumido, sem gravação, sem saída de stock |
| LC-21 | `createInvoice` de 2 × 100 + 16% | a trava é chamada com **232,00** (o total) |

### `POSServiceTest` — porta do balcão

| ID | Cenário | Esperado |
|---|---|---|
| LC-30 | Venda de 116,00 com 40,00 em numerário | a trava é chamada com **76,00** (só o fiado) |
| LC-31 | Venda paga na totalidade | a trava é chamada com **0,00** |
| LC-32 | Fiado acima do limite | recusa; **sem** fatura gravada e **sem saída de stock** |

**Execução:** `mvn -o test -Dtest=ClientCreditLimitTest,ReceivablesServiceTest,ComercialServiceTest,POSServiceTest,InvoiceAgingTest`
→ 90 testes, 0 falhas (2026-08-14).

> LC-32 apanhou um defeito real durante a implementação: a saída de stock do POS estava dentro
> do ciclo das linhas e corria **antes** da trava. Ver §3 da spec.

---

## Manuais (backend de pé)

| ID | Passos | Esperado |
|---|---|---|
| LC-50 | Cadastrar cliente com limite 5.000 MT | tabela de Clientes mostra "5.000,00 MT" |
| LC-51 | Cliente sem limite | coluna mostra "Sem limite" |
| LC-52 | Emitir fatura de 3.000 a esse cliente | passa |
| LC-53 | Emitir segunda fatura de 3.000 | recusa com a mensagem dos números; a série FT **não** salta |
| LC-54 | Receber 3.000 e repetir | passa |
| LC-55 | POS: venda paga a dinheiro a cliente estourado | passa |
| LC-56 | POS: venda a fiado a cliente estourado | recusa; stock **não** baixa |

**Estado:** por executar (exigem backend de pé + desktop).
