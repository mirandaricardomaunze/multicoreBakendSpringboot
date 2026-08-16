# Conferência à chegada — especificação

**Criado em:** 2026-08-16
**Estado:** implementado (backend), automatizado em CC-01..CC-17
**Origem:** pergunta do utilizador sobre logística. Das quatro peças analisadas, é a única que
**serve a todos os perfis** — supermercado, loja, mercearia e armazém.

---

## 1. O problema

Encomendaste 100 sacos. Chegam 97, e 2 vêm rasgados. Ficas com 95 bons — e a factura do
fornecedor diz 100.

O sistema já sabia receber 97 em vez de 100 (recepção parcial, V19). O que **não** sabia era o
resto da história: que 2 chegaram estragados, que 3 nunca vieram, e **quanto é que isso vale**.

Não é um roubo grande. São 3 sacos de cada vez, todas as semanas, durante um ano — e ninguém
consegue reclamar seja o que for, porque não há registo de nada.

---

## 2. A conferência reparte o que foi encomendado em três

```
encomendado 100 = 95 boas (entram em stock) + 2 danificadas + 3 em falta
```

| Parte | Entra em stock? | Fecha a linha? | Porquê |
|---|---|---|---|
| **Boas** | ✅ sim | sim | é a mercadoria que existe |
| **Danificadas** | ❌ não | **não** | chegou, mas não se pode vender. Dar por recebida fecharia a encomenda a esconder o problema |
| **Em falta** | ❌ não | **sim** | é o *fecho curto*: declara-se que não virão, para a encomenda não ficar eternamente por receber |

**Guarda:** boas + danificadas + em falta não podem exceder o que estava por receber. Sem isto,
registar 5 danificados numa linha com 2 por receber inventava divergências que nunca existiram.

---

## 3. O registo

`GoodsReceiptDiscrepancy` — **uma linha por ocorrência**, não um saldo acumulado: a data e a
quantidade daquele dia são a prova de que se precisa para reclamar.

Guarda o **preço unitário da encomenda**, porque o que se reclama é o valor, não a quantidade.
`amount()` = quantidade × preço.

Fornecedor gravado por **id e nome**: o relatório não tem de atravessar a encomenda, e o nome
fica como estava à data da ocorrência.

`DAMAGED` e `MISSING` são registos separados de propósito. São coisas diferentes e tratam-se de
forma diferente: a mercadoria danificada **chegou** (o fornecedor vai querer receber por ela), a
que falta **não chegou**. No mesmo saco, perdia-se a única informação que serve para reclamar.

---

## 4. O relatório (é aqui que a perda fica visível)

| Endpoint | O que dá |
|---|---|
| `GET /api/purchases/discrepancies?from&to` | ocorrências do período |
| `GET /api/purchases/discrepancies/open` | só o que ainda há a reclamar |
| `GET /api/purchases/discrepancies/by-supplier?from&to` | resumo por fornecedor |
| `POST /api/purchases/discrepancies/{id}/resolve` | fecha uma ocorrência |

O resumo por fornecedor vem **ordenado do que mais custou para o que menos custou**. É essa
ordenação que faz o trabalho: quem aparece em cima é aquele com quem vale a pena ter a conversa.

Separa **total ocorrido** de **por reclamar** — o que já foi creditado ou substituído não deve
continuar a inflacionar a queixa.

**Resolver exige explicação** (nota de crédito, substituição, perdoado). Sem ela, "resolvido" não
vale nada daqui a seis meses.

---

## 5. Compatibilidade

`ReceiveLine` ganhou `damagedQuantity`, `missingQuantity` e `notes` — todos **opcionais**, com
construtor retrocompatível. Uma recepção feita como antes continua a funcionar exactamente na
mesma e não grava divergência nenhuma (CC-03).

---

## 6. Limites conhecidos (v1)

- **Sem UI.** Só API. O ecrã de Compras ainda não mostra nem regista divergências.
- **Não liga à factura do fornecedor.** Sabe-se o que se recebeu a menos, mas o sistema não
  compara automaticamente com o valor facturado nem sugere a nota de crédito.
- **Não devolve mercadoria ao fornecedor** como documento (não há guia de devolução a
  fornecedor); a divergência é um registo, não um movimento.
- **Sem fotografia** da mercadoria danificada — numa reclamação a sério, costuma ser o que
  resolve a discussão.
- A mercadoria danificada **não entra em stock nem em quebras**: desaparece do circuito. Se se
  quiser contabilizá-la como perda, é preciso um lançamento à parte.
