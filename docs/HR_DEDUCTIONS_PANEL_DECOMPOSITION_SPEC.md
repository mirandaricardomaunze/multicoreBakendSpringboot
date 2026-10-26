# SPEC-HR-DEDUCTIONS-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Descontos, Adiantamentos e Empréstimos

## 1. Contexto & Diagnóstico
O painel `HRDeductionsPanel.java` (278 linhas) é responsável pela gestão e acompanhamento do saldo em dívida de adiantamentos salariais, empréstimos e descontos recorrentes na folha de pagamentos de RH.

### Problemas Identificados
1. **Violação de Feedback Profissional (Legacy `JOptionPane`):**
   - O painel contém 5 chamadas a `JOptionPane`:
     - Verificação de lista vazia de colaboradores (`JOptionPane.showMessageDialog` com aviso).
     - Confirmação de gravação bem sucedida (`JOptionPane.showMessageDialog` com informação).
     - Captura de erro em `RuntimeException` (`JOptionPane.showMessageDialog` com erro).
     - Verificação de linha selecionada na desativação (`JOptionPane.showMessageDialog`).
     - Diálogo de confirmação para desativar desconto com saldo pendente (`JOptionPane.showConfirmDialog`).
   - Está presente na allowlist de tolerância do `ProfessionalFeedbackHarnessTest.java`.
2. **Mistura de Responsabilidades no Orquestrador:**
   - O orquestrador acumula construção de layout de tabela e filtros, configuração de Quick Peek, montagem dinâmica de formulários (`ModernFormDialog`) com múltiplos tipos (Adiantamento, Empréstimo, Recorrente) e despacho de mutações assíncronas de ativação/desativação.

## 2. Objectivos da Refatoração
1. **Redução de Linhas do Orquestrador:** Reduzir `HRDeductionsPanel.java` de 278 linhas para $\le 140$ linhas (-50%).
2. **Erradicação Total de `JOptionPane`:** 0 chamadas a `JOptionPane`, avançando a catraca no `ProfessionalFeedbackHarnessTest.java` e retirando o painel da allowlist.
3. **Extração de Colaboradores Especializados:**
   - `HrDeductionFormDialog.java`: Diálogo modal ergonómico baseado em `ModernFormDialog` para novo adiantamento, empréstimo ou desconto recorrente com validação contextual e feedback profissional.
4. **Preservação Integral de Layout e Contratos:**
   - Construtor package-private `HRDeductionsPanel(HRPanel owner)`.
   - Métodos `buildPanel()` e `load()`.
   - Cabeçalho canónico `UIHelper.tableCardTop("Descontos, Adiantamentos e Empréstimos", bar, refreshBtn, stopBtn, newDeductionBtn)`.
   - `TableQuickPeekController.install(table, card, ...)`.
   - `UIHelper.ensureHeadersFit(table)`.
   - Renderizadores canónicos `TableCellRenderers.money()` e `TableCellRenderers.status()`.
