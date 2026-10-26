# SPEC-USER-MANAGEMENT-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Gestão de Utilizadores

## 1. Contexto & Diagnóstico
O painel `UserManagementPanel.java` (260 linhas) é responsável pela gestão corporativa de utilizadores, matriz de perfis/roles, definição e redefinição de senhas, e atribuição de PINs de Gestor para autorização rápida de operações sensíveis (como anulações de faturas e descontos de balcão).

### Problemas Identificados
1. **Acoplamento de Operações no Orquestrador:**
   - Métodos operacionais de prompt e mutação de credenciais (`openSetPinDialogForSelected`, `openResetPasswordForSelected`, `toggleStatusForSelected`, `openEditDialogForSelected`) estão misturados com a montagem de layout de UI, paginação, filtros e renderização de KPIs.
2. **Potencial de Modularidade:**
   - O orquestrador deve focar-se estritamente na composição visual canónica e lifecycle dos dados. A gestão de credenciais e ações deve ser delegada a um coordenador de operações com responsabilidade única.

## 2. Objectivos da Refatoração
1. **Redução de Linhas do Orquestrador:** Reduzir `UserManagementPanel.java` de 260 linhas para $\le 150$ linhas (-42%).
2. **Extração do Coordenador de Operações:**
   - `UserOperationsCoordinator.java` ($\le 130$ linhas): Coordenador responsável pela atribuição de PIN de gestor, redefinição de senhas, alternância de estado ativo/inativo, abertura do editor de utilizadores, povoamento de tabela e KPIs, e montagem dinâmica do Quick Peek.
3. **Erradicação Absoluta de `JOptionPane`:** Zero ocorrências mantidas em todos os ficheiros do pacote `mz.multicore.erp.gui.users`.
4. **Preservação Integral de Layout e Contratos:**
   - Construtor público `UserManagementPanel(UserApiClient userApiClient)`.
   - Método `refreshDataAsync()`.
   - `KpiCard.createGrid(4)` com 4 cartões de métricas.
   - `UIHelper.tableCardTop("Gestão de Utilizadores", filters, actionsMenu, refreshBtn, createBtn)`.
   - `UIHelper.ensureHeadersFit(usersTable)`.
   - `TableQuickPeekController.install(usersTable, card, ...)`.
   - `ClientTablePagination.install(usersTable)`.
   - Compatibilidade estrita com execução headless (`GUP-07` e `AA-01`).
