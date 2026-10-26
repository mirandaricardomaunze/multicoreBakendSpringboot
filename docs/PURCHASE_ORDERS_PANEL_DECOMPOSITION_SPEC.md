# SPEC-PURCHASE-ORDERS-PANEL-DECOMPOSITION-001: Decomposição Modular de PurchaseOrdersPanel (Fase 13)

## 1. Visão Geral e Contexto
O ficheiro `PurchaseOrdersPanel.java` possui atualmente **772 linhas**, acumulando num único componente:
1. Montagem do formulário de rascunho de encomenda e gestão da grelha de linhas editáveis com fator de embalagem multinível.
2. Construção e formatação da lista principal de encomendas, filtros canónicos, paginação e painel QuickPeek.
3. Orquestração do ciclo de vida das encomendas: criação, edição, recepção total e parcial, cancelamento e etiquetagem física.

Esta fase 13 decompõe o painel em colaboradores especializados de responsabilidade única, preservando rigorosamente todos os contratos, asserções de reflexão e homologações PHC existentes.

## 2. Metas de Redução e Limites Físicos Estritos
1. `PurchaseOrdersPanel.java`: $\le 380$ linhas (redução de $\sim 51\%$).
2. `PurchaseOrderEditorCoordinator.java`: $\le 280$ linhas (gestão da grelha de linhas, lookup, embalagem e validação do rascunho).
3. `PurchaseOrderOperationsCoordinator.java`: $\le 220$ linhas (ciclo de vida: emissão, actualização, recepção total/parcial, cancelamento e emissão de etiquetas).
4. Zero diálogos legados `JOptionPane`: substituição integral por `ModernMessageDialog`, `ToastManager` e `ModernFormDialog`.

## 3. Preservação Rigorosa de Contratos e Expectativas
- **`PurchasesUnifiedHarnessTest`**:
  - `PurchaseOrdersPanel.java` deve conter:
    - `TableFilter.bar(`
    - `UIHelper.tableCardTop(`
    - `TableQuickPeekController.install(poListTable`
    - `UIHelper.ensureHeadersFit(poListTable)`
  - Proibido conter `poFilters = new JPanel(new GridBagLayout())`.
  - Proibido conter reticências Unicode nos botões (`"Nova Encomenda…"`, `"Receber Parcial…"`).
- **`PurchaseReceivingLabelPrintingHarnessTest`**:
  - `PurchaseOrdersPanel.extractReceivedProducts(PurchaseOrderDTO order)` deve permanecer estático e acessível.
  - `PurchaseOrdersPanel.java` deve conter `"Imprimir Etiquetas"`, `"extractReceivedProducts"` e zero emojis.
- **`PurchaseOrderReceivingHarnessTest`**:
  - Referência expressa a `PurchaseOrderReceivingDialog`.
- **`PhcDocumentEditorHomologationHarnessTest`**:
  - Proibido conter `"editingLineIndex"`, `"editSelectedDraftLine"`, `"editLineButton"`, `"Actualizar item"`.
  - Deve conter `"stopPoCellEditing()"` e `"for (int row = 0; row < owner.poDraftLines.size(); row++)"`.
  - Deve conter `"packagesPerBox()"`, `"unitsPerPackage()"` e `"multiply(unitsPerBox)"`.
- **`DocumentPackageQuantityCoverageHarnessTest`**:
  - `new PackageQuantityEditor()` deve ocorrer exactamente 1 vez em `PurchaseOrdersPanel.java`.
- **`ProfessionalFeedbackHarnessTest`**:
  - Eliminar qualquer chamada a `JOptionPane` em `PurchaseOrdersPanel.java`. Ratchet down para 0 chamadas.

## 4. Plano de Execução
1. Escrever especificação e documento de harness (`HARNESS-PURCHASE-ORDERS-PANEL-DECOMPOSITION-001`).
2. Criar `PurchaseOrderOperationsCoordinator.java`.
3. Criar `PurchaseOrderEditorCoordinator.java`.
4. Refatorar `PurchaseOrdersPanel.java` mantendo conformidade com as asserções literais dos testes existentes.
5. Criar `PurchaseOrdersPanelDecompositionHarnessTest.java`.
6. Executar suíte de testes de compras, homologação PHC, feedback profissional, desktop thin context e multi-module architecture harness test.
7. Actualizar `tasks/current.md`.
