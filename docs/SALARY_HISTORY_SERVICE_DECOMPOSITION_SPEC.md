# Especificação de Decomposição: SalaryHistoryService (Fase 184)

## Objetivo
Decompor `SalaryHistoryService.java` (173 linhas) numa fachada enxuta (`<= 75` linhas) que expõe as consultas da série salarial e delega a mutação, validação e atualização da ficha do colaborador para `SalaryChangeMutationService`.

## Arquitetura Alvo
1. `SalaryHistoryService.java` (`<= 75` linhas) — Fachada `@Service` preservando `PermissionGuard.requireManagerOrAdmin`, consultas `getHistory` / `salaryOn` e o construtor legado.
2. `SalaryChangeMutationService.java` (`<= 125` linhas) — Registo de alterações salariais (`registerChange`, `record`), reflexo na ficha do colaborador quando vigente hoje e projecção `SalaryChangeDTO`.
