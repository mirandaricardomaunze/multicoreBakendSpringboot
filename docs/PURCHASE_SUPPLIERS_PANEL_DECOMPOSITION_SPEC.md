# SPEC-PURCHASE-SUPPLIERS-PANEL-DECOMPOSITION-001: Decomposição Modular de Fornecedores Cadastrados

## 1. Visão Geral & Contexto Operacional

O painel `PurchaseSuppliersPanel.java` pertence ao módulo de Compras (`ComprasPanel.java`) do Multicore ERP, assegurando a manutenção cadastral e o ciclo de vida dos fornecedores da empresa: listagem de entidades fornecedoras, pesquisa por nome/NUIT, filtros por estado ativo/inativo, visualização de contactos via Quick Peek, criação/edição via diálogo modal e ativação/desativação operacional.

Actualmente com 181 linhas, o painel agrega layout de tabela, filtros, controlador de Quick Peek, modal de criação/edição e alternância de estado ativo.

Esta especificação define a extração dos colaboradores especializados de responsabilidade única.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `PurchaseSuppliersPanel.java` | 181 | $\le 95$ | Orquestrador de visualização, tabela, filtros e menus de ações |
| `SupplierFormDialog.java` | 0 (Novo) | $\le 75$ | Diálogo modal ergonómico de registo e edição de fornecedor com `ModernFormDialog` e gravação assíncrona |
| `PurchaseSuppliersViewCoordinator.java` | 0 (Novo) | $\le 80$ | Povoamento de Quick Peek com dados de contacto, seleção de fornecedor e alternância assíncrona de estado ativo |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos:**
   - `PurchaseSuppliersPanel(ComprasPanel owner)` (package-private).
   - `public JPanel buildPanel()`.
2. **Layout Canónico Multicore:**
   - Card único `new ModernPanel(16)` com `UIHelper.tableCardTop("Fornecedores Cadastrados", ...)`.
   - Filtros horizontais em linha única via `TableFilter.bar(...)`.
   - `UIHelper.ensureHeadersFit(owner.suppliersTable)`.
   - `TableQuickPeekController.install(owner.suppliersTable, listCard, ...)`.
   - `ClientTablePagination.install(owner.suppliersTable)`.
3. **Feedback Profissional e Zero `JOptionPane`:**
   - Total ausência de `JOptionPane`.
   - Avisos e sucessos via `owner.showPurchaseNotice`, `owner.showPurchaseSuccess` e `owner.showPurchaseError`.
4. **Sanitização de Caracteres & Tipografia:**
   - Uso de hífens ASCII `-` em vez de travessões Unicode (`—`), prevenindo anomalias no Windows Java 2D.
