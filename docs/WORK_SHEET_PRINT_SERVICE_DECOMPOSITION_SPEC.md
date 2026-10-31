# Especificação de Decomposição: WorkSheetPrintService (Fase 160)

## Contexto e Problema
`WorkSheetPrintService` (219 linhas) combinava a consulta da folha de obra e validação multi-empresa com a construção integral das tabelas PDF A4 (bloco de pedido, intervenção, linhas de mão de obra/peças, resumo sem IVA e nota fiscal).

## Solução Arquitectural
1. **`WorkSheetPrintService` (Fachada — `<= 55` linhas)**:
   - Obtém `WorkSheet` via `WorkSheetRepository.findByIdAndSupportTicketCompanyId(...)`.
   - Delega a renderização PDF para `WorkSheetDocumentRenderer`.
   - Mantém construtor compatível `WorkSheetPrintService(WorkSheetRepository)`.
2. **`WorkSheetDocumentRenderer` (`@Component` — `<= 195` linhas)**:
   - Constrói o documento A4 com cabeçalho, carimbo de anulação, bloco de pedido, intervenção, linhas, resumo sem IVA e nota fiscal.
