# Harness de Decomposição do GuideRemittancePrintService

`GuideRemittancePrintServiceDecompositionHarnessTest` assegura que a composição PDF A4 paisagem permanece
extraída em `GuideRemittanceDocumentRenderer`, mantendo a fachada `GuideRemittancePrintService` restrita ao
controlo tenant-scoped e delegação dentro dos limites físicos de linhas.
