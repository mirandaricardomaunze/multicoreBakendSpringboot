# Especificação de Decomposição: InventoryCountSheetPrintService (Fase 177)

## Contexto e Problema
`InventoryCountSheetPrintService` (187 linhas) combinava a consulta de saldos por empresa ou armazém com a renderização integral da Folha de Contagem Cega de Inventário em PDF A4.

## Solução Arquitectural
1. **`InventoryCountSheetPrintService` (Fachada — `<= 70` linhas)**:
   - Valida a empresa e obtém as existências ordenadas por armazém e artigo.
   - Delega a renderização PDF para `InventoryCountSheetDocumentRenderer`.
   - Mantém construtor compatível de 3 argumentos.
2. **`InventoryCountSheetDocumentRenderer` (`@Component` — `<= 145` linhas)**:
   - Constrói o documento A4 de contagem cega (cabeçalho, âmbito, tabela com caixa de contagem em branco e assinaturas).
