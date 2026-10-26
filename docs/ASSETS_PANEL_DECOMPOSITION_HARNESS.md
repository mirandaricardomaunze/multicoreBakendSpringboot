# Plano de Testes & Validação: Decomposição de AssetsPanel

**Identificador:** `HARNESS-ASSETS-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/ASSETS_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Validar que a decomposição do painel `AssetsPanel` mantém conformidade total com todos os requisitos visuais, ausência de regressões, preservação de campos package-private para testes de reflexão e respeito pelos tetos de linhas.

---

## 2. Casos de Teste do Harness (`AssetsPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `AssetsPanel.java` possui $\le 250$ linhas.
2. **`testCollaboratorsLineCounts`:**
   - Valida que `AssetFormDialog.java` possui $\le 125$ linhas.
   - Valida que `AssetDepreciationCoordinator.java` possui $\le 100$ linhas.
3. **`testPackagePrivateFieldsAndMethodsPreserved`:**
   - Valida a presença dos campos `table` e `feedback` com visibilidade de pacote em `AssetsPanel.java`.
   - Valida a presença do método `showAssetDetails()` com visibilidade de pacote em `AssetsPanel.java`.
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros criados/modificados contém chamadas a `JOptionPane`.
5. **`testConstructorsPreserved`:**
   - Valida que os construtores de 1 argumento e de 2 argumentos de `AssetsPanel` são preservados.
