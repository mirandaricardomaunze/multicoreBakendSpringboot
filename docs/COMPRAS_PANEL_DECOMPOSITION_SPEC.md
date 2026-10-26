# Especificação Canónica: Decomposição de `ComprasPanel` (Fase 9)

**Identificador:** `SPEC-COMPRAS-PANEL-DECOMPOSITION-001`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  
**Data:** 2026-10-23  
**Status:** Em Implementação  

---

## 1. Contexto e Diagnóstico

O painel de compras da aplicação desktop (`ComprasPanel.java`) acumulou **854 linhas de código**, concentrando:
1. Construção e orquestração de abas corporativas (`Faturas de Compra (V/FT)`, `Reposição`, `Encomendas a Fornecedor`, `Contas a Pagar`, `Gestão de Fornecedores`, `Gestão de Negócios`, `Conta Corrente & Reconciliação`).
2. Gestão de metadados e carregamento assíncrono em cascata (fornecedores, armazéns, contas de tesouraria, produtos com fatores de embalagem).
3. Ciclo de vida operacional das faturas de compra (histórico com `TableFilter` e `TableQuickPeekController`, registo de compras com `ModernFormDialog`, validação de IVA de fornecedor, cálculo de subtotais e integração de linhas a crédito/pronto).
4. Acoplamento mútuo com painéis colaboradores existentes (`PurchaseOrdersPanel`, `PurchaseSuppliersPanel`, `PurchaseBusinessUnitsPanel`, `PurchaseReorderPanel`, `PurchasePayablesPanel`).

### Invariantes e Restrições Arquiteturais
- **Preservação de Campos e Construtores:** Os 2 construtores de `ComprasPanel` devem ser mantidos intactos. Campos acedidos pelos painéis vizinhos (`poSupplierCombo`, `poWarehouseCombo`, `poProductCombo`, `warehousesList`, `productsList`, etc.) devem permanecer acessíveis e compatíveis.
- **Invariante de Testes Existentes:**
  - `DocumentPackageQuantityCoverageHarnessTest`: Requer explicitamente `new PackageQuantityEditor()` em `ComprasPanel.java`.
  - `ProfessionalFeedbackHarnessTest`: Requer que validações de formulário (`UIHelper.markFieldInvalid(quantityField`, `priceField`, `purchaseVatField`) estejam presentes em `ComprasPanel.java`, sem nenhuma chamada a `JOptionPane`.
  - `TableActionPlacementHarnessTest`: Proíbe acções fora do topo do card (`historyCard.add(actionRow, BorderLayout.SOUTH)`).
- **Teto Físico de Linhas:**
  - `ComprasPanel.java`: Redução de 854 para $\le 450$ linhas.
  - Colaboradores novos: Cada um com $\le 190$ linhas.

---

## 2. Desenho da Decomposição Modular

### 2.1 Colaboradores Especializados

1. **`PurchaseMetadataCoordinator.java` ($\le 160$ linhas):**
   - Responsabilidade única: Carregamento assíncrono de entidades de apoio (fornecedores activos, armazéns da empresa, contas financeiras e produtos).
   - Propagação reactiva para combos de compras e combos partilhados de encomendas a fornecedor (`refreshPoCombos`).
   - Mapeamento seguro de identificadores (ex.: resolução de nome de armazém por ID).

2. **`PurchaseInvoiceOperations.java` ($\le 180$ linhas):**
   - Responsabilidade única: Ciclo de vida das faturas de compra e integração de stock.
   - Povoamento e atualização do modelo do histórico de compras (`loadPurchasesHistory`, `applyPurchasesHistory`).
   - Orquestração do diálogo de registo de compra (`openPurchaseFormDialog`), montagem do pedido (`buildPurchaseRequest`) e limpeza do rascunho.
   - Povoamento dinâmico do combo de negócios do histórico.

3. **`ComprasPanel.java` ($\le 450$ linhas):**
   - Papel: Orquestrador da interface de compras, integrador das abas corporativas, formulário de adição de linhas com `PackageQuantityEditor` e validação semântica com feedback inline.

---

## 3. Critérios de Sucesso e Validação

1. `ComprasPanel.java` $\le 450$ linhas.
2. `PurchaseMetadataCoordinator.java` $\le 160$ linhas.
3. `PurchaseInvoiceOperations.java` $\le 190$ linhas.
4. Zero chamadas a `JOptionPane`.
5. Testes existentes (`DesktopThinContextTest`, `TableActionPlacementHarnessTest`, `ProfessionalFeedbackHarnessTest`, `DocumentPackageQuantityCoverageHarnessTest`, `DesktopInitializationTest`, `AccountStatementPanelHarnessTest`) 100% verdes.
6. Novo teste `ComprasPanelDecompositionHarnessTest` aprovado com validação de limites físicos e integridade.
7. Teste de conformidade arquitetural `MultiModuleArchitectureHarnessTest` aprovado.
