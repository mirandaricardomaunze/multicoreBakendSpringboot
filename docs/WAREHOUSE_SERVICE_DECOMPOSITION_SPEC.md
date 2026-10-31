# Especificação de Decomposição: WarehouseService (Fase 169)

## Contexto e Problema
`WarehouseService` (205 linhas) concentrava as consultas de armazéns activos, armazéns de venda e estado de tranca de contagem cega (`stockCountLocked`) juntamente com a criação, edição, activação/desactivação de armazéns e tranca/destranca de stock.

## Solução Arquitectural
1. **`WarehouseService` (Fachada — `<= 125` linhas)**:
   - Aplica os guards de segurança (`PermissionGuard`, `CurrentUserContext`) e delega para `WarehouseLifecycleService` e `WarehouseQueryService`.
   - Mantém `toDTO` e o construtor compatível de 3 argumentos.
2. **`WarehouseLifecycleService` (`@Component` — `<= 125` linhas)**:
   - Gere `createWarehouse` (todas as sobrecargas), `updateWarehouse`, `setWarehouseActive` e `setStockCountLocked`.
3. **`WarehouseQueryService` (`@Component` — `<= 95` linhas)**:
   - Gere `getWarehousesByCompany`, `getSalesWarehousesByCompany`, `getAllWarehousesByCompany`, `resolveSalesWarehouses`, `loadWarehouseForActiveCompany`, `requireWarehouseCompany`, `isStockCountLocked`, `findWarehousesByCompany` e `toDTO`.
