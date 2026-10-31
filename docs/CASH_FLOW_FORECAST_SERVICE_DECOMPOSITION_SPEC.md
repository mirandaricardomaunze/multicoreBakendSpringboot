# Especificação Técnica: Decomposição do CashFlowForecastService (Fase 135)

## Diagnóstico

`CashFlowForecastService` (260 linhas) concentrava a recolha de saldos de tesouraria, contas a receber e
contas a pagar com o agrupamento nos 7 baldes temporais, apuramento de défice acumulado e classificação de
alertas de liquidez.

## Decisão

- `CashFlowForecastService`: fachada que valida `PermissionGuard.requireManagerOrAdmin`, orquestra a recolha e o cálculo e preserva o construtor legado de 3 repositórios.
- `CashFlowDataCollector`: recolha de saldos de caixa/bancos, facturas pendentes (`RECEIVABLE`), compras em aberto (`PAYABLE`) e classificação no balde temporal (`determineBucket`).
- `CashFlowProjectionCalculator`: consolidação dos 7 baldes temporais, trajectória acumulada de liquidez, detecção de défice (`CRITICAL`/`WARNING`/`HEALTHY`) e ranking top 10.
