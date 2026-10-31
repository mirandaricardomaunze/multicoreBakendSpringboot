# Especificação de Decomposição: ValuedInventoryBookPrintService (Fase 161)

## Contexto e Problema
`ValuedInventoryBookPrintService` (218 linhas) combinava a consulta das existências e da empresa com a renderização integral do Livro de Inventário Valorizado A4 (cabeçalho do Código Comercial, tabela valorizada ao CUMP e termo de encerramento legal PGC-NIRF).

## Solução Arquitectural
1. **`ValuedInventoryBookPrintService` (Fachada — `<= 60` linhas)**:
   - Valida a empresa e obtém as existências positivas ordenadas.
   - Delega a renderização PDF para `ValuedInventoryBookDocumentRenderer`.
   - Mantém construtor compatível `ValuedInventoryBookPrintService(StockRepository, CompanyService)`.
2. **`ValuedInventoryBookDocumentRenderer` (`@Component` — `<= 190` linhas)**:
   - Constrói o documento A4 com cabeçalho oficial, tabela valorizada ao CUMP e termo de encerramento legal.
