# Especificação de Decomposição: ChartOfAccountsService (Fase 190)

## Objetivo
Decompor `ChartOfAccountsService.java` (166 linhas) numa fachada enxuta (`<= 85` linhas) que delega a sementeira PGC-NIRF e criação de contas para `ChartOfAccountsMutationService` e as consultas/validações de movimentação para `ChartOfAccountsQueryService`.

## Arquitetura Alvo
1. `ChartOfAccountsService.java` (`<= 85` linhas) — Fachada `@Service` preservando `PermissionGuard.requireManagerOrAdmin`, `toDTO`, `defaultNatureFor` e o construtor legado.
2. `ChartOfAccountsMutationService.java` (`<= 85` linhas) — Sementeira idempotente PGC-NIRF (`seedDefaultChart`) e criação de contas (`createAccount`).
3. `ChartOfAccountsQueryService.java` (`<= 65` linhas) — Listagem de contas, resolução de conta movimentável (`requirePostableAccount`) e verificação de plano (`hasChart`, `hasAccounts`).
