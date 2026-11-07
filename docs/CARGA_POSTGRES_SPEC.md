# Carga e integridade em PostgreSQL — Especificação e harness

**Última actualização:** 2026-11-07

## Objectivo

Medir a venda POS em paralelo em PostgreSQL e provar que a conta bate no fim. Complementa
[ROBUSTEZ_MULTIUTILIZADOR_SPEC.md](ROBUSTEZ_MULTIUTILIZADOR_SPEC.md), que define as protecções.

## Como correr

Usar sempre uma instância descartável, com dados de demonstração: os guiões escrevem na base.

1. Levantar o backend com o perfil `prod` sobre PostgreSQL, com Flyway, `ddl-auto=validate` e
   `app.seed-demo-data=true`. As guardas de produção obrigam a:
   - `server.address=127.0.0.1`, porque há dados de demonstração;
   - `ATTACHMENT_KEY` aleatória;
   - `DB_PASSWORD` com pelo menos 16 caracteres;
   - nesta instância apenas, `security.two-factor.required=false`, porque os guiões não fazem 2FA.
2. O perfil `prod` substitui as senhas de demonstração no arranque. Na base descartável, repor o hash
   BCrypt de `password` nas contas de demonstração depois de cada arranque.
3. `python scripts/exercitar-modulos.py <porta>` (opcional para a carga)
4. `python scripts/carga-pos.py <porta> 5,10,20,40 30`

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

## Evidência de 2026-11-07

A carga correu sobre uma base acabada de criar, sem `exercitar-modulos.py` antes, de propósito: assim
a primeira rajada de vendas cria o "Consumidor Final" e a série FT do ano. Foi isso que revelou dois
defeitos que a ordem de 31-10 escondia.

| Postos | Vendas | Erros | Vendas/s | p50 ms | p95 ms | p99 ms | Máx. ms |
|-------:|-------:|------:|---------:|-------:|-------:|-------:|--------:|
| 5 | 1.990 | 0 | 66,3 | 56 | 166 | 280 | 760 |
| 10 | 3.302 | 0 | 110,1 | 87 | 112 | 139 | 418 |
| 20 | 3.294 | 0 | 109,8 | 169 | 243 | 493 | 630 |
| 40 | 3.666 | 0 | 122,2 | 324 | 387 | 470 | 727 |

- 12.252 vendas aceites, stock reduzido em 12.252 e 12.252 pagamentos gravados.
- Faturas numeradas de 1 a 12.252, sem duplicados nem buracos (CG-04).
- 0 linhas `ERROR` no log do backend.
- Inclui a verificação de reservas de pedidos de separação em cada venda (`SaleStockIssuer`). Mesma
  máquina de 31-10; a diferença de débito não deve ser lida como ganho de desempenho.

**Defeitos encontrados e corrigidos:**

| Id | Defeito | Correcção |
|----|---------|-----------|
| CG-06 | Numa base nova, as primeiras vendas simultâneas criavam o "Consumidor Final" ao mesmo tempo; 4 em 5 falhavam por chave duplicada. | `WalkInClientBootstrap` cria-o no arranque e quando nasce uma empresa. Se mesmo assim faltar, é criado numa transacção à parte, e quem perde a corrida lê o registo do outro. |
| CG-07 | A primeira fatura simultânea de cada ano criava a série FT ao mesmo tempo; em PostgreSQL o INSERT recusado abortava a transacção e 4 vendas saíam com erro 500. Repetia-se a cada 1 de Janeiro. | `DocumentSequenceBootstrap` cria as séries do ano e do seguinte. A criação passou para uma transacção à parte. |
| CG-08 | `carga-pos.py` reutilizava a ligação do administrador, parada durante a ronda; em `prod` o servidor fecha-a aos 20 s e o Windows dá o erro 10053. | O guião religa antes de usar uma ligação parada há mais de 15 s. |
| CG-09 | `exercitar-modulos.py` enviava o recibo com campos antigos e lia a recusa como "folha já processada"; o RHC-53 parecia falhar. | Corpo actualizado; a nota mostra a resposta real. |

`exercitar-modulos.py`: 42 verificações aprovadas, 0 falhas, numa base nova. A falha antiga do RHC-53
não estava na contabilidade, estava no guião: o pedido do recibo usava campos que a API já não aceita
(`overtimeHours` e `bonuses` em vez de `overtime` e `allowances`). Como a nota presumia "folha já
processada", o recibo nunca era pago e não havia lançamento. O guião foi corrigido e passou a mostrar
a resposta real (CG-09).

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
