# Especificação de Decomposição: SalesReturnService (Fase 192)

## Objetivo
Decompor `SalesReturnService.java` (164 linhas) numa fachada enxuta (`<= 60` linhas) que delega o apuramento de linhas devolvíveis e o processamento do assistente de devolução para colaboradores coesos.

## Arquitetura Alvo
1. `SalesReturnService.java` (`<= 60` linhas) — Fachada `@Service` preservando o construtor legado.
2. `ReturnableInvoiceLineCalculator.java` (`<= 75` linhas) — Apuramento de saldos devolvíveis por linha de fatura, descontando notas de crédito anteriores e calculando composição de embalagens (`getReturnableLines`).
3. `SalesReturnWizardProcessor.java` (`<= 115` linhas) — Validação de quantidades, emissão de Nota de Crédito e tentativa de auto-aprovação (`processReturn`).
