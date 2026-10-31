# Especificação Técnica: Decomposição do FleetManifestService (Fase 137)

## Diagnóstico

`FleetManifestService` (256 linhas) misturava a validação e montagem de manifestos de carga (conversão de
guias de remessa em paragens, cálculo de caixas/embalagens e cobrança no destino) com as consultas,
transições de estado e mapeamento DTO.

## Decisão

- `FleetManifestService`: fachada tenant-scoped que resolve `companyId`, delega aos colaboradores e preserva `toDTO` e o construtor legado de 5 argumentos.
- `FleetManifestBuilder`: validação do pedido, associação de viatura, numeração `MC`, construção sequencial das paragens (`FleetManifestStop`), cálculo de caixas/embalagens e valores a cobrar.
- `FleetManifestQueryService`: listagem por empresa, detalhe com paragens, actualização de estado e projecção `FleetManifestDTO`.
