# Carga e integridade em PostgreSQL — Especificação e harness

**Última actualização:** 2026-10-31

## Objectivo

Medir a venda POS em paralelo em PostgreSQL e provar que a conta bate no fim. Complementa
[ROBUSTEZ_MULTIUTILIZADOR_SPEC.md](ROBUSTEZ_MULTIUTILIZADOR_SPEC.md), que define as protecções.

## Como correr

Usar sempre uma instância descartável, com dados de demonstração: os guiões escrevem na base.

1. Levantar o backend sobre PostgreSQL com Flyway, `ddl-auto=validate` e `app.seed-demo-data=true`.
2. `python scripts/exercitar-modulos.py <porta>`
3. `python scripts/carga-pos.py <porta> 5,10,20,40 30`

A ordem importa. Depois da carga há stock para vender as 999.999 unidades que
`exercitar-modulos.py` usa para provar a recusa por falta de stock, e essa verificação deixa de
fazer sentido.

Não usar `timeout` nem parar o terminal que lançou o `pg_ctl`: o PostgreSQL pode ser terminado com
ele. Foi o que aconteceu nesta validação.

## Harness

| Id | Cenário | Espera-se |
|----|---------|-----------|
| CG-01 | Rondas de 5, 10, 20 e 40 postos a vender sem pausas no mesmo artigo | 0 erros e p95 até 2.000 ms |
| CG-02 | Pagamentos CASH, CARD, BANK_TRANSFER, MPESA e EMOLA | Todos aceites e gravados |
| CG-03 | Stock no fim | Baixa exactamente o número de vendas aceites |
| CG-04 | Numeração FT no fim | Sem duplicados nem buracos |
| CG-05 | Paragem brusca do PostgreSQL | Recuperação automática, sem perder vendas aceites |

CG-01 a CG-03 são verificados por `carga-pos.py`. CG-04 e CG-05 foram conferidos em SQL.

## Evidência de 2026-10-31

Ambiente: um portátil Windows com PostgreSQL 18.2 e backend na mesma máquina, pool de 20 ligações.
Cada ronda durou 30 segundos, com um artigo por venda.

| Postos | Vendas | Erros | Vendas/s | p50 ms | p95 ms | p99 ms | Máx. ms |
|-------:|-------:|------:|---------:|-------:|-------:|-------:|--------:|
| 5 | 1.261 | 0 | 42,0 | 104 | 215 | 291 | 561 |
| 10 | 1.811 | 0 | 60,4 | 158 | 213 | 420 | 632 |
| 20 | 2.037 | 0 | 67,9 | 295 | 369 | 700 | 1.209 |
| 40 | 2.222 | 0 | 74,1 | 557 | 687 | 1.100 | 1.403 |

- 7.331 vendas aceites e stock reduzido em 7.331.
- 7.331 pagamentos gravados, incluindo 1.451 M-Pesa e 1.439 e-Mola.
- 7.345 facturas, com numeração de 1 a 7.345, sem duplicados nem buracos.
- Recuperação automática após paragem brusca, com os totais acima intactos.
- 0 linhas `ERROR` no log do backend.
- Com `exercitar-modulos.py` antes da carga: 33 verificações aprovadas e 1 de texto desactualizado,
  já corrigida. A repetição completa não terminou: foi interrompida pelo limite de 100 s, com 7 mil
  facturas na base.

## Limites

- Mede só a venda POS, sem relatórios, listagens nem impressão a concorrer.
- O débito cresce pouco acima de 20 postos: a venda é serializada pela série de facturas e pelo
  stock do mesmo artigo. Vendas de artigos diferentes não foram medidas.
- O número medido não é uma capacidade certificada para produção: o hardware final pode ser outro.
- `exercitar-modulos.py` fica lento com milhares de facturas. A causa não foi investigada; as
  listagens sem paginação são a primeira suspeita.
- Vendas pelo campo antigo `treasuryAccountId` não criam `payment_entries`. O desktop deve usar
  `payments`.
- Os 85 de stock inicial dos dados de demonstração não têm movimento, por isso o stock de sementeira
  não bate com o livro de movimentos.
