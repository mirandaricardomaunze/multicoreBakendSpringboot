# SPEC-ACCOUNTING-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Contabilidade

## 1. Contexto e Diagnóstico

O painel `AccountingPanel.java` (533 linhas) é a central de contabilidade geral do Multicore ERP:
- **Plano de Contas:** Visualização hierárquica das contas, semeadura do PGC-NIRF, aviso preventivo de ausência de plano e registo manual de novas contas.
- **Diário Contabilístico:** Livro diário com paginação remota (`TablePager`), registo manual em partidas dobradas e verificação de equilíbrio débito/crédito.
- **Balancete de Verificação:** Apuramento periódico de balancete por intervalo de datas com validação de encerramento.
- **Razão Geral (Extrato de Conta):** Consulta de movimentos e saldo progressivo por conta movimentável.
- **Demonstrações Financeiras:** Demonstração de Resultados (Proveitos vs Custos) e Balanço Patrimonial (Activo, Passivo e Capital Próprio).

### Problemas Arquiteturais Identificados no Monólito:
1. **Acumulação de 5 Abas Complexas num Único Ficheiro:** O painel acumula a construção, modelos de tabela (`DefaultTableModel`), formulários modais de criação (`openAccountDialog`, `openEntryDialog`) e lógica de carregamento assíncrono para todas as 5 dimensões contabilísticas num único arquivo.
2. **Mistura de UI e Formatação Local:** Helpers de tabela, renderizadores monetários e manipuladores de datas estão duplicados no corpo do painel.
3. **Padrão Canónico de Abas:** Módulos corporativos de grande porte como `FiscalPanel` (Fase 11) foram decompostos com enorme sucesso em componentes especializados por aba (`FiscalIvaTab`, `FiscalTaxRatesTab`, etc.), proporcionando clareza cirúrgica e facilidade de manutenção.

---

## 2. Decisão de Arquitetura e Estrutura Decomposta

No pacote `mz.multicore.erp.gui.accounting`:

1. **`AccountingPanel.java` (Orquestrador Magro $\le 120$ linhas):**
   - Mantém o construtor canónico `public AccountingPanel(AccountingApiClient accountingApiClient)`.
   - Inicializa `InlineFeedbackPanel feedback` e o `JTabbedPane` com `UIHelper.styleTabbedPaneMulticore(tabs)`.
   - Constrói e injeta as 5 abas especializadas.
   - Implementa o ciclo de vida `onPanelSelected()`, delegando o carregamento inicial para as abas ativas.
   - Preserva estritamente as asserções de testes existentes: `UIHelper.tableCardTop(` e `UIHelper.ensureHeadersFit(table)`.

2. **`AccountingChartTab.java` ($\le 140$ linhas):**
   - Responsabilidade: Aba "Plano de Contas".
   - Tabela de contas, aviso preventivo de plano vazio (`emptyChartWarning`), semeadura do PGC-NIRF e diálogo modal para criação de contas em partidas dobradas com `ModernFormDialog`.

3. **`AccountingJournalTab.java` ($\le 150$ linhas):**
   - Responsabilidade: Aba "Diário".
   - Tabela paginada com `TablePager`, filtro rápido `TableQuickFilterBar`, barra de totais e diálogo modal de novo lançamento em partidas dobradas.

4. **`AccountingTrialBalanceTab.java` ($\le 90$ linhas):**
   - Responsabilidade: Aba "Balancete".
   - Filtros de intervalo de datas (`balanceFrom`, `balanceTo`), cálculo assíncrono e sumário de encerramento (verificação se o balancete fecha).

5. **`AccountingLedgerTab.java` ($\le 95$ linhas):**
   - Responsabilidade: Aba "Razão".
   - Pesquisa por código de conta e intervalo de datas, tabela de movimentos com saldo progressivo e resumo da conta.

6. **`AccountingStatementsTab.java` ($\le 130$ linhas):**
   - Responsabilidade: Aba "Demonstrações".
   - Demonstração de resultados e Balanço, cálculo e verificação de fecho patrimonial.

7. **`AccountingUiHelper.java` ($\le 90$ linhas):**
   - Utilitário comum de UI contabilística: criação e estilização de tabelas com `TableQuickPeekController`, renderizadores monetários, cartões `ModernPanel(16)` e parsing seguro de datas.

---

## 3. Preservação Estrita de Contratos e Restrições Não-Negociáveis

1. **Construtores e APIs:**
   - Preservar `public AccountingPanel(AccountingApiClient accountingApiClient)` intacto.
   - Preservar `public void onPanelSelected()` intacto.
2. **Conformidade de Testes:**
   - `TableActionPlacementHarnessTest`: proibido colocar botões em `BorderLayout.SOUTH` de cards de tabela; o topo de card deve usar `UIHelper.tableCardTop`.
   - `AccountingFiscalUnifiedHarnessTest`: deve conter `UIHelper.ensureHeadersFit(table)`.
   - `DesktopInitializationTest`: instanciação limpa de `AccountingPanel`.
   - `DesktopThinContextTest`: contexto do desktop livre de JPA ou beans de backend.
3. **Ergonomia e Feedback:**
   - Proibido qualquer diálogo `JOptionPane`. Utilização exclusiva de `InlineFeedbackPanel`, `ToastManager` e `ModernFormDialog`.
   - Proibido qualquer emoji Unicode em labels, abas ou botões.

---

## 4. Métricas de Sucesso

| Componente | Linhas Anteriores | Linhas Alvo | Redução Esperada |
|---|---|---|---|
| `AccountingPanel.java` | 533 | $\le 120$ | $\ge 77\%$ |
| `AccountingChartTab.java` | 0 | $\le 140$ | Novo componente |
| `AccountingJournalTab.java` | 0 | $\le 150$ | Novo componente |
| `AccountingTrialBalanceTab.java` | 0 | $\le 90$ | Novo componente |
| `AccountingLedgerTab.java` | 0 | $\le 95$ | Novo componente |
| `AccountingStatementsTab.java` | 0 | $\le 130$ | Novo componente |
| `AccountingUiHelper.java` | 0 | $\le 90$ | Novo utilitário |
| **Erros de Teste / Regressões** | 0 | 0 | 100% aprovado |
