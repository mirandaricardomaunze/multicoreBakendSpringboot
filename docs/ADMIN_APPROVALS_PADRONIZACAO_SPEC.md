# Especificação Técnica de Padronização — Fase 8: Administração, Utilizadores & Central de Aprovações

## 1. Contexto e Diagnóstico

Esta fase fecha a padronização das áreas transversais de governança, controlo de acessos, crédito e relacionamento com o cliente:
- **Gestão de Utilizadores:** Controlo de acessos RBAC, perfis de segurança, PINs de autorização de gestor.
- **Central de Aprovações:** Gestão de pedidos pendentes (descontos, créditos, anulações) e histórico de decisões tomadas.
- **Centro de Risco de Crédito & Cobrança:** Matriz de aging por faixas de mora, limites concedidos vs. utilizados, bloqueio de crédito.
- **CRM & Assistência ao Cliente:** Gestão de pedidos de assistência (tickets), técnicos atribuídos e histórico de contactos.

### Diagnóstico de Desvios
1. **Ausência de Calibração `ensureHeadersFit`:** As tabelas destas áreas não chamavam `UIHelper.ensureHeadersFit(table)`, podendo truncar identificadores, valores ou perfis de utilizador.
2. **Ausência de Gaveta «Quick Peek» Silencioso:** Consulta rápida de utilizadores, pedidos de aprovação, posições de risco e tickets necessitava abrir janelas pesadas ou diálogos de decisão.

---

## 2. Requisitos Canónicos da Fase 8

1. **Card Canónico:** Manutenção de `ModernPanel(16)` via `UIHelper.tableCardTop(...)`.
2. **Calibração de Colunas:** Aplicação obrigatória de `UIHelper.ensureHeadersFit(table)` em todos os painéis.
3. **Quick Peek Silencioso com Tecla `Espaço`:**
   - **Utilizadores:** Username, Nome Completo, Perfil, Estado, PIN de Gestor.
   - **Aprovações:** Documento, Submissor, Valor, Perfil Requerido, Justificação.
   - **Risco de Crédito:** Cliente, NUIT, Limite, Dívida Total, Disponível, Total em Mora, Risco, Situação.
   - **CRM (Tickets):** ID, Cliente, Assunto, Prioridade, Técnico Atribuído, Estado.
4. **Preservação de Integridade:** Testes `AdminApprovalsUnifiedHarnessTest`, `DesktopThinContextTest` e `UiPanelDecompositionTest` 100% verdes.

---

## 3. Matriz de Ficheiros

| Painel | Ficheiro | Ações Canónicas |
| :--- | :--- | :--- |
| **Utilizadores** | `users/UserManagementPanel.java` | `ensureHeadersFit` e Quick Peek em `usersTable`. |
| **Aprovações** | `ApprovalsPanel.java` | `ensureHeadersFit` e Quick Peek em `pendingTable` e `historyTable`. |
| **Risco de Crédito** | `CreditRiskPanel.java` | `ensureHeadersFit` e Quick Peek em `table`. |
| **CRM & Assistência** | `CRMPanel.java` | `ensureHeadersFit` e Quick Peek em `ticketsTable` e `contactsTable`. |
