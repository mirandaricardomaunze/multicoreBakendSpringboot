# Harness de Verificação — PrintController (Fase 195)

## Cobertura do Harness (`PrintControllerDecompositionHarnessTest`)
1. **Delegação:** Verifica que `PrintController` delega a geração e resposta PDF para `PrintRequestDispatcher`.
2. **Limites físicos de linhas:**
   - `PrintController.java` <= 230 linhas (reduzido de 432 linhas)
   - `PrintRequestDispatcher.java` <= 250 linhas
