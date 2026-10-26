# SPEC-HR-EXPENSES-PANEL-DECOMPOSITION-001: Decomposição Modular de Notas de Despesas

## 1. Visão Geral & Contexto Operacional

O painel `HRExpensesPanel.java` integra a gestão de Recursos Humanos (`HRPanel.java`), permitindo o acompanhamento, listagem e submissão de notas de despesas e pedidos de reembolso de colaboradores corporativos por categoria (Alimentação, Deslocações, Alojamento, Outros).

Actualmente com 150 linhas, o painel centraliza montagem de tabela com renderizadores monetários e de status, pesquisa textual e filtros por estado de aprovação, Quick Peek lateral e diálogo modal de submissão de despesas.

Esta especificação define a extração dos componentes especializados de responsabilidade única.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `HRExpensesPanel.java` | 150 | $\le 75$ | Orquestrador de visualização, tabela, filtros e acionamento de ações |
| `HrExpenseClaimDialog.java` | 0 (Novo) | $\le 75$ | Diálogo modal ergonómico de registo e submissão de notas de despesa via `ModernFormDialog` |
| `HrExpensesViewCoordinator.java` | 0 (Novo) | $\le 65$ | Mapeamento de tabela, povoamento do Quick Peek e atualização de visão geral |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos:**
   - `HRExpensesPanel(HRPanel owner)` (package-private).
   - `public JPanel buildPanel()`.
   - `public void refresh()`.
2. **Layout Canónico Multicore & Testes Regressivos:**
   - `UIHelper.tableCardTop("Notas de Despesas", expBar, refreshBtn, exportBtn, submitBtn)`.
   - `TableQuickPeekController.install(table, card, ...)`.
   - `UIHelper.ensureHeadersFit(table)`.
   - `TableFilter.bar(expSearch, TableFilter.label("Estado:"), expEstado)`.
3. **Feedback Profissional e Zero `JOptionPane`:**
   - Total ausência de `JOptionPane`.
   - Avisos e notificações através de `owner.showNotice`, `owner.showSuccess` e `owner.showActionError`.
