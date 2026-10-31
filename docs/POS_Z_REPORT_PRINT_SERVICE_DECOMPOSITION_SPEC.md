# Especificação Técnica: Decomposição do POSZReportPrintService (Fase 130)

## Diagnóstico

`POSZReportPrintService` (294 linhas) concentrava o carregamento da sessão e empresa activa, a composição
A4 principal (cabeçalho, metadados, reconciliação de gaveta, meios de pagamento e assinaturas) e a
construção de secções complementares (passagens de turno, parsing JSON de notas/moedas e observações).

## Decisão

- `POSZReportPrintService`: fachada tenant-scoped que obtém `PosZReportDTO` e `Company` e delega a emissão.
- `PosZReportDocumentRenderer`: composição do PDF A4, cabeçalho, reconciliação da gaveta, meios de pagamento e assinaturas.
- `PosZReportSectionBuilder`: tabelas opcionais de passagem de turno, contagem física de notas/moedas (JSON) e observações do fecho.
- O construtor legado `(POSService, CompanyService)` permanece intacto.
