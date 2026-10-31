# Harness de Testes - StockMovementService Decomposição (Fase 114)

## Casos de Teste Estruturais e Funcionais
1. **Verificação de Limites Físicos (Linhas de Código):**
   - `StockMovementService.java`: $\le 140$ linhas.
   - `StockMovementRegistrationService.java`: $\le 230$ linhas.
   - `StockAdjustmentCoordinator.java`: $\le 165$ linhas.
   - `StockMovementQueryService.java`: $\le 95$ linhas.
2. **Delegação e Preservação de Assinatura:**
   - Garantir delegação para `StockMovementRegistrationService.registerMovement`.
   - Garantir delegação para `StockAdjustmentCoordinator.adjustStock`.
   - Garantir delegação para `StockMovementQueryService.findMovementsByCompany`.
3. **Casos Funcionais:**
   - Sucesso em `StockMovementPaginationHarnessTest`.
   - Sucesso em `InventoryServiceTest`.
4. **Verificação Arquitetural Global:**
   - `MultiModuleArchitectureHarnessTest`: 100% aprovado.
   - `DesktopThinContextTest`: 100% aprovado.
