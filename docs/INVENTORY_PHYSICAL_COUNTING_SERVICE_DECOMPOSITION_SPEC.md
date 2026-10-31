# Especificação Técnica: Decomposição do InventoryPhysicalCountingService (Fase 132)

## Diagnóstico

`InventoryPhysicalCountingService` (274 linhas) concentrava a abertura de sessões com fotografia de stock,
o registo de contagens (manual ou código de barras em modo incremental), o fecho com lançamento de ajustes
no inventário e as consultas/projecções DTO.

## Decisão

- `InventoryPhysicalCountingService`: fachada tenant-scoped que valida a empresa activa e delega para os colaboradores.
- `InventoryCountingLifecycleService`: criação da sessão, início da contagem, registo de quantidades contadas, fecho com movimentos `ADJUSTMENT` e cancelamento.
- `InventoryCountingQueryService`: consultas por sessão e empresa, cálculo do stock esperado por artigo e mapeamento `InventorySessionDTO`.
- Construtor legado de seis argumentos preservado sem alteração de contrato.
