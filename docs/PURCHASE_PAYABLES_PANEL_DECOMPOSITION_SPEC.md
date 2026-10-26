# SPEC-PURCHASE-PAYABLES-PANEL-DECOMPOSITION-001: Decomposição Modular de Contas a Pagar a Fornecedores

## 1. Visão Geral & Contexto Operacional

O painel `PurchasePayablesPanel.java` integra o módulo de Compras (`ComprasPanel.java`) do Multicore ERP, sendo responsável pela gestão e acompanhamento de faturas de compras a crédito, contas a pagar a fornecedores, filtragem por linha de negócio e período temporal, bem como a liquidação financeira directa com afetação de contas de tesouraria.

Actualmente com 223 linhas, o painel acumula:
1. Montagem estrutural de tabela e filtros com `TableFilter.bar` e `ClientTablePagination`.
2. Lógica de apresentação de Quick Peek lateral com detalhe do fornecedor e cálculo de dívida.
3. Tratamento de agregação de linhas de negócio e recálculo assíncrono do rodapé de totais a pagar.
4. Construção modal e ciclo de liquidação de compras a crédito com `ModernFormDialog`, validação de saldo e invocação remota à API de compras.

Esta especificação define a decomposição estrita do painel em dois colaboradores especializados de responsabilidade única.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `PurchasePayablesPanel.java` | 223 | $\le 115$ | Orquestrador de visualização, tabela e acionamento de ações |
| `SupplierPaymentDialog.java` | 0 (Novo) | $\le 95$ | Diálogo modal ergonómico de registo de pagamento a fornecedor com seleção de conta e validação |
| `PurchasePayablesViewCoordinator.java` | 0 (Novo) | $\le 110$ | Apuramento de dados, povoamento do Quick Peek, atualização de filtros de negócio e cálculo de rodapé |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos Públicos:**
   - `PurchasePayablesPanel(ComprasPanel owner)` (package-private).
   - `public JPanel buildPanel()`.
   - `public void refresh()`.
2. **Layout Canónico Multicore:**
   - Card único `new ModernPanel(16)` com `UIHelper.tableCardTop("Contas a Pagar a Fornecedores", ...)`.
   - Filtros horizontais em linha única via `TableFilter.bar(...)`.
   - `UIHelper.ensureHeadersFit(owner.payablesTable)`.
   - `TableQuickPeekController.install(owner.payablesTable, card, ...)`.
   - `ClientTablePagination.install(owner.payablesTable)`.
3. **Feedback Profissional e Zero `JOptionPane`:**
   - 100% dos avisos e erros geridos via `owner.showPurchaseNotice`, `owner.showPurchaseSuccess` ou `ModernFormDialog`.
   - Total ausência de `JOptionPane`.
4. **Sanitização de Caracteres & Tipografia:**
   - Uso de hífens ASCII `-` em vez de travessões Unicode (`—`), prevenindo anomalias no Windows Java 2D.
