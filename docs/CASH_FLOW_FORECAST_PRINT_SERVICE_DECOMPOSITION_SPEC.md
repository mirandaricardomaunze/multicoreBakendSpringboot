# Especificação Técnica: Decomposição do CashFlowForecastPrintService (Fase 138)

## Diagnóstico

`CashFlowForecastPrintService` (254 linhas) misturava a obtenção da empresa e da projecção de fluxo de
caixa com a construção visual A4 (cartões métricos, banner de alerta, matriz de baldes temporais, top 10
recebimentos/pagamentos e assinaturas).

## Decisão

- `CashFlowForecastPrintService`: fachada que carrega `Company` e `CashFlowForecastDTO`, delega a emissão e preserva o construtor legado `(CashFlowForecastService, CompanyService)`.
- `CashFlowForecastDocumentRenderer`: composição integral do PDF A4 do Mapa Previsional de Fluxo de Caixa.
