# HARNESS-PURCHASE-ORDERS-PANEL-DECOMPOSITION-001: Harness de Validação da Decomposição de PurchaseOrdersPanel (Fase 13)

## 1. Objectivos do Teste de Harness
O teste automatizado `PurchaseOrdersPanelDecompositionHarnessTest` verifica:
1. **DECOMP-01: Limites Físicos Estritos de Linhas de Código**:
   - `PurchaseOrdersPanel.java` $\le 380$ linhas.
   - `PurchaseOrderEditorCoordinator.java` $\le 280$ linhas.
   - `PurchaseOrderOperationsCoordinator.java` $\le 220$ linhas.
2. **DECOMP-02: Zero Diálogos Legados `JOptionPane`**:
   - Nenhum dos ficheiros do domínio de encomendas pode invocar `JOptionPane.show...`.
3. **DECOMP-03: Homologação e Asserções Contratuais Canónicas**:
   - Presença dos métodos públicos estáticos: `PurchaseOrdersPanel.extractReceivedProducts(PurchaseOrderDTO)`.
   - Presença dos literais exigidos por `PurchasesUnifiedHarnessTest`, `PhcDocumentEditorHomologationHarnessTest` e `DocumentPackageQuantityCoverageHarnessTest`.
4. **DECOMP-04: Estrutura da Tabela e Ações**:
   - Verificação das colunas da tabela de encomendas e ações no menu semântico.
5. **DECOMP-05: Ciclo de Vida e Operações Seguras**:
   - Invocação assíncrona de `refresh()`, `openPurchaseOrderFormDialog()` e `savePurchaseOrder()` com mocks.
