# Harness de Verificação — DataLoader (Fase 196)

## Cobertura do Harness (`DataLoaderDecompositionHarnessTest`)
1. **Delegação:** Verifica que `DataLoader.run(...)` delega a sementeira de referência e de demonstração para `ReferenceAndAdminDataSeeder` e `DemoEnterpriseDataSeeder`.
2. **Limites físicos de linhas:**
   - `DataLoader.java` <= 115 linhas (de 583 linhas)
   - `ReferenceAndAdminDataSeeder.java` <= 150 linhas
   - `DemoEnterpriseDataSeeder.java` <= 340 linhas
