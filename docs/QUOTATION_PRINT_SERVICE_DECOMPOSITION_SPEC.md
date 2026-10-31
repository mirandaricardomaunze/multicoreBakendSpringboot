# Decomposição do QuotationPrintService — Fase 126

O formato térmico foi isolado em `QuotationThermalRenderer` e o documento A4 em
`QuotationA4Renderer`. A fachada mantém apenas carregamento tenant-scoped e delegação. Validade,
expiração, descontos, IVA, aviso não fiscal, `DocumentConfigService` e rodapé configurável permanecem
inalterados. O construtor anterior foi preservado.
