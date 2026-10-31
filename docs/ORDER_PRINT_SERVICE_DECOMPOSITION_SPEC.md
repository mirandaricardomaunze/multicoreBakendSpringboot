# Especificação Técnica: Decomposição do OrderPrintService (Fase 128)

## Diagnóstico

`OrderPrintService` concentrava persistência, autorização multi-tenant e toda a composição visual da
encomenda A4: cabeçalho, cliente, metadados, linhas, peso, totais, condições, notas e assinaturas.

## Decisão

- `OrderPrintService`: fachada tenant-scoped que carrega a encomenda e delega a emissão.
- `OrderA4Renderer`: composição exclusiva do documento A4, sem acesso a repositórios ou tenant.
- O construtor legado `(OrderRepository, LineRowMapper, DocumentConfigService)` permanece disponível.

O conteúdo documental, colunas, origem da cotação, cálculo de peso e desconto, rodapé configurável e
assinaturas permanecem inalterados. Não há mudança de contrato REST nem de regra comercial.
