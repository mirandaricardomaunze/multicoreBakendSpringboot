# Especificação Canónica — Decomposição do ClientesPanel

**ID:** `SPEC-CLIENTES-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / CRM / Gestão de Clientes  
**Alvo:** `mz.multicore.erp.gui.ClientesPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivo & Diagnóstico

O `ClientesPanel` é o painel central da área de clientes do ERP, integrando o diretório de clientes cadastrados, matriz de aging/risco de crédito (`CreditRiskPanel`) e conta corrente reconciliada (`CustomerStatementPanel`).

### Diagnóstico de Complexidade
- **Monolitismo de responsabilidades:** O painel acumulava montagem de abas, estruturação de tabela com paginação e quick peek, diálogo modal completo de edição e cadastro de cliente (54 linhas com campos, filtros de dígitos e validação semântica), confirmação e execução de deleção de cliente com diálogo legado `JOptionPane`.
- **Diálogo legado `JOptionPane`:** Utilização de `JOptionPane.showConfirmDialog` para confirmar a remoção de cliente violando as convenções corporativas de eliminação de legacy dialogs.
- **Tamanho original:** 335 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

A decomposição segue o padrão corporativo canónico com separação estrita de responsabilidades:

1. **`ClientesPanel` (Orquestrador / View Principal):**
   - Gestão de ciclo de vida das abas (`JTabbedPane`), selecção de abas especializadas (`selectCreditRiskTab`, `selectStatementTab`).
   - Construção do card principal `ModernPanel(16)` contendo filtros canónicos em linha única (`TableFilter.bar`), tabela estritamente estilizada com paginação (`ClientTablePagination.install`) e controlador de visualização rápida (`TableQuickPeekController.install`).
   - Delegação de operações de CRUD e visualização de ficha ao coordenador especializado.
   - Meta de linhas: $\le 245$ linhas.

2. **`ClientFormDialog` (`mz.multicore.erp.gui.commercial`):**
   - Diálogo modal ergonómico e corporativo baseado em `ModernFormDialog` para criação e edição de clientes.
   - Encapsulamento completo de campos (`nameField`, `taxIdField`, `emailField`, `addressField`, `termsField`, `creditField`), filtro de 9 dígitos para NUIT/NIF, e validações atómicas (`validateMinLength(3, "Nome")`, `validateNuit()`, `validateEmail()`).
   - Submissão assíncrona ao `ComercialApiClient` (`createClient` / `updateClient`) com fecho atómico e notificação visual.

3. **`ClientesOperationsCoordinator` (`mz.multicore.erp.gui.commercial`):**
   - Coordenação de abertura de diálogo de novo cliente ou edição.
   - Exclusão com confirmação segura e sem bloqueio via `UIHelper.confirm`, eliminando `JOptionPane` residual.
   - Invocação e acoplamento com a ficha executiva `CustomerDetailDialog`.
   - Utilitários de formatação de prazos e limites de crédito em conformidade com o padrão moçambicano (`pt-MZ`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Construtores Canónicos:** Preservação estrita dos construtores de 3 e 4 parâmetros:
  - `ClientesPanel(ComercialApiClient, PrintApiClient, CreditRiskApiClient)`
  - `ClientesPanel(ComercialApiClient, PrintApiClient, CreditRiskApiClient, AccountStatementApiClient)`
- **Contratos de Teste Existentes:**
  - `AccountStatementPanelHarnessTest`: Preservação de `JTabbedPane` com 3 abas quando `statementApiClient` for fornecido.
  - `RadarAlertasProactivoHarnessTest`: Métodos públicos `selectCreditRiskTab()` e `selectStatementTab()`.
  - `AdminApprovalsUnifiedHarnessTest` (AA-03): Presença literal de `UIHelper.tableCardTop("Gestão de Clientes"`, `ClientTablePagination.install(table)`, `UIHelper.ensureHeadersFit(table)` e `TableQuickPeekController.install(table`.
  - `CommercialCrmUnifiedHarnessTest` (COMM-01): `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table` e ausência de `new JPanel(new GridBagLayout())`.
  - `TableExportFilterTest`: Presença literal do botão `"Exportar PDF"` com ícone `"fas-file-pdf"` e chamada `TableExportAction.export(this, printApiClient, table, "Clientes", "clientes")`.
  - `TableActionPlacementHarnessTest`: Presença de `UIHelper.tableCardTop(`.
  - `ProfessionalFeedbackHarnessTest`: Redução a 0 chamadas directas a `JOptionPane` em `ClientesPanel.java`.
