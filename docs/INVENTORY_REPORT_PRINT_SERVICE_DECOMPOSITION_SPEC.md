# Especificação Técnica: Decomposição do InventoryReportPrintService (Fase 139)

## Diagnóstico

`InventoryReportPrintService` (252 linhas) concentrava a consulta e ordenação dos saldos de stock por
armazém/empresa e a composição PDF A4 (cabeçalho, âmbito, tabela de artigos com conversão em caixas,
resumo valorizado e assinaturas).

## Decisão

- `InventoryReportPrintService`: fachada que valida a empresa, carrega o armazém opcional e a lista ordenada de `Stock` e delega a emissão.
- `InventoryReportDocumentRenderer`: composição do PDF A4 do Inventário de Stock.
- Construtor legado `(StockRepository, WarehouseRepository, CompanyService)` preservado.
