# Especificação Canónica — Decomposição do OutstandingAccountsPanel

**ID:** `SPEC-OUTSTANDING-ACCOUNTS-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / Contas Correntes & Cobranças  
**Alvo:** `mz.multicore.erp.gui.commercial.OutstandingAccountsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui.commercial`)  

---

## 1. Objectivo & Diagnóstico

O `OutstandingAccountsPanel` gerencia as contas correntes de faturas com saldo em dívida, aging de recebíveis (antiguidade de saldos), liquidação tardia de faturas, e acionamento de ferramentas preventivas de cobrança (Radar Preventivo, Prestações e Devolução de Faturas).

### Diagnóstico de Complexidade
- **Acúmulo de Responsabilidades:** O painel acumulava montagem de tabela de 12 colunas com calibração de larguras, montagem de barra de filtros em cascata com sumário de aging, quick peek detalhado, coordenação de ações de cobrança e diálogo modal completo de liquidação com contas de tesouraria (`showPaymentDialog`).
- **Tamanho original:** 323 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

A decomposição segue o padrão corporativo canónico:

1. **`OutstandingAccountsPanel` (Orquestrador / View Principal):**
   - Construção do card principal `ModernPanel(16)` contendo barra de filtros unificada em linha única via `TableFilter.bar(...)`.
   - Construtor público canónico `OutstandingAccountsPanel(ComercialApiClient, FinanceApiClient, POSApiClient)`.
   - Preservação estrita do menu semântico `ActionMenuButton` contendo "Radar Preventivo", "Prestações", "Devolver Fatura".
   - Tabela de 12 colunas com `UIHelper.ensureHeadersFit(table)` e `ensureColumnFits` para rótulos longos.
   - Meta de linhas: $\le 205$ linhas.

2. **`OutstandingPaymentDialog` (`mz.multicore.erp.gui.commercial`):**
   - Diálogo modal ergonómico baseado em `ModernFormDialog` para recebimento e liquidação tardia de faturas em dívida.
   - Validação de saldo a receber via `MoneyField`, seleção de método e conta de tesouraria, submissão assíncrona ao `POSApiClient` (`registerLatePayment`) com `runWithProgress` e feedback via `ToastManager`.

3. **`OutstandingAccountsOperationsCoordinator` (`mz.multicore.erp.gui.commercial`):**
   - Coordenador de operações visuais para configuração dinâmica do Quick Peek (`populatePeek`).
   - Formatação estruturada em HTML do resumo de escalões de antiguidade (`formatAgingSummary`).
   - Despacho das ações de cobrança (`openRadar`, `openInstallments`, `openSalesReturn`, `receivePayment`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Contratos de Teste Existentes:**
  - `CommercialCrmUnifiedHarnessTest` (COMM-02): Presença obrigatória de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table` e ausência de `new JPanel(new GridBagLayout())`.
  - `TableActionPlacementHarnessTest`: Presença obrigatória de `UIHelper.tableCardTop(`.
  - `ProfessionalFeedbackHarnessTest`: Manutenção estrita de 0 chamadas a `JOptionPane`.
