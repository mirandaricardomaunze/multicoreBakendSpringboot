# Harness de Decomposição do POSZReportPrintService

`POSZReportPrintServiceDecompositionHarnessTest` garante que a composição PDF e o parsing de denominações
permanecem fora da fachada `POSZReportPrintService`, valida a delegação para `PosZReportDocumentRenderer`
e `PosZReportSectionBuilder` e impõe limites físicos de linhas por ficheiro.
