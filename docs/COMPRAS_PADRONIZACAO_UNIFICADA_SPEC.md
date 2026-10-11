# Especificação Técnica — Padronização Visual & Ergonómica de Compras & Fornecedores (Fase 1)

> **Status:** Em Execução  
> **Data:** 2026-10-04  
> **Módulo:** `desktop` / Compras & Fornecedores  
> **Documento:** `docs/COMPRAS_PADRONIZACAO_UNIFICADA_SPEC.md`

---

## 1. Diagnóstico do Estado Anterior

A varredura completa ao módulo de Compras identificou incoerências em relação às regras canónicas de UI Swing:

### 1.1 `PurchaseOrdersPanel.java` (Encomendas a Fornecedor)
- **Filtros Multi-Linha:** Barra `poFilters` implementada com `GridBagLayout` em 2 linhas (rótulos superiores e caixas de texto/combos inferiores), roubando ~45px de espaço vertical útil.
- **Reticências Proibidas:** Botão `Nova Encomenda…` e item de menu `Receber Parcial…` violam a secção 4 de `swing_ui_rules.md`.
- **Cabeçalho Fragmentado:** O cabeçalho foi construído fora da estrutura canónica consolidada.
- **Ausência de Quick Peek e Calibração:** Ausência de `TableQuickPeekController` com tecla `Espaço` e sem calibração `UIHelper.ensureHeadersFit(poListTable)`.

### 1.2 `PurchasePayablesPanel.java` (Contas a Pagar a Fornecedores)
- **Duplicação Estrita de Cabeçalho e Ações:** Título "Contas a Pagar a Fornecedores" e botões `Actualizar` e `Registar Pagamento` apareciam simultaneamente no topo externo (`BorderLayout.NORTH` do painel) e dentro do card `listCard` via `UIHelper.tableCardTop`.
- **Filtros Multi-Linha:** `payFilters` com `GridBagLayout` ocupando 2 linhas.
- **Ausência de Quick Peek e Calibração:** Sem `TableQuickPeekController` e sem `UIHelper.ensureHeadersFit(owner.payablesTable)`.

### 1.3 `PurchaseSuppliersPanel.java` (Catálogo de Fornecedores)
- **Filtros Multi-Linha:** `supFilters` em 2 linhas com `GridBagLayout`.
- **Congestionamento de Ações:** Três botões agrupados à direita sem hierarquia clara.
- **Ausência de Quick Peek e Calibração:** Sem `TableQuickPeekController` e sem `UIHelper.ensureHeadersFit(owner.suppliersTable)`.

---

## 2. Padrão Unificado da Solução

### 2.1 Encomendas a Fornecedor (`PurchaseOrdersPanel`)
1. **Filtros em Linha Única:** Barra horizontal `TableFilter.bar(...)` contendo:
   - Campo de pesquisa rápida (`owner.poSearchField`).
   - Seletor de Estado (`poEstado`: Todos, ORDERED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED).
   - Seletor de Período (`poPeriodo`).
   - Botão discreto de Quick Peek (ícone `fas-columns`).
2. **Eliminação de Reticências:**
   - Botão primário: `"Nova Encomenda"`.
   - Item do menu de ações: `"Receber Parcial"`.
3. **Card Único:** `ModernPanel(16)` contendo cabeçalho com título, ações e filtros via `UIHelper.tableCardTop`.
4. **Quick Peek com Tecla Espaço:**
   - Detalhes instantâneos da encomenda: Nº, Fornecedor, Data, Prazo de Entrega, Estado e Valor Total.
   - Ação direta no drawer: "Receber Encomenda" ou "Ver Detalhes".
5. **Calibração de Colunas:** Larguras preferenciais otimizadas e `UIHelper.ensureHeadersFit(poListTable)`.

### 2.2 Contas a Pagar a Fornecedores (`PurchasePayablesPanel`)
1. **Eliminação Total da Duplicação Externa:**
   - Removido o painel `header` exterior; o título "Contas a Pagar a Fornecedores" vive unicamente no topo do `ModernPanel(16)`.
2. **Filtros em Linha Única:**
   - Campo de pesquisa livre (`paySearch`).
   - Seletor de período (`payPeriodo`).
   - Botão discreto de Quick Peek (ícone `fas-columns`).
3. **Ações no Topo do Card:**
   - Esquerda: Título do card.
   - Direita: Botão `Actualizar` e botão de destaque `Registar Pagamento`.
4. **Quick Peek com Tecla Espaço:**
   - Detalhes instantâneos da compra: Nº de Compra, Fornecedor, Valor Total, Montante Pago, Saldo em Aberto e Data.
   - Ação direta: "Registar Pagamento".
5. **Calibração de Colunas:** `UIHelper.ensureHeadersFit(owner.payablesTable)`.

### 2.3 Fornecedores (`PurchaseSuppliersPanel`)
1. **Filtros em Linha Única:**
   - Campo de pesquisa (`supplierSearchField`).
   - Seletor de Estado (`supEstado`).
   - Botão discreto de alternância de Quick Peek (ícone `fas-columns`).
2. **Card Único e Ações:**
   - Esquerda: Título "Fornecedores Cadastrados".
   - Direita: `Actualizar`, `Ações` (Editar, Activar/Desactivar) e `Novo Fornecedor`.
3. **Quick Peek com Tecla Espaço:**
   - Detalhes instantâneos do fornecedor: Nome, NUIT/NIF, Contacto, Telefone, Email, Endereço e Estado.
   - Ação direta: "Editar Fornecedor".
4. **Calibração de Colunas:** `UIHelper.ensureHeadersFit(owner.suppliersTable)`.
