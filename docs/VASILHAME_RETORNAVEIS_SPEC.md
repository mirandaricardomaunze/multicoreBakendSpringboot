# Spec — Vasilhame e embalagens retornáveis (SA-C12, F5)

**Data:** 2026-11-09 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

O depósito de bebidas entrega 50 grades e recebe 42; a revenda de gás troca botijas; a fábrica de blocos deixa
paletes na obra. Ninguém sabe quantas grades estão em cada cliente nem há quanto tempo.

## Decisões (incluindo a decisão em aberto §7.4 da SPEC mestre)

1. **Só conta-corrente de quantidades, sem caução em dinheiro.** Receber caução seria um valor do cliente em
   poder da empresa, com tratamento fiscal próprio; fica fora. O que não volta é **faturado** (fatura FT normal,
   ao preço de venda do artigo), o que fecha essa quantidade.
2. **Artigo retornável** (`returnable_items`): um produto que controla stock, marcado pela gestão.
3. **Movimentos** (`returnable_movements`): entregue (OUT), devolvido (IN) e cobrado (CHARGED). O saldo por
   cliente e artigo só muda por movimento; uma `requestKey` evita duplicar.
4. **Stock:** o vasilhame continua a ser da empresa. A entrega passa-o para o armazém **"Vasilhame em
   clientes"** (trânsito, sem venda no POS); a devolução traz de volta à loja; a cobrança fatura a partir desse
   armazém. O componente `TransitWarehouseProvider` é partilhado com a consignação.
5. **Recusas:** entregar mais do que o stock, receber ou cobrar mais do que o saldo, Consumidor Final, artigo
   inactivo.
6. **Antiguidade:** "fora há N dias" conta a partir da entrega mais antiga ainda não coberta por devoluções e
   cobranças (FIFO).

## API (`/api/returnables`)

| Método | Caminho | Quem |
|---|---|---|
| GET / PUT | `/items` | vendedor lê · gestão grava |
| GET | `/balances?companyId=&clientId=` · `/movements?companyId=&days=` | vendedor |
| POST | `/movements` (OUT ou IN) | vendedor |
| POST | `/charges` | gestão |

Migração `V129__vasilhame_retornaveis.sql`: só tabelas novas.

## Interface

Módulo **Vasilhame** (capacidade `RETURNABLES`: estaleiro, fábrica de blocos, bebidas) com indicadores
(clientes com vasilhame, unidades e valor fora, fora há mais de 30 dias). Separadores **Saldos por Cliente**
(entregar, receber, cobrar não devolvidos), **Movimentos** e **Artigos Retornáveis**.

## Testes

`ReturnablesTest`: saldo e antiguidade FIFO; entrega, idempotência, stock, devolução a mais recusada, saldo e
valor, cobrança acima do saldo recusada, fatura que fecha o saldo.
