# Especificação — Consolidação e Ergonomia da Tesouraria & Reconciliação Bancária

> **Status:** Aprovado e Canónico  
> **Data:** 2026-10-04  
> **Contexto:** Aplicação dos padrões corporativos executivos (Card Unificado, Regra dos 2-3 Botões com ActionMenuButton, Quick Peek sem modais e KPI Drilldown) ao painel de Tesouraria e Reconciliação Bancária (`BankReconciliationPanel`).

---

## 1. Problema Atual

Conforme demonstrado na tela do utilizador (`Tesouraria & Conciliação Bancária`):
1. **Verticalidade Fragmentada e Barra de Rolagem Global:**
   - 4 linhas empilhadas verticalmente antes da tabela:
     1. Grid de 4 cartões de KPI.
     2. Linha solta com seletores (`Conta Bancária` e `Extracto`).
     3. Linha solta com 5 botões coloridos (`Actualizar`, `Importar Extracto`, `Auto-Conciliar`, `Emitir Relatório`, `Fechar Reconciliação`).
     4. Linha de filtros do card (`Filtrar`, `Estado`, `Data`).
   - Abaixo da tabela, uma 5.ª linha com mais botões soltos (`Conciliar Manualmente`, `Lançar Encargo`, `Desfazer Conciliação`).
   - **Consequência:** A tabela fica comprimida com apenas ~200px de altura e a janela exibe uma barra azul de rolagem global (`ArrowScrollPanel`), forçando o utilizador a rolar verticalmente a tela toda.
2. **Proliferação de Botões:**
   - Mais de 7 botões espalhados em três zonas distintas (cima, meio e baixo), violando o padrão canónico de agrupamento.
3. **Ausência de Inspeção Rápida («Quick Peek»):**
   - O utilizador é obrigado a abrir diálogos modais para inspecionar vínculos entre o extrato bancário e faturas/recibos do ERP.

---

## 2. Decisões Arquiteturais e Padrão Unificado

### 2.1 Consolidação no Card Único `ModernPanel(16)`
- Os seletores de `Conta Bancária` e `Extracto` sobem para o topo do card da tabela (`BorderLayout.NORTH`).
- As acções são consolidadas no canto superior direito do card:
  - `[ ↻ ]` (Botão de actualização multiutilizador)
  - `[ 📥 Importar Extracto ]` (Ação primária de alto destaque)
  - `[ Operações ▾ ]` ([ActionMenuButton](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/ActionMenuButton.java)) contendo:
    1. `Auto-Conciliar Movimentos` (ícone `fas-magic`)
    2. `Conciliar Manualmente` (ícone `fas-link`)
    3. `Lançar Encargo Bancário` (ícone `fas-receipt`)
    4. `Desfazer Conciliação` (ícone `fas-undo`)
    5. `Emitir Relatório (PDF)` (ícone `fas-file-pdf`)
  - `[ 🔒 Fechar Extracto ]` (Ação de encerramento seguro com diálogo de confirmação)
- A barra inferior solta (`buildBottomActionBar`) é eliminada, pois as suas ações passam a residir no menu de operações e dentro da gaveta lateral do Quick Peek.

### 2.2 Ganho de Altura Vertical (+150px)
- A eliminação das linhas soltas exteriores recupera ~150px de altura.
- O card da tabela expande-se verticalmente para preencher a janela.
- A barra de rolagem global da janela desaparece; a rolagem passa a ser exclusivamente interna da tabela `itemsTable`.

### 2.3 «Quick Peek» Silencioso com Tecla Espaço
- Integração de `TableQuickPeekController` em `itemsTable`:
  - `SPACE` / `ESC` abre e recolhe suavemente a gaveta lateral direita (340px) acoplada em `BorderLayout.EAST` do card.
  - Exibe data bancária, descrição original do extrato, referência bancária, valor com realce tipográfico (verde para créditos / ardósia para débitos), badge semântico de estado (`CONCILIADO`, `PENDENTE`) e detalhes da transação vinculada no ERP.
  - Ação direta no rodapé do drawer: "Conciliar Manualmente" ou "Desfazer Vínculo".

### 2.4 Cartões de KPI com Drilldown Interativo
- `KpiCard.makeInteractive`:
  - Cartão **«Movimentos Pendentes»**: filtra `statusFilter` instantaneamente para `"PENDENTE"`.
  - Cartão **«Saldo no Extracto»**: repõe `"Todos os estados"`.
  - Cursor de mão e hover destacado em `UIHelper.ACCENT_BLUE`.
