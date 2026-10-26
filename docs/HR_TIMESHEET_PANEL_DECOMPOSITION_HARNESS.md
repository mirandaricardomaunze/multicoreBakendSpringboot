# Plano de Testes & Validação: Decomposição de HRTimeSheetPanel

**Identificador:** `HARNESS-HR-TIMESHEET-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/HR_TIMESHEET_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração de `HRTimeSheetPanel` reduz a sua extensão para $\le 165$ linhas, elimina 100% dos diálogos legados `JOptionPane` (com avanço no `ProfessionalFeedbackHarnessTest`) e preserva todos os contratos de layout e chamadas assíncronas esperados por `TableActionPlacementHarnessTest`, `DesktopInitializationTest` e `DesktopThinContextTest`.

---

## 2. Casos de Teste do Harness (`HRTimeSheetPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `HRTimeSheetPanel.java` possui $\le 165$ linhas.
2. **`testCollaboratorLineCounts`:**
   - Valida que `HrTimeEntryDialog.java` possui $\le 90$ linhas.
   - Valida que `HrOvertimeRatesDialog.java` possui $\le 110$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `HRTimeSheetPanel.java` contém:
     - `UIHelper.tableCardTop("Folha de Ponto", filters,\n                moreBtn, closeBtn, entryBtn)`
     - `new ModernPanel(16)`
     - `TableFilter.bar(search, refreshBtn)`
     - `UIHelper.ensureHeadersFit(table)`
     - `TableQuickPeekController.install(table, card`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros (`HRTimeSheetPanel.java`, `HrTimeEntryDialog.java`, `HrOvertimeRatesDialog.java`) contém chamadas a `JOptionPane`.
5. **`testPublicApiPreserved`:**
   - Valida a existência do construtor package-private `HRTimeSheetPanel(HRPanel owner)`.
   - Valida a existência dos métodos package-private `buildPanel()` e `load()`.
