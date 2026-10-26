# SPEC-CONFIG-SUPPORT-PANEL-DECOMPOSITION-001: Decomposição Modular de Suporte e Assistência

## 1. Visão Geral & Contexto Operacional

A interface `ConfigSupportPanel.java` (no módulo desktop) é responsável pela consulta e conversação de pedidos de assistência e suporte técnico à plataforma Multicore ERP.

Actualmente com 189 linhas de código, a classe acumula a construção da tabela de pedidos com filtros e Quick Peek, o formulário modal de abertura de tickets de assistência com validação e o diálogo de visualização da thread de mensagens e envio de respostas.

Esta especificação define a extração dos diálogos de suporte para um colaborador especializado:
- `ConfigSupportTicketDialog.java`: Diálogos modais ergonómicos baseados em `ModernFormDialog` para abertura de novo ticket e consulta/resposta de conversação.
- `ConfigSupportPanel.java`: Painel orquestrador conciso focado na tabela de tickets, filtros canónicos e Quick Peek.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `ConfigSupportPanel.java` | 189 | $\le 105$ | Orquestrador de visualização, tabela e integração de filtros e Quick Peek |
| `ConfigSupportTicketDialog.java` | 0 (Novo) | $\le 90$ | Diálogos modais de abertura de ticket e conversação/resposta de suporte |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos:**
   - Package-private `ConfigSupportPanel(ConfigPanel owner)`.
   - `public JPanel buildPanel()`.
   - `public void refresh()`.
2. **Layout Canónico e UX:**
   - Card principal com `ModernPanel(16)`.
   - Tabela de suporte com `owner.supportTable` e `owner.supportModel`.
   - `UIHelper.tableCardTop("Pedidos de Assistência à Plataforma", supBar, refreshBtn, viewBtn, newBtn)`.
   - `TableFilter.bar(supSearch)` e `TableQuickPeekController.install(owner.supportTable, listCard, ...)`.
3. **Segurança e Zero `JOptionPane`:**
   - Verificação de perfil gestor/administrador (`SignedInUser.isManagerOrAdmin()`).
   - Total ausência de `JOptionPane`, utilizando `ModernFormDialog` e `ToastManager`.
