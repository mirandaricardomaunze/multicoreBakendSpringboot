# Harness de Decomposição do OrderPrintService

`OrderPrintServiceDecompositionHarnessTest` fixa a separação entre carregamento tenant-scoped e
composição A4, exige os renderers corporativos de linhas, totais e assinaturas e limita a fachada a
55 linhas e o renderer extraído a 260 linhas.
