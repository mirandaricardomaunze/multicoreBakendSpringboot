# Harness de Testes: Decomposição do PurchaseService (Fase 112)

## 1. Objetivos do Harness
Garantir que a refatoração do `PurchaseService` mantém:
1. Limites físicos de linhas rigorosos:
   - `PurchaseService.java` $\le 175$ linhas.
   - `SupplierManagementService.java` $\le 150$ linhas.
   - `DirectPurchaseService.java` $\le 240$ linhas.
   - `SupplierPaymentService.java` $\le 130$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `PurchaseServiceTest` (9/9 testes) e `MulticoreServicesTest`.

## 2. Testes Automatizados Executados
- `PurchaseServiceDecompositionHarnessTest`:
  - `orchestratorUnderLimit`: Valida que `PurchaseService.java` não ultrapassa 175 linhas.
  - `supplierManagementServiceUnderLimit`: Valida que `SupplierManagementService.java` não ultrapassa 150 linhas.
  - `directPurchaseServiceUnderLimit`: Valida que `DirectPurchaseService.java` não ultrapassa 240 linhas.
  - `supplierPaymentServiceUnderLimit`: Valida que `SupplierPaymentService.java` não ultrapassa 130 linhas.
- `PurchaseServiceTest`: Cobertura completa de fornecedores (atualização, desativação, pesquisa, validação NUIT e guarda de empresa), compras com liquidação e eventos.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.
