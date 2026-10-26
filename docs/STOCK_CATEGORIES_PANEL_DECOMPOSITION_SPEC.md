# SPEC-STOCK-CATEGORIES-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Categorias de Produto

## 1. Contexto & Diagnóstico
O painel `StockCategoriesPanel.java` (250 linhas) é responsável pela parametrização, pesquisa e manutenção do catálogo de categorias de produtos de stock e retalho do ERP Multicore.

### Problemas Identificados
1. **Acoplamento de Diálogo Modal no Orquestrador:**
   - O orquestrador implementa internamente o método `openCategoryDialog` (65 linhas), com seletor de paleta de cores `JColorChooser`, swatch interativo, formulário modal `ModernFormDialog` e mutação assíncrona.
2. **Lógica de Renderização e Quick Peek no Orquestrador:**
   - O renderizador gráfico de swatch de cor (`ColorCellRenderer` e `colorSwatchIcon`) e o inspetor lateral Quick Peek aumentam o acoplamento visual.

## 2. Objectivos da Refatoração
1. **Redução de Linhas do Orquestrador:** Reduzir `StockCategoriesPanel.java` de 250 linhas para $\le 130$ linhas (-50%).
2. **Extração de Colaboradores Especializados:**
   - `StockCategoryFormDialog.java` ($\le 95$ linhas): Diálogo modal ergonómico baseado em `ModernFormDialog` para criação e edição de categorias com seletor de cor `JColorChooser` e validação semântica.
   - `StockCategoriesViewCoordinator.java` ($\le 125$ linhas): Coordenador de apresentação para contagem e mapeamento de produtos por categoria (`applyCategories`), filtragem dinâmica em memória (`filterCategories`), renderizador de swatch de cor e Quick Peek lateral.
3. **Erradicação Absoluta de `JOptionPane`:** Zero ocorrências mantidas em todos os ficheiros de categorias.
4. **Preservação Integral de Layout e Contratos:**
   - Construtor package-private `StockCategoriesPanel(StockPanel owner)`.
   - Métodos públicos `buildPanel()` e `refresh()`.
   - `UIHelper.tableCardTop("Categorias de Produto", searchRow, refreshBtn, moreBtn, editBtn, newBtn)`.
   - `UIHelper.ensureHeadersFit(owner.categoriesTable)`.
   - `TableQuickPeekController.install(owner.categoriesTable`.
   - `TableFilter.bar(`.
   - Ausência estrita de `new JPanel(new GridBagLayout())` em formulários de filtros.
