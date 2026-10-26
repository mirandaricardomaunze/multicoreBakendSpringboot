# SPEC-RECEIPTS-PANEL-DECOMPOSITION-001: Decomposição Modular de Recibos e Liquidações Comerciais

## 1. Visão Geral & Contexto Operacional

O painel `ReceiptsPanel.java` (no pacote `mz.multicore.erp.gui.commercial`) é responsável pela listagem, consulta rápida (Quick Peek), emissão de recibos para faturas de clientes e cancelamento/anulação formal de recibos emitidos no Multicore ERP.

Actualmente com 237 linhas de código, o painel acumula a orquestração do layout da tabela de recibos, a montagem e submissão do formulário modal de registo de recebimento (`openPayment`), o mapeamento de linhas/unidades de negócio e a anulação com justificação obrigatória.

Esta especificação estabelece a decomposição em componentes especializados com responsabilidade única:
- `ReceiptPaymentDialog.java`: Diálogo modal ergonómico baseado em `ModernFormDialog` para recebimento de faturas e emissão de recibos.
- `ReceiptsViewCoordinator.java`: Coordenador de apresentação para povoamento de linhas da tabela, filtros de negócio, configuração do Quick Peek e anulação de recibo selecionado.
- `ReceiptsPanel.java`: Painel orquestrador conciso focado na montagem visual canónica e contratos públicos.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `ReceiptsPanel.java` | 237 | $\le 115$ | Orquestrador de visualização, tabela e integração de filtros canónicos |
| `ReceiptPaymentDialog.java` | 0 (Novo) | $\le 85$ | Diálogo modal ergonómico para registo de recebimento e emissão de recibo |
| `ReceiptsViewCoordinator.java` | 0 (Novo) | $\le 85$ | Povoamento de tabela/unidades de negócio, Quick Peek e fluxo de anulação de recibo |

---

## 3. Contratos e Invariantes Preservados

1. **Construtor e Métodos Públicos:**
   - `public ReceiptsPanel(ComercialApiClient apiClient, Runnable invoicesRefresh)`
   - `public void openPayment(Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal, List<TreasuryAccountDTO> accounts)`
   - `public void refresh()`
2. **Layout Canónico e Filtros:**
   - `UIHelper.tableCardTop("Recibos Emitidos (Liquidações)", filters, refresh, cancel)`
   - `TableFilter.bar(...)` contendo pesquisa, combos de negócio, método, estado, período e botão peek.
   - `UIHelper.ensureHeadersFit(table)` e `TableQuickPeekController.install(table, card, ...)`
   - Card principal com `ModernPanel(16)`.
3. **Feedback Profissional e Zero `JOptionPane`:**
   - Uso exclusivo de `InlineFeedbackPanel`, `ToastManager` e `ModernFormDialog`. Total ausência de `JOptionPane`.
