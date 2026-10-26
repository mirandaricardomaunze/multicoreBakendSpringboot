# HARNESS-FISCAL-PANEL-DECOMPOSITION-001: Harness de Validação da Decomposição do FiscalPanel (Fase 11)

## 1. Objectivos do Teste de Harness
O teste automatizado `FiscalPanelDecompositionHarnessTest` verifica:
1. **DECOMP-01: Limites Físicos Estritos de Linhas de Código**:
   - `FiscalPanel.java` $\le 160$ linhas.
   - `FiscalIvaTab.java` $\le 260$ linhas.
   - `FiscalTaxRatesTab.java` $\le 180$ linhas.
   - `FiscalWithholdingsTab.java` $\le 200$ linhas.
   - `FiscalPayrollTab.java` $\le 140$ linhas.
   - `FiscalDeclarationsTab.java` $\le 100$ linhas.
2. **DECOMP-02: Zero Diálogos Legados `JOptionPane`**:
   - Nenhum dos ficheiros do módulo fiscal pode invocar `JOptionPane.show...`.
3. **DECOMP-03: Integridade das 5 Abas Corporativas**:
   - `FiscalPanel` constrói e expõe exactamente 5 abas estilizadas:
     - "Apuramento IVA"
     - "Taxas Fiscais"
     - "Retenções na Fonte"
     - "IRPS & INSS Salarial"
     - "Declarações"
4. **DECOMP-04: Ciclo de Vida e Inicialização Assíncrona Segura**:
   - Invocação de `onPanelSelected()` e `refreshAll()` sem falhas ou NPE quando as APIs respondem com mocks/stubs.
5. **DECOMP-05: Conformidade da Catraca de Feedback Profissional**:
   - `FiscalPanel.java` removido com sucesso da allowlist do `ProfessionalFeedbackHarnessTest`.
