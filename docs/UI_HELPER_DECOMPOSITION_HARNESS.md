# Harness de Verificação — UIHelper (Fase 197)

## Cobertura do Harness (`UIHelperDecompositionHarnessTest`)
1. **Delegação e Preservação de API:** Verifica que `UIHelper` preserva todas as constantes e métodos estáticos (`formatMzn`, `humanStatus`, `readableTextOn`, `iconForTitle`, `looksNumeric`, `activeLoads`, etc.) delegando para os 5 colaboradores coesos.
2. **Limites físicos de linhas:**
   - `UIHelper.java` <= 420 linhas (reduzido de 3.031 linhas)
   - `UiThemeAndWindowSupport.java` <= 380 linhas
   - `UiTabbedAndTableStyler.java` <= 620 linhas
   - `UiIconAndImageSupport.java` <= 290 linhas
   - `UiAsyncTaskSupport.java` <= 170 linhas
   - `UiFormControlAndFormatSupport.java` <= 520 linhas
