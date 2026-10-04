# Especificação Técnica — Padronização Visual & Ergonómica da Reposição Inteligente

> **Status:** Aprovado  
> **Data:** 2026-10-04  
> **Módulo:** `desktop` / Compras / Reposição Inteligente  
> **Documento:** `docs/REPOSICAO_INTELIGENTE_PADRONIZACAO_SPEC.md`

---

## 1. Diagnóstico do Estado Anterior

Na análise visual da aba **Compras -> Reposição** ("Reposição Inteligente & Previsão de Rutura"), foram identificadas 4 não-conformidades críticas com o design system do Multicore ERP:

1. **KPI Cards Não-Padronizados:**
   - O painel utilizava um método privado `createKpiCard` com cartões brancos/cinzentos rasos (~40px de altura) e ícones desalinhados, violando a especificação `KpiCard.STANDARD_CARD_HEIGHT = 96px` e a paleta semântica canónica.
2. **Duplicação de Título e Ações:**
   - O título "Reposição Inteligente & Previsão de Rutura" e os botões `[Actualizar]` e `[Criar Encomenda]` apareciam duplicados tanto no topo externo da página como no cabeçalho interno do card.
3. **Filtros Empilhados com Excesso de Altura:**
   - A barra de filtros utilizava `GridBagLayout` com 2 linhas (labels na linha superior e campos na inferior), desperdiçando espaço vertical útil.
4. **Tabela com Truncamento Excessivo e Sem Quick Peek:**
   - 13 colunas sem larguras preferenciais otimizadas geravam truncamento por reticências em série (`Stock ...`, `Venda...`, `Dias S...`, `Sugeri...`, `Fornecedor Habit...`, `Preço ...`, `Custo ...`).
   - Ausência do controlador «Quick Peek» por tecla `Espaço` (`TableQuickPeekController`).

---

## 2. Padrão Unificado da Solução

### 2.1. Cartões de KPI Canónicos (`KpiCard.createGrid(4)`)
- **Card 1 (Vermelho / `REJECTED_RED`):** `Produtos Esgotados` (Interativo: clique filtra para `ESGOTADO`).
- **Card 2 (Amarelo / `PENDING_YELLOW`):** `Rutura Iminente (≤ 7d)` (Interativo: clique filtra para `CRÍTICO`).
- **Card 3 (Azul / `ACCENT_BLUE`):** `Total a Repor` (Interativo: clique repõe todos os produtos).
- **Card 4 (Verde / `APPROVED_GREEN`):** `Custo Estimado` (Investimento total em Meticais `MT`).

### 2.2. Card Consolidado (`ModernPanel(16)`)
- **Cabeçalho Único:**
  - `WEST`: Subtítulo com texto explicativo do algoritmo de rotação (30 dias).
  - `EAST`: Botões de ação no padrão `actionsBar(refreshBtn, orderBtn)`.
- **Barra de Filtro Horizontal Canónica:**
  - `SearchField` (pesquisa livre em produto, SKU ou fornecedor).
  - `Urgência:` [Todas, ESGOTADO, CRÍTICO, BAIXO].
  - `Fornecedor:` [Todos os fornecedores...].
  - `[Quick Peek]` no lado direito da barra de filtros.
- **Tabela Formatada com `UIHelper.ensureHeadersFit`:**
  - Larguras calibradas para visualização confortável em telas 1366x768 e superiores.
  - Renderers monetários (`TableCellRenderers.money()`) e de estado (`TableCellRenderers.status()`).

### 2.3. «Quick Peek» Silencioso em Sugestões de Compra
- Tecla `Espaço` e botão lateral exibem gaveta com:
  - Nome do produto, SKU e Fornecedor habitual.
  - Stock atual vs Stock mínimo.
  - Velocidade média diária e dias de cobertura restante.
  - Embalagem: Unidades por caixa, Sugestão em Caixas e Unidades Totais.
  - Custo unitário e Custo total previsto em Meticais.
  - Ação direta no drawer: "Criar Encomenda para este Fornecedor".
