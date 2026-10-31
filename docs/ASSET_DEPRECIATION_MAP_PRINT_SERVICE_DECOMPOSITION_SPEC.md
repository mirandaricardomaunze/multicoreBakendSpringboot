# Especificação de Decomposição: AssetDepreciationMapPrintService (Fase 166)

## Contexto e Problema
`AssetDepreciationMapPrintService` (210 linhas) combinava a consulta da empresa e activos imobilizados com a renderização integral do Mapa Oficial de Amortizações e Reintegrações A4 horizontal (Modelo 22 — Anexo D PGC-NIRF).

## Solução Arquitectural
1. **`AssetDepreciationMapPrintService` (Fachada — `<= 55` linhas)**:
   - Valida a empresa e obtém os activos via `AssetRepository.findByCompanyIdOrderByAssetCodeAsc(...)`.
   - Delega a renderização PDF para `AssetDepreciationMapDocumentRenderer`.
   - Mantém construtor compatível `AssetDepreciationMapPrintService(AssetRepository, CompanyService)`.
2. **`AssetDepreciationMapDocumentRenderer` (`@Component` — `<= 185` linhas)**:
   - Constrói o documento A4 horizontal com cabeçalho fiscal, tabela de 10 colunas de amortização e bloco de assinatura OCAM/Gerência.
