# Especificação de Decomposição Modular: Notas Comerciais (CommercialNotesPanel)

**Identificador:** `SPEC-COMMERCIAL-NOTES-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / CRM / Faturação & Notas de Crédito e Débito  
**Alvo:** `mz.multicore.erp.gui.commercial.CommercialNotesPanel.java` (504 linhas)  
**Meta de Redução:** $\le 370$ linhas no orquestrador principal ($\approx 30\%$ de redução, preservando contratos PHC)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `CommercialNotesPanel.java` é responsável pela consulta, filtragem, aprovação, rejeição, impressão fiscal e emissão modal de Notas de Crédito e Notas de Débito associadas a faturas previamente emitidas no Multicore ERP.

Embora o painel já utilize `TableFilter.bar`, `UIHelper.tableCardTop` e elimine chamadas diretas a `JOptionPane`, ele concentrava num único ficheiro monolítico de 504 linhas:
1. **Emissão Modal de Notas de Débito:** Formulário modal completo de criação com tabela de linhas, adição/remoção dinâmica de linhas, validações de IVA e envio de pedido à API.
2. **Ciclo Operacional & Reimpressão Fiscal:** Gestão de estados (Pendente, Aprovado, Rejeitado), diálogos de confirmação com `ModernMessageDialog`, recolha de justificações obrigatórias de recusa via `UIHelper.promptRequiredText`, e consulta assíncrona de vias impressas com `SensitivePrintCoordinator` / `PrintApiClient`.
3. **Emissão de Notas de Crédito:** Formulário de crédito sobre fatura com tabela inline de quantidades vendidas, já devolvidas e a devolver.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve preservar rigorosamente todas as asserções existentes no ecossistema de testes do Multicore ERP:
1. **`CommercialMultiuserRefreshHarnessTest`:**
   - O orquestrador `CommercialNotesPanel.java` deve conter os literais canónicos:
     - `createRefreshButton(this::loadCredits)`
     - `createRefreshButton(this::loadDebits)`
2. **`StockCommercialUiHarnessTest`:**
   - O orquestrador deve manter a criação de botões de menu semântico:
     - `ActionMenuButton`, `"Aprovar"`, `"Rejeitar"`
3. **`PhcInlineDocumentGridHarnessTest`:**
   - O orquestrador `CommercialNotesPanel.java` deve reter a edição inline da grelha de crédito:
     - `return column == 6`
     - `new CreateCreditNoteLineRequest`
4. **`CommercialCrmUnifiedHarnessTest`:**
   - Não conter `new JPanel(new GridBagLayout())` em filtros.
   - Conter `TableFilter.bar(`.
   - Conter `UIHelper.ensureHeadersFit(creditTable)` e `UIHelper.ensureHeadersFit(debitTable)`.
5. **`TableActionPlacementHarnessTest`:**
   - Utilizar estritamente `UIHelper.tableCardTop(`.
   - Não possuir barras de botões soltas no rodapé.
6. **`ProfessionalFeedbackHarnessTest`:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui.commercial/
├── CommercialNotesPanel.java                (Orquestrador principal: <= 370 linhas)
│   ├── creditTab() / debitTab()
│   ├── loadCredits() / loadDebits()
│   ├── buildCreditTab() / buildDebitTab()
│   └── showCreditDialog() [Preserva grid column == 6 e CreateCreditNoteLineRequest]
├── DebitNoteFormDialog.java                 (Colaborador modal: <= 95 linhas)
│   └── show(...) - Grelha de linhas de débito, IVA, botões Adicionar/Remover
└── CommercialNotesOperationsCoordinator.java (Coordenador de acções: <= 100 linhas)
    ├── approveCredit / rejectCredit / printCredit
    ├── approveDebit / rejectDebit / printDebit
    ├── sensitivePrint
    └── applyPrintStatuses
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `CommercialNotesPanel.java` | $\le 370$ linhas | Construção das abas, tabelas, filtros e orquestração do ciclo |
| **Modal de Débito** | `DebitNoteFormDialog.java` | $\le 95$ linhas | Diálogo modal ergonómico de emissão de Nota de Débito |
| **Operações & Impressão** | `CommercialNotesOperationsCoordinator.java` | $\le 100$ linhas | Aprovação, rejeição, confirmação e impressão sensível |
