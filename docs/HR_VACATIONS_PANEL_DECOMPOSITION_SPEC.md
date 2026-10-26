# SPEC-HR-VACATIONS-PANEL-DECOMPOSITION-001: Decomposição Modular de Pedidos de Férias

## 1. Visão Geral & Contexto Operacional

O painel `HRVacationsPanel.java` pertence ao módulo de Recursos Humanos (`HRPanel.java`), gerindo o ciclo de vida completo de pedidos de férias dos colaboradores: submissão de pedidos com datas de início e fim, controlo de dias úteis, cálculo do ano de referência, decisões hierárquicas (aprovação / rejeição com motivo) e integração com o subsídio de férias (`HRBonusActions`).

Actualmente com 201 linhas, o painel centraliza montagem de tabela, filtros por estado e período, Quick Peek lateral de consulta rápida, formulário modal de criação de pedidos e fluxo de aprovação e rejeição assíncrona.

Esta especificação estabelece a decomposição em dois colaboradores especializados de responsabilidade única.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `HRVacationsPanel.java` | 201 | $\le 110$ | Orquestrador de visualização, tabela, filtros e acionamento de ações |
| `HrVacationFormDialog.java` | 0 (Novo) | $\le 90$ | Diálogo modal ergonómico de registo de pedido de férias com `DateField`, `JSpinner` e validação |
| `HrVacationsViewCoordinator.java` | 0 (Novo) | $\le 90$ | Mapeamento de tabela, povoamento do Quick Peek, decisões de aprovação/rejeição e subsídio |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos:**
   - `HRVacationsPanel(HRPanel owner)` (package-private).
   - `JPanel buildPanel()`.
   - `void load()`.
   - `void openVacationAllowance()`.
2. **Layout Canónico Multicore & Testes Regressivos:**
   - `UIHelper.tableCardTop("Pedidos de Férias", vacBar,\n                exportBtn, decisionBtn, newBtn)`.
   - `TableQuickPeekController.install(table, card, ...)`.
   - `UIHelper.ensureHeadersFit(table)`.
   - `TableFilter.bar(vacSearch, TableFilter.label("Estado:"), vacEstado, TableFilter.label("Início:", "fas-calendar-alt"), vacPeriodo, refreshBtn)`.
   - Menus de ação: "Decisão & Subsídio" com "Aprovar", "Rejeitar", "Subsídio de Férias".
3. **Feedback Profissional e Zero `JOptionPane`:**
   - Total ausência de `JOptionPane`.
   - Notificações de sucesso/erro via `owner.showSuccess`, `owner.showNotice` e `owner.showActionError`.
