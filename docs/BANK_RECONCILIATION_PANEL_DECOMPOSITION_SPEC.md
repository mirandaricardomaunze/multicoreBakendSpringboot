# SPEC-BANK-RECONCILIATION-PANEL-DECOMPOSITION-001: Decomposição Modular do Centro de Reconciliação Bancária

## 1. Contexto & Diagnóstico

O painel `BankReconciliationPanel.java` é o centro operacional de tesouraria responsável por:
1. Povoamento assíncrono de contas de tesouraria (excluindo caixas físicas) e extractos bancários oficiais.
2. Renderização de cartões KPI de 96px com drilldown interactivo e actualização dinâmica de filtros.
3. Importação de extractos bancários moçambicanos (BIM, BCI, Standard Bank, Moza) via diálogo modal complexo com upload de ficheiro CSV e parsing manual tolerante a delimitadores.
4. Diálogos modais de associação manual de movimentos com transacções de tesouraria do mesmo valor.
5. Lançamento e conciliação imediata de encargos/comissões bancárias via `ModernFormDialog`.
6. Ciclo de fecho de extracto, auto-conciliação e emissão de relatório PDF com pré-visualização.

### Problemas do ficheiro actual (893 linhas):
- Concentra num único ficheiro layout, parsing de ficheiros CSV, múltiplos formulários modais de associação e lógica de exportação PDF.
- Dificulta a manutenção e testes isolados das operações bancárias.
- Viola a directriz corporativa de painéis concisos ($\le 450$ linhas).

---

## 2. Objectivos Arquitecturais

1. **Redução do Orquestrador:** Reduzir `BankReconciliationPanel.java` de 893 para $\le 450$ linhas.
2. **Desacoplamento em 3 Colaboradores Especializados:**
   - `BankStatementImportDialog.java` ($\le 190$ linhas): Diálogo modal de importação de extracto com upload CSV, sanitização de delimitadores e conversão segura de datas/valores.
   - `BankReconciliationMatchDialogs.java` ($\le 190$ linhas): Modais ergonómicos de reconciliação manual e lançamento directo de encargos bancários.
   - `BankReconciliationOperationsCoordinator.java` ($\le 160$ linhas): Coordenação do ciclo de fecho, auto-conciliação, anulação de vínculos, geração de relatório PDF e actualização de estados dos botões de acção.
3. **Preservação Integral de Invariantes & Contratos:**
   - 100% retrocompatível com `BankReconciliationUnifiedHarnessTest` e `GuiActionAlertGuidanceHarnessTest`.
   - `accountCombo`, `statementCombo` e `operationsMenu` rigorosamente contidos no card `ModernPanel(16)`.
   - `mainContent` contendo estritamente os 4 componentes: feedback, kpis, espaçador vertical e tableCard.
   - Menus semânticos respeitando o teto de 5 acções.
   - Tecla Espaço abre silenciosamente o `TableQuickPeekController`.
   - Zero chamadas a `JOptionPane`.

---

## 3. Matriz de Responsabilidades

| Classe | Responsabilidade | Linhas Estimadas | Teto Máximo |
| :--- | :--- | :---: | :---: |
| `BankReconciliationPanel.java` | Orquestrador principal, inicialização de KPIs e card de tabela com QuickPeek | $\sim 360$ | **450** |
| `BankStatementImportDialog.java` | Modal de importação de extracto, upload de CSV e parsing seguro | $\sim 150$ | **190** |
| `BankReconciliationMatchDialogs.java` | Diálogos de vinculação manual e lançamento de encargo bancário | $\sim 150$ | **190** |
| `BankReconciliationOperationsCoordinator.java` | Auto-match, desconciliação, fecho de extracto, relatório PDF e estados de botões | $\sim 125$ | **160** |

---

## 4. Plano de Implementação

1. **Passo 1:** Criar `docs/BANK_RECONCILIATION_PANEL_DECOMPOSITION_SPEC.md` e `docs/BANK_RECONCILIATION_PANEL_DECOMPOSITION_HARNESS.md`.
2. **Passo 2:** Implementar `BankStatementImportDialog.java`.
3. **Passo 3:** Implementar `BankReconciliationMatchDialogs.java`.
4. **Passo 4:** Implementar `BankReconciliationOperationsCoordinator.java`.
5. **Passo 5:** Refatorar `BankReconciliationPanel.java` delegando as operações nos novos colaboradores e mantendo as assinaturas públicas e pacote-privadas intactas.
6. **Passo 6:** Criar `BankReconciliationPanelDecompositionHarnessTest.java` validando linhas, ausência de `JOptionPane` e instanciação de componentes.
7. **Passo 7:** Executar suite completa de testes e validar conformidade arquitectural.
