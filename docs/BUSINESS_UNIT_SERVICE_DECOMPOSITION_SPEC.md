# Especificação de Decomposição: BusinessUnitService (Fase 187)

## Objetivo
Decompor `BusinessUnitService.java` (169 linhas) numa fachada enxuta (`<= 80` linhas) que delega a mutação do ciclo de vida das unidades de negócio e as consultas/políticas de restrição comercial para colaboradores coesos.

## Arquitetura Alvo
1. `BusinessUnitService.java` (`<= 80` linhas) — Fachada `@Service` preservando `CurrentUserContext` e o construtor legado de 5 argumentos.
2. `BusinessUnitMutationService.java` (`<= 105` linhas) — Criação, atualização, alternância de estado ativo/inativo e auditoria (`create`, `update`, `toggleActive`).
3. `BusinessUnitQueryAndPolicyService.java` (`<= 75` linhas) — Consultas por empresa/ID, validação de restrição de produto por unidade de negócio (`validateProductRestriction`) e conversão DTO (`toDTO`).
