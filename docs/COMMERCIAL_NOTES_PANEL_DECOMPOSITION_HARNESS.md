# Plano de Testes & Validação: Decomposição de CommercialNotesPanel

**Identificador:** `HARNESS-COMMERCIAL-NOTES-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/COMMERCIAL_NOTES_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração do painel `CommercialNotesPanel` reduz a sua complexidade ciclomática e número de linhas mantendo total estabilidade de funcionamento, compatibilidade de APIs e aderência aos testes existentes.

---

## 2. Casos de Teste do Harness (`CommercialNotesPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `CommercialNotesPanel.java` possui $\le 370$ linhas.
2. **`testCollaboratorsLineCounts`:**
   - Valida que `DebitNoteFormDialog.java` possui $\le 95$ linhas.
   - Valida que `CommercialNotesOperationsCoordinator.java` possui $\le 100$ linhas.
3. **`testCanonicalStringsPreserved`:**
   - Valida que `CommercialNotesPanel.java` mantém:
     - `createRefreshButton(this::loadCredits)`
     - `createRefreshButton(this::loadDebits)`
     - `ActionMenuButton`
     - `"Aprovar"`
     - `"Rejeitar"`
     - `return column == 6`
     - `new CreateCreditNoteLineRequest`
     - `TableFilter.bar(`
     - `UIHelper.tableCardTop(`
     - `UIHelper.ensureHeadersFit(creditTable)`
     - `UIHelper.ensureHeadersFit(debitTable)`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros criados/modificados (`CommercialNotesPanel.java`, `DebitNoteFormDialog.java`, `CommercialNotesOperationsCoordinator.java`) contém chamadas a `JOptionPane`.
5. **`testPublicApiAndConstructorsPreserved`:**
   - Valida que `CommercialNotesPanel` preserva os dois construtores canónicos:
     - `(ComercialApiClient, CreditNoteApiClient, DebitNoteApiClient, Supplier<List<WarehouseDTO>>)`
     - `(ComercialApiClient, CreditNoteApiClient, DebitNoteApiClient, Supplier<List<WarehouseDTO>>, PrintApiClient)`
   - Valida os métodos públicos `creditTab()`, `debitTab()` e `refresh()`.
