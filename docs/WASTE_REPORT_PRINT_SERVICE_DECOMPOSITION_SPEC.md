# Especificação Técnica: Decomposição do WasteReportPrintService (Fase 140)

## Diagnóstico

`WasteReportPrintService` (252 linhas) misturava a obtenção dos registos de quebras e resumo do período
com a composição do PDF A4 (cartões de KPI, tabela de itens de quebra, agregação por motivo e assinaturas).

## Decisão

- `WasteReportPrintService`: fachada que valida a empresa, resolve o intervalo temporal, consulta `StockWasteRepository` e `StockWasteService` e delega a emissão.
- `WasteReportDocumentRenderer`: composição integral do PDF A4 do Relatório de Quebras e Perdas de Stock.
- Construtor legado `(StockWasteRepository, StockWasteService, CompanyService)` preservado.
