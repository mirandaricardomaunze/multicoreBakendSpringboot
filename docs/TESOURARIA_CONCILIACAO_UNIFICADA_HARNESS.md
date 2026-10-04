# Harness — Teste e Validação da Tesouraria e Reconciliação Bancária Unificada

> **Status:** Aprovado  
> **Data:** 2026-10-04  
> **Suíte Automatizada:** `BankReconciliationUnifiedHarnessTest.java`

---

## 1. Objectivos de Teste

Validar a conformidade da tela de Reconciliação Bancária com as regras arquiteturais e de design:
1. Card único contendo seletores, filtros e tabela sem barras flutuantes exteriores.
2. Agrupamento de ações secundárias em `ActionMenuButton` com $\le 5$ opções.
3. Presença de `TableQuickPeekController` para inspeção por tecla `SPACE`.
4. Cartões de KPI com interatividade e drilldown.
5. `BankReconciliationPanel.java` com contagem estrita de linhas $\le 1000$.

---

## 2. Casos de Teste (TRECON-01 a TRECON-05)

### TRECON-01: Consolidação no Card `ModernPanel(16)`
- `accountCombo` e `statementCombo` estão contidos dentro da hierarquia do card da tabela.
- Não existem painéis de botões flutuantes externos soltos entre os KPIs e o card.

### TRECON-02: ActionMenuButton de Operações com $\le 5$ Ações
- O cabeçalho do card contém `ActionMenuButton` com as opções:
  - `Auto-Conciliar`
  - `Conciliar Manualmente`
  - `Lançar Encargo Bancário`
  - `Desfazer Conciliação`
  - `Emitir Relatório (PDF)`
- Contagem estrita de acções $\le 5$.

### TRECON-03: Quick Peek Instalado na Tabela de Movimentos
- A tabela `itemsTable` possui `TableQuickPeekController` registado.
- Tecla `SPACE` alterna a visibilidade da gaveta lateral `QuickPeekPanel`.

### TRECON-04: KPIs com Interatividade (Drilldown)
- O cartão de Movimentos Pendentes possui cursor de mão (`Cursor.HAND_CURSOR`) e ouvinte de clique que atualiza o filtro de estado para `"PENDENTE"`.

### TRECON-05: Decomposição Estrita ($\le 1000$ linhas)
- `BankReconciliationPanel.java` possui $\le 1000$ linhas de código.
