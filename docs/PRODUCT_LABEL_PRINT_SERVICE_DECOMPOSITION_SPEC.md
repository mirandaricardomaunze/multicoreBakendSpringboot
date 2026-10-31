# Especificação de Decomposição: ProductLabelPrintService (Fase 173)

## Contexto e Problema
`ProductLabelPrintService` (198 linhas) combinava a consulta de produtos e empresa com a geração de código de barras Code128 e composição das grelhas PDF em formato A4 (3 colunas) e bobina térmica 80mm.

## Solução Arquitectural
1. **`ProductLabelPrintService` (Fachada — `<= 75` linhas)**:
   - Valida a empresa e os produtos seleccionados, limitando o número de cópias a `1..200`.
   - Delega a renderização PDF para `ProductLabelDocumentRenderer`.
   - Mantém construtor compatível `ProductLabelPrintService(ComercialService, CompanyService)`.
2. **`ProductLabelDocumentRenderer` (`@Component` — `<= 155` linhas)**:
   - Constrói as folhas de etiquetas em A4 e bobina térmica 80mm com códigos de barras Code128, lote e validade.
