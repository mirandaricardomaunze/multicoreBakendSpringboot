# Especificação de Decomposição: EquipmentCatalogService (Fase 186)

## Objetivo
Decompor `EquipmentCatalogService.java` (170 linhas) numa fachada enxuta (`<= 75` linhas) que delega a mutação do catálogo de equipamentos/EPIs e as consultas paginadas para colaboradores coesos.

## Arquitetura Alvo
1. `EquipmentCatalogService.java` (`<= 75` linhas) — Fachada `@Service` preservando `mapToEquipmentDTO` e o construtor legado.
2. `EquipmentCatalogMutationService.java` (`<= 115` linhas) — Criação, atualização, validação de prazos/códigos e conversão DTO (`createEquipment`, `updateEquipment`, `mapToEquipmentDTO`).
3. `EquipmentCatalogQueryService.java` (`<= 65` linhas) — Obtenção individual, listagem de ativos e pesquisa paginada (`getEquipment`, `listActiveEquipments`, `searchEquipments`).
