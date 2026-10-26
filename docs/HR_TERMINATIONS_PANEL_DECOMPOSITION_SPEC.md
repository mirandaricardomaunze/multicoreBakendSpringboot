# Especificação de Decomposição Modular: Cessações e Acertos Finais (HRTerminationsPanel)

**Identificador:** `SPEC-HR-TERMINATIONS-PANEL-DECOMPOSITION-001`  
**Domínio:** Recursos Humanos / Cessações e Acertos Finais  
**Alvo:** `mz.multicore.erp.gui.HRTerminationsPanel.java` (296 linhas)  
**Meta de Redução:** $\le 165$ linhas no orquestrador principal ($\approx 44\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `HRTerminationsPanel.java` é responsável pela gestão e processamento de rescisões laborais e acertos finais dos colaboradores, incluindo o apuramento de proporcionais legais (13.º mês, férias não gozadas, aviso prévio e compensação), emissão de documentos sensíveis com auditoria (`SensitivePrintCoordinator`) e liquidação financeira.

Actualmente, o painel acumulava:
1. **Diálogos de Rescisão e Apuramento:** Modal de cessação com seletores de colaborador, motivo legal, compensação e aviso prévio em `ModernFormDialog`, seguido de diálogo de confirmação do apuramento de acerto com `JOptionPane.showConfirmDialog`.
2. **Liquidação Financeira e Impressão:** Confirmação de pagamento com `JOptionPane.showConfirmDialog` e despacho para `SensitivePrintCoordinator` (Acerto PDF e Certificado de Trabalho).
3. **Inspeção Rápida (Quick Peek):** Mapeamento detalhado dos campos da linha de rescisão.
4. **Orquestração da Interface:** Tabela de 10 colunas, card `ModernPanel(16)` e barra de filtros `TableFilter.bar`.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **`HrCrmUiErgonomicsHarnessTest`:**
   - Conter textualmente:
     ```java
     UIHelper.tableCardTop("Cessações e Acertos Finais", bar,
             refreshBtn, docsBtn, payBtn, newBtn)
     ```
   - Menu `docsBtn` com "Acerto PDF" e "Certificado de Trabalho".
2. **`HRUnifiedHarnessTest` (`HR-03`):**
   - Conter `UIHelper.ensureHeadersFit(table)`.
   - Conter `TableQuickPeekController.install(table`.
3. **`TableActionPlacementHarnessTest`:**
   - Conter `UIHelper.tableCardTop(`.
4. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.
   - Avanço da catraca no `ProfessionalFeedbackHarnessTest.java` (`.isZero()` e remoção da allowlist).

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── HRTerminationsPanel.java                     (Orquestrador principal: <= 165 linhas)
│   ├── buildPanel() [ModernPanel(16), tableCardTop, TableQuickPeekController, ensureHeadersFit]
│   └── load(), apply(List<TerminationDTO>)
├── HrTerminationFormDialog.java                 (Formulário e apuramento: <= 135 linhas)
│   ├── openTerminationDialog(HRPanel, Runnable)
│   └── confirmAndTerminate(HRPanel, CreateTerminationRequest, TerminationDTO, Runnable)
└── HRTerminationsOperationsCoordinator.java     (Operações de pagamento & print: <= 90 linhas)
    ├── paySettlement(...) [ModernMessageDialog.confirm, zero JOptionPane]
    ├── printSettlement(...)
    ├── printCertificate(...)
    └── populatePeek(...)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `HRTerminationsPanel.java` | $\le 165$ linhas | Layout, tabela, filtros, menu semântico e dados |
| **Formulário de Rescisão** | `HrTerminationFormDialog.java` | $\le 135$ linhas | Diálogo de cessação e confirmação de acerto final |
| **Coordenador Operacional** | `HRTerminationsOperationsCoordinator.java` | $\le 90$ linhas | Pagamento, impressão sensível e Quick Peek |
