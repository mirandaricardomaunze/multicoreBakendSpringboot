# Harness de Testes - ProductionService Decomposição (Fase 115)

## Casos de Teste Estruturais e Funcionais
1. **Verificação de Limites Físicos (Linhas de Código):**
   - `ProductionService.java`: $\le 110$ linhas.
   - `BillOfMaterialsService.java`: $\le 165$ linhas.
   - `ProductionOrderLifecycleService.java`: $\le 245$ linhas.
   - `ProductionOrderMapper.java`: $\le 60$ linhas.
2. **Delegação e Preservação de Assinatura:**
   - Garantir delegação para `BillOfMaterialsService.createBom`, `listBoms`, `getBom`.
   - Garantir delegação para `ProductionOrderLifecycleService.createProductionOrder`, `startProduction`, `completeProduction`, `cancelProduction`.
3. **Casos Funcionais:**
   - Criação de BOM e cálculo de custos estimados.
   - Criação de ordem de produção com cálculo proporcional e perdas.
   - Transição de estado de ordens (PLANNED -> IN_PROGRESS -> COMPLETED / CANCELLED).
   - Movimentação de stocks na conclusão e publicação de `ProductionCompletedEvent`.
4. **Verificação Arquitetural Global:**
   - `MultiModuleArchitectureHarnessTest`: 100% aprovado.
   - `DesktopThinContextTest`: 100% aprovado.
