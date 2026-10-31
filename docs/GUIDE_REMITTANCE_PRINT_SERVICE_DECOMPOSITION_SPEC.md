# Especificação Técnica: Decomposição do GuideRemittancePrintService (Fase 131)

## Diagnóstico

`GuideRemittancePrintService` (278 linhas) misturava o carregamento tenant-scoped da factura com a
composição das quatro vias A4 paisagem da Guia de Remessa, blocos de destinatário, transporte, peso bruto
da carga e assinaturas.

## Decisão

- `GuideRemittancePrintService`: fachada tenant-scoped que carrega a factura, valida a empresa activa e delega a emissão.
- `GuideRemittanceDocumentRenderer`: composição das 4 vias nominais em A4 paisagem, colunas com valor unitário e subtotal visíveis, resumo de carga (`LoadSummaryRenderer`), bloco de transporte/peso e assinaturas.
- Construtores legados de 3 e 4 argumentos preservados sem alteração de contrato.
