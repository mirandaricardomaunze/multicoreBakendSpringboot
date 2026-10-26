# Especificação de Decomposição Modular: Folha de Ponto & Assiduidade (HRTimeSheetPanel)

**Identificador:** `SPEC-HR-TIMESHEET-PANEL-DECOMPOSITION-001`  
**Domínio:** Recursos Humanos / Ponto e Assiduidade  
**Alvo:** `mz.multicore.erp.gui.HRTimeSheetPanel.java` (288 linhas)  
**Meta de Redução:** $\le 165$ linhas no orquestrador principal ($\approx 43\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `HRTimeSheetPanel.java` é responsável pela visualização da assiduidade e horas suplementares apuradas por mês/ano, fecho/reabertura de período de ponto, registo manual de marcações de colaboradores e configuração das taxas de acréscimo de horas extra (diurna, noturna, descanso/feriado).

Atualmente acumulava:
1. **Diálogo de Registo de Marcação Manual:** `recordEntry()` (40 linhas), construindo formulário com combo de colaboradores, `DateField`, `JTextField` para horas e `ModernFormDialog`.
2. **Diálogo de Acréscimos de Hora Extra:** `openOvertimeRatesDialog()` (55 linhas), com formulário de multiplicadores legais e chamada de persistência.
3. **Diálogo Legado `JOptionPane`:** Linha 171 utilizava `JOptionPane.showConfirmDialog` para confirmação de fecho do mês, figurando na lista de exceções do `ProfessionalFeedbackHarnessTest`.
4. **Orquestração da Tabela:** Tabela de 9 colunas, Quick Peek lateral (`TableQuickPeekController`), filtros de ano/mês e cartão `ModernPanel(16)`.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **Contratos Visuais & Invariantes de Tabela:**
   - Preservar construtor package-private `HRTimeSheetPanel(HRPanel owner)`.
   - Preservar método package-private `JPanel buildPanel()` e `void load()`.
   - Preservar `ModernPanel(16)` e `UIHelper.tableCardTop("Folha de Ponto", filters, moreBtn, closeBtn, entryBtn)`.
   - Preservar `UIHelper.ensureHeadersFit(table)`.
   - Preservar `TableQuickPeekController.install(table, card, ...)`.
   - Preservar `TableFilter.bar(search, refreshBtn)`.
2. **Eliminação de Diálogos Legados:**
   - **Substituir o `JOptionPane.showConfirmDialog` residual por `UIHelper.confirm(...)`.**
   - **Avançar a catraca no `ProfessionalFeedbackHarnessTest.java` para `.isZero()` e remover da allowlist.**

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── HRTimeSheetPanel.java                       (Orquestrador principal: <= 165 linhas)
│   ├── buildPanel() [ModernPanel(16), tableCardTop, TableFilter.bar, QuickPeek]
│   ├── load(), apply(TimeSheetDTO), toggleClose()
├── HrTimeEntryDialog.java                      (Diálogo modal de marcação: <= 90 linhas)
│   └── show(HRPanel owner, Runnable onSuccess)
└── HrOvertimeRatesDialog.java                  (Diálogo modal de acréscimos: <= 110 linhas)
    └── show(HRPanel owner)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `HRTimeSheetPanel.java` | $\le 165$ linhas | Apuramento mensal, tabela, filtros de período e fecho/reabertura |
| **Diálogo de Marcação** | `HrTimeEntryDialog.java` | $\le 90$ linhas | Formulário modal para registo manual de entrada/saída de colaborador |
| **Diálogo de Acréscimos** | `HrOvertimeRatesDialog.java` | $\le 110$ linhas | Formulário modal para taxas e base legal de horas extra |
