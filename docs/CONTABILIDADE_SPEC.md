# Contabilidade (PGC-NIRF) — especificação

**Criado em:** 2026-08-15
**Estado:** v1 implementada (backend + desktop), automatizada em CT-01..CT-46
**Origem:** lacuna levantada na auditoria de gestão de 2026-08-09 (`tasks/current.md`): *"**sem
contabilidade** (nem plano de contas, nem razão, nem balancete). Esta última é a maior ausência
para um ERP de gestão."*
**Decisões do utilizador (2026-08-15):** plano **PGC-NIRF de Moçambique**; lançamentos
**automáticos + manuais**.

---

## 1. O problema

O sistema registava vendas, compras, caixa, tesouraria e salários — e nada disso chegava a ser
contabilidade. Não havia plano de contas, não havia razão, não havia balancete. Um contabilista
que pedisse o extracto da conta Clientes não tinha resposta; um gerente que quisesse saber o
resultado do mês tinha de o reconstruir a partir de listagens.

---

## 2. O plano de contas

**PGC-NIRF** (Plano Geral de Contabilidade de Moçambique), semeado por empresa. Não é o plano
oficial completo: é o recorte que este ERP movimenta (vendas, compras, IVA, caixa, banco,
clientes, fornecedores, existências, pessoal). O resto acrescenta-se pelo ecrã — a estrutura de
códigos é a mesma, nada tem de ser refeito.

| Classe | Conteúdo |
|---|---|
| 1 | Meios circulantes financeiros (Caixa 1101, Banco 1201) |
| 2 | Terceiros (Clientes 2101, Fornecedores 2201, IVA 2431/2432, Pessoal 2601) |
| 3 | Existências (Mercadorias 3201) |
| 4 | Imobilizado |
| 5 | Capital e reservas |
| 6 | Custos e perdas (CMVMC 6101, FSE 6201, Pessoal 6301) |
| 7 | Proveitos e ganhos (Vendas 7101, Serviços 7201) |
| 8 | Resultados |

### Duas regras que não são óbvias

**A natureza é da conta, não da classe.** Clientes (2101) e Fornecedores (2201) são ambos classe
2 e têm naturezas opostas; IVA liquidado (2431) é credor e IVA dedutível (2432) é devedor, na
mesma sub-conta. Derivar a natureza da classe daria metade do balancete com o sinal trocado —
por isso `AccountNature` é **gravada em cada conta**. (Um teste do balancete falhou exactamente
por isto durante o desenvolvimento; ver harness CT-42.)

**Só folhas aceitam lançamentos.** Contas-mãe (`postable = false`) existem para somar. Lançar
numa conta-mãe é o erro clássico que faz o balancete deixar de fechar por classes.

A sementeira é **idempotente**: semear por cima criaria duplicados ou reporia contas que o
contabilista desactivou.

---

## 3. A partida dobrada

`JournalEntry.validateForPosting()` é a **única porta de validação** — venha o lançamento do
contabilista ou de uma fatura. Recusa:

| Situação | Porquê |
|---|---|
| débito ≠ crédito | um lançamento desequilibrado gravado é um balancete que nunca mais fecha e ninguém sabe quando começou |
| menos de duas partidas | não é partida dobrada |
| partida com débito **e** crédito | ambígua; é assim que entram lançamentos que parecem equilibrados sem o estar |
| partida sem valor, ou negativa | valores negativos não são partidas — inverte-se o lado |
| total zero | não movimenta nada |
| conta-mãe ou inactiva | ver §2 |

---

## 4. Numeração e multi-empresa

Série **`LC`**, numerada por empresa (`UNIQUE(company_id, entry_number)`) — a lição da V31. O
plano de contas também é por empresa: duas empresas podem ter planos diferentes.

---

## 5. Lançamentos automáticos

O módulo comercial **não conhece a contabilidade**. Publica factos
(`SaleRegisteredEvent`, `PaymentReceivedEvent`) com **números, nunca entidades JPA**, e o
`AutomaticPostingService` traduz. `@EventListener` síncrono, na **mesma transacção**: se o
lançamento falhar, a venda não fica gravada pela metade.

### Venda (fatura, incluindo POS)
```
D  Clientes 2101        total          (a parte a crédito)
D  Caixa 1101 / Banco 1201             (a parte paga no acto)
C  Vendas 7101          líquido
C  IVA liquidado 2431   imposto
D  CMVMC 6101           custo          ┐ só quando o custo é conhecido
C  Mercadorias 3201     custo          ┘
```
O custo vem da fotografia gravada na linha (ver
[MARGEM_CUSTO_HISTORICO_SPEC.md](MARGEM_CUSTO_HISTORICO_SPEC.md)). **Sem custo conhecido não se
inventa um** para o lançamento ficar bonito — fica só a venda.

### Recebimento (recibo)
```
D  Caixa / Banco    valor
C  Clientes 2101    valor
```
**Só o movimento de saldo.** O proveito já foi lançado na emissão da fatura; lançá-lo outra vez
aqui contaria a mesma venda duas vezes — o erro clássico deste tipo de integração.

### Duas travas
- **Sem plano de contas, não lança e não estoira.** Uma empresa que ainda não semeou o plano tem
  de poder continuar a vender; a contabilidade é opcional até alguém a ligar.
- **Um documento, um lançamento.** Índice único `(company_id, source, source_document_id)` +
  verificação no serviço: repetir o pedido HTTP não pode duplicar as vendas na contabilidade.

Uma fatura à espera de aprovação de desconto **não** é lançada — o stock ainda não se moveu,
não é venda.

---

## 6. Razão e balancete

- **Balancete** (`GET /api/accounting/trial-balance?from&to`): por conta movimentada, totais a
  débito e a crédito e saldo com o sinal da natureza. O campo `balanced` não é decoração: se os
  totais não baterem, o relatório diz **"NÃO FECHA"** em vez de apresentar números com ar de
  certos.
- **Razão** (`GET /api/accounting/ledger/{conta}?from&to`): movimentos da conta com **saldo de
  abertura** (tudo o que foi lançado antes do período) e saldo acumulado por movimento. Sem o
  saldo de abertura, o extracto de Março começaria do zero e ninguém saberia quanto o cliente já
  devia a 1 de Março.

---

## 7. Limites conhecidos (v1)

- **Salários não lançam automaticamente.** O evento de folha salarial ainda não é publicado; o
  processamento lança-se à mão (D 6301 / C 2601). É o próximo a ligar.
- **Compras não lançam automaticamente.** Mesma razão; o plano já tem as contas (2201, 2432,
  3201).
- **Notas de crédito/débito** não geram estorno contabilístico automático.
- **Sem fecho de exercício**: não há apuramento de resultados nem transporte de saldos para o ano
  seguinte (classe 8 existe no plano mas não é movimentada automaticamente).
- **Sem balanço nem demonstração de resultados** formatados — o balancete é a base para os
  construir.
- O par Caixa/Banco escolhe-se por um booleano (`cashPayment`): numa venda com métodos mistos,
  considera-se caixa se **algum** dos pagamentos for numerário. Repartir por método é v2.

---

## 8. Migração V38

Três tabelas: `accounts`, `journal_entries`, `journal_lines`. Índice único parcial em
`(company_id, source, source_document_id)` para a trava de duplicação.
