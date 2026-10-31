# Especificação de Decomposição: InventoryCountService (Fase 171)

## Contexto e Problema
`InventoryCountService` (200 linhas) concentrava o ciclo de vida das sessões de contagem de inventário (início de sessão, gravação de contagens, aplicação de ajustes de stock com bloqueio pessimista e cancelamento) juntamente com as consultas e mapeamento de `InventoryCountDTO`.

## Solução Arquitectural
1. **`InventoryCountService` (Fachada — `<= 85` linhas)**:
   - Aplica os guards de segurança (`PermissionGuard`, `CurrentUserContext`) e delega para `InventoryCountLifecycleService` e `InventoryCountQueryService`.
   - Mantém o construtor compatível de 4 argumentos.
2. **`InventoryCountLifecycleService` (`@Component` — `<= 125` linhas)**:
   - Gere `createSession`, `saveCounts`, `applySession` e `cancelSession`.
3. **`InventoryCountQueryService` (`@Component` — `<= 85` linhas)**:
   - Gere `listSessions`, `getSession`, `loadForCompany`, `loadForCompanyForUpdate`, `toDetail` e `toSummary`.
