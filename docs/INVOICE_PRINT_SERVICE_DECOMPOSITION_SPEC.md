# Especificação Técnica: Decomposição do InvoicePrintService (Fase 125)

## Diagnóstico

`InvoicePrintService` concentrava carregamento e autorização, factura A4, factura térmica e rodapé
fiscal paginado. A primeira etapa separa os dois blocos independentes sem alterar regras fiscais.

## Decisão

- `InvoiceThermalRenderer`: composição exclusiva do talão térmico.
- `InvoicePageFooter`: rodapé fiscal e identificação da via/página A4.
- `InvoiceA4Renderer`: composição das quatro vias A4, cliente, linhas, totais e notas.
- `InvoicePrintService`: mantém apenas carregamento tenant-scoped e delegação.

O conteúdo, as quatro vias A4, totais, IVA e formato térmico permanecem inalterados. A composição A4
foi extraída na continuação da fase, depois de o primeiro gate intermédio ficar verde.
