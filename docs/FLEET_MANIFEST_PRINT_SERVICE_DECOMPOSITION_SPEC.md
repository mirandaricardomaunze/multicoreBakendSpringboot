# Especificação de Decomposição: FleetManifestPrintService (Fase 162)

## Contexto e Problema
`FleetManifestPrintService` (218 linhas) combinava a consulta do manifesto e validação multi-empresa com a construção integral das tabelas PDF A4 (informação de transporte, KPIs de carga, tabela de paradas e bloco de assinaturas).

## Solução Arquitectural
1. **`FleetManifestPrintService` (Fachada — `<= 55` linhas)**:
   - Obtém `FleetManifest` via `FleetManifestRepository.findByIdAndCompanyIdWithStops(...)`.
   - Delega a renderização PDF para `FleetManifestDocumentRenderer`.
   - Mantém construtor compatível `FleetManifestPrintService(FleetManifestRepository)`.
2. **`FleetManifestDocumentRenderer` (`@Component` — `<= 195` linhas)**:
   - Constrói o documento A4 do manifesto de carga e rota de distribuição.
