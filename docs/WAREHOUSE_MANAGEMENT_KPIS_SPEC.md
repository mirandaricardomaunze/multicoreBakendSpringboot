# WAREHOUSE_MANAGEMENT_KPIS_SPEC — Indicadores Executivos de Gestão de Armazéns

**Estado:** Aprovado  
**Data:** 2026-10-18  
**Domínio:** Stock & Armazéns / WMS  
**Módulos Envolvidos:** `desktop` (GUI), `contracts` (DTOs)

---

## 1. Contexto & Objectivo

O Multicore ERP gerencia redes de armazenagem multi-armazém (armazéns centrais, entrepostos, depósitos frios, lojas de retalho com balcão POS e armazéns virtuais de trânsito).
A aba **"Gestão de Armazéns"** (`StockWarehousesPanel`) anteriormente apresentava apenas a grelha tabular de armazéns, sem uma camada executiva consolidada de indicadores.

Esta especificação formaliza a integração da grelha de **4 KPIs executivos uniformes** no topo da aba de Gestão de Armazéns, permitindo aos gestores e directores de logística visualizar instantaneamente a capacidade operacional e a distribuição de existências da empresa.

---

## 2. Requisitos Não Negociáveis & Design System

1. **Uniformidade Visual de KPIs:**
   - Conforme `AGENTS.md` e `docs/UI_DESIGN_SYSTEM.md`, KPIs em painéis com tabela devem ser dispostos em linha horizontal única com `KpiCard.createGrid(4)` e altura canónica `KpiCard.STANDARD_CARD_HEIGHT = 96px`.
   - Expressamente proibido criar cartões com classes privadas ou layout ad-hoc.
2. **Estrutura de Layout do Painel:**
   - O painel da aba (`tab`) usa `BorderLayout`.
   - `BorderLayout.NORTH`: Grid de KPIs (`KpiCard.createGrid(4)`).
   - `BorderLayout.CENTER`: Card canónico `ModernPanel(16)` contendo o topo da tabela (`UIHelper.tableCardTop`), barra de filtros horizontal (`TableFilter.bar`) e a `JTable`.
3. **Cálculo em Tempo Real:**
   - Os indicadores devem ser recalculados dinamicamente sempre que os armazéns ou saldos de stock forem recarregados (`onPanelSelected` / `refresh`).
4. **Resiliência e Ausência de NPEs:**
   - Tratamento de valores nulos, listas vazias ou capacidades não informadas (formatadas com segurança).

---

## 3. Especificação dos 4 KPIs Executivos

### KPI 1: Armazéns Activos
- **Título:** `"Armazéns Activos"`
- **Valor Principal:** `X / Y` (onde X = total de armazéns com `active == true` e Y = total cadastrado).
- **Subtítulo / Detalhe:** `X loja(s) POS · Y depósito(s)` (quantos permitem vendas directas no POS vs depósitos de apoio/reserva).
- **Ícone:** `"fas-warehouse"`
- **Cor:** `UIHelper.ACCENT_CYAN`

### KPI 2: Capacidade Global Instalada
- **Título:** `"Capacidade Instalada"`
- **Valor Principal:** Somatório das capacidades nominais (`capacity`) dos armazéns cadastrados (ex.: `"250.000 un"` ou `"—"` se nenhuma informada).
- **Subtítulo / Detalhe:** `"Capacidade máxima de armazenagem"`
- **Ícone:** `"fas-boxes"`
- **Cor:** `UIHelper.MODULE_STOCK`

### KPI 3: Valor Total Alocado
- **Título:** `"Valor em Armazém"`
- **Valor Principal:** Somatório do valor financeiro das existências alocadas nos armazéns (`quantidade × preço` ou somatório valorizado dos stocks).
- **Subtítulo / Detalhe:** `"Existências valorizadas em stock"`
- **Ícone:** `"fas-coins"`
- **Cor:** `UIHelper.APPROVED_GREEN`

### KPI 4: Rede de Distribuição
- **Título:** `"Rede de Distribuição"`
- **Valor Principal:** Quantidade de tipos de armazéns em operação (ex.: `"3 Tipos"` ou total de unidades operacionais).
- **Subtítulo / Detalhe:** `"Lojas, depósitos e centros de distribuição"`
- **Ícone:** `"fas-network-wired"`
- **Cor:** `UIHelper.ACCENT_BLUE`

---

## 4. Matriz de Rastreabilidade de Testes

| ID Teste | Descrição | Componente |
| :--- | :--- | :--- |
| `H-WMS-01` | Renderização do grid com 4 KPIs canónicos de 96px | `StockWarehouseKpiHarnessTest` |
| `H-WMS-02` | Contagem precisa de armazéns activos e lojas de vendas POS | `StockWarehouseKpiHarnessTest` |
| `H-WMS-03` | Cálculo correcto da capacidade instalada agregada | `StockWarehouseKpiHarnessTest` |
| `H-WMS-04` | Valorização financeira precisa do inventário em armazém | `StockWarehouseKpiHarnessTest` |
