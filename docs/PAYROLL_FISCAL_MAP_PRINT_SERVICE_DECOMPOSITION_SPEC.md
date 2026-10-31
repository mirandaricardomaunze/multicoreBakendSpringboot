# Especificação de Decomposição: PayrollFiscalMapPrintService (Fase 183)

## Objetivo
Decompor `PayrollFiscalMapPrintService.java` (174 linhas) numa fachada de carregamento (`<= 55` linhas) e num renderizador OpenPDF dedicado (`PayrollFiscalMapDocumentRenderer.java`).

## Arquitetura Alvo
1. `PayrollFiscalMapPrintService.java` (`<= 55` linhas) — Fachada `@Service` que carrega a empresa e o resumo fiscal (`PayrollFiscalSummaryDTO`) e delega a composição para `PayrollFiscalMapDocumentRenderer`.
2. `PayrollFiscalMapDocumentRenderer.java` (`<= 145` linhas) — Componente `@Component` que constrói o documento PDF A4 com cabeçalho, período de referência, grelha de retenções INSS/IRPS, totais e assinaturas.
