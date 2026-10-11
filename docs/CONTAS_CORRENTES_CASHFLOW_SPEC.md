# Especificação Técnica — Padronização de Contas Correntes & Cash Flow Previsional (Fase 2)

> **Status:** Em Execução  
> **Data:** 2026-10-04  
> **Módulo:** `desktop` / Tesouraria & Extratos / Contas Correntes & Cash Flow  
> **Documento:** `docs/CONTAS_CORRENTES_CASHFLOW_SPEC.md`

---

## 1. Diagnóstico do Estado Anterior

A auditoria aos ecrãs de Extratos de Conta Corrente e Cash Flow Previsional revelou discrepâncias em relação às diretrizes canónicas:

### 1.1 `CustomerStatementPanel.java` (Conta Corrente & Reconciliação de Clientes)
- **Cabeçalho e Filtros Fora do Card:** O seletor de cliente, datas de início/fim e botões de ação (`Imprimir Extrato PDF`, `Enviar por Email`) viviam num `buildHeader()` externo solto no topo, em vez de integrados no card da tabela.
- **Card Não-Padronizado:** `buildTableCard()` criava `new ModernPanel(12)` em vez do padrão canónico de raio 16 (`ModernPanel(16)`).
- **Sem Quick Peek e Calibração:** Ausência de `TableQuickPeekController` ativado por `Espaço` e sem calibração `UIHelper.ensureHeadersFit`.

### 1.2 `SupplierStatementPanel.java` (Conta Corrente & Reconciliação de Fornecedores)
- **Cabeçalho e Filtros Fora do Card:** Mesma inconsistência — seletores e botões soltos no topo externo.
- **Card com Raio 12:** `new ModernPanel(12)` em vez de `ModernPanel(16)`.
- **Sem Quick Peek e Calibração:** Ausência de `TableQuickPeekController` e `UIHelper.ensureHeadersFit`.

### 1.3 `CashFlowForecastPanel.java` (Projeção de Fluxo de Caixa Previsional)
- **Cartões com Raio 10:** Criação de `ModernPanel(10)` em `buildBucketsCard()` e `buildDetailsCard()`.
- **Calibração de Cabeçalhos:** Tabelas de horizontes e maiores contas sem `UIHelper.ensureHeadersFit`.

---

## 2. Padrão Unificado da Solução

### 2.1 Conta Corrente de Clientes e Fornecedores
1. **Estrutura Canónica:**
   - `BorderLayout.NORTH`: KPIs analíticos canónicos (`KpiCard.createGrid(5)` para clientes e `KpiCard.createGrid(4)` para fornecedores).
   - `BorderLayout.CENTER`: Card único `ModernPanel(16)` contendo:
     - `BorderLayout.NORTH`: Cabeçalho via `UIHelper.tableCardTop(...)` integrando:
       - Seletor de entidade (Cliente ou Fornecedor).
       - Intervalo de datas (`DateField` início e fim).
       - Ações à direita: `Actualizar`, `Imprimir Extrato (PDF)` e `Enviar por Email`.
       - Botão discreto de alternância de Quick Peek (ícone `fas-columns`).
     - `BorderLayout.CENTER`: Scroll da tabela com larguras preferenciais e `UIHelper.ensureHeadersFit`.
     - `BorderLayout.SOUTH`: Paginação da tabela com `ClientTablePagination`.
2. **«Quick Peek» Silencioso com Tecla Espaço:**
   - Detalhes do movimento: Data, Tipo de Documento, Nº de Documento, Descrição, Referência, Débito, Crédito e Saldo Acumulado.
   - Destaque monetário profissional com realce tipográfico.

### 2.2 Projeção de Fluxo de Caixa Previsional
1. **Padronização dos Painéis:** Substituição de `ModernPanel(10)` por `ModernPanel(16)` nos blocos analíticos.
2. **Calibração de Tabelas:** Chamada obrigatória a `UIHelper.ensureHeadersFit` para garantir legibilidade estrita dos cabeçalhos monetários.
