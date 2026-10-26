# Plano de Testes & Validação: Decomposição de CRMPanel

**Identificador:** `HARNESS-CRM-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/CRM_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração do painel `CRMPanel` reduz o seu número de linhas sem quebrar nenhum dos contratos literais esperados por `AdminApprovalsUnifiedHarnessTest`, `HrCrmUiErgonomicsHarnessTest`, `TableActionPlacementHarnessTest` ou `DesktopInitializationTest`.

---

## 2. Casos de Teste do Harness (`CRMPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `CRMPanel.java` possui $\le 205$ linhas.
2. **`testCollaboratorLineCount`:**
   - Valida que `CrmViewCoordinator.java` possui $\le 110$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `CRMPanel.java` contém:
     - `UIHelper.ensureHeadersFit(ticketsTable)`
     - `TableQuickPeekController.install(ticketsTable`
     - `UIHelper.tableCardTop("Pedidos dos Clientes"`
     - `UIHelper.tableCardTop("Folhas de Obra Registadas", wsBar,\n                moreBtn, billBtn, newWsBtn)`
     - `Ações da Folha`
     - `new ModernPanel(16)`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros contém chamadas a `JOptionPane`.
5. **`testPublicApiPreserved`:**
   - Valida a existência do construtor público `CRMPanel(CRMApiClient, ComercialApiClient)`.
   - Valida a existência dos métodos públicos `refreshData()` e `onPanelSelected()`.
