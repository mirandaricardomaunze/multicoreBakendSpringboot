# Especificação de Decomposição Modular: Contratos de Trabalho (HRContractsPanel)

**Identificador:** `SPEC-HR-CONTRACTS-PANEL-DECOMPOSITION-001`  
**Domínio:** Recursos Humanos / Gestão de Contratos de Trabalho  
**Alvo:** `mz.multicore.erp.gui.HRContractsPanel.java` (301 linhas)  
**Meta de Redução:** $\le 175$ linhas no orquestrador principal ($\approx 45\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `HRContractsPanel.java` é responsável pela gestão e ciclo de vida de contratos de trabalho (sem termo, a termo certo/incerto, temporário e estágio), emissão de contratos com impressão sensível (`SensitivePrintCoordinator`), ativação com passagem de salário à ficha do colaborador, renovação com cálculo da nova data de vigência e rescisão auditada.

Actualmente, o painel acumulava:
1. **Diálogos Modais de Ciclo de Vida:** Criação de contrato com 10 campos em `ModernFormDialog`, diálogo de renovação e diálogo de cessação contratual.
2. **Coordenação Operacional & Confirmação:** Ativação com confirmação via diálogo legado `JOptionPane`, e despacho de impressão de documento sensível.
3. **Configuração de Quick Peek:** Mapeamento de colunas para resumo rápido via barra de espaço.
4. **Orquestração da Interface:** Tabela de contratos com 8 colunas, filtros por estado (`TableFilter.combo`) e menu semântico de acções agrupadas `ActionMenuButton("Gestão do Contrato")`.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **`HRContractsPanelHarnessTest` (`HRC-01`, `HRC-02`, `HRC-03`):**
   - Construtor package-private `HRContractsPanel(HRPanel owner)`.
   - Método `JPanel buildPanel()`.
   - Card central `ModernPanel(16)` contendo filtros no topo (`BorderLayout.NORTH`) e `JScrollPane` no centro (`BorderLayout.CENTER`).
   - `ActionMenuButton` com rótulo "Gestão do Contrato".
2. **`HRUnifiedHarnessTest` (`HR-01`):**
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
├── HRContractsPanel.java                     (Orquestrador principal: <= 175 linhas)
│   ├── buildPanel() [ModernPanel(16), tableCardTop, TableQuickPeekController, ensureHeadersFit]
│   └── load(), apply(List<EmploymentContractDTO>)
├── HrContractFormDialog.java                 (Diálogos de ciclo de vida: <= 140 linhas)
│   ├── showCreateDialog(HRPanel, Runnable)
│   ├── showRenewDialog(HRPanel, EmploymentContractDTO, Runnable)
│   └── showTerminateDialog(HRPanel, EmploymentContractDTO, Runnable)
└── HRContractsOperationsCoordinator.java     (Operações & Quick Peek: <= 80 linhas)
    ├── printContract(...)
    ├── activateContract(...) [UIHelper.confirm, zero JOptionPane]
    └── populatePeek(...)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `HRContractsPanel.java` | $\le 175$ linhas | Layout, tabela, filtros, menu semântico e orquestração |
| **Formulários Contratuais** | `HrContractFormDialog.java` | $\le 140$ linhas | Diálogos de novo contrato, renovação e cessação |
| **Coordenador Operacional** | `HRContractsOperationsCoordinator.java` | $\le 80$ linhas | Impressão sensível, ativação segura e Quick Peek |
