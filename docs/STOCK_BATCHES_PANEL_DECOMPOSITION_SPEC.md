# Especificação de Decomposição Modular: Lotes & Validades (StockBatchesPanel)

**Identificador:** `SPEC-STOCK-BATCHES-PANEL-DECOMPOSITION-001`  
**Domínio:** Inventário / Lotes e Validades de Artigos  
**Alvo:** `mz.multicore.erp.gui.StockBatchesPanel.java` (292 linhas)  
**Meta de Redução:** $\le 165$ linhas no orquestrador principal ($\approx 43\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `StockBatchesPanel.java` é a aba de controlo de lotes e datas de validade do módulo de inventário (`StockPanel`). É responsável por:
1. Filtros cruzados por pesquisa de texto, armazém, estado de validade e linha de negócio (`TableFilter.bar`).
2. Resumo analítico proativo (`updateBatchesSummary`) alertando para lotes vencidos e a vencer em $\le$ `EXPIRY_SOON_DAYS`.
3. Filtragem em memória dos lotes carregados (`filterBatches`), com suporte a mascaramento de quantidades quando o utilizador não tem privilégio de visualização de stock (`owner.stockHidden()`).
4. Painel de inspeção rápida lateral (`TableQuickPeekController`) com atalho para ficha do artigo (`showProductDetailDialog`) ou criação de lote.
5. Exportação de listagem em PDF (`exportBatchesPdf`).

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **Contratos Visuais & Invariantes de Tabela:**
   - Preservar construtor package-private `StockBatchesPanel(StockPanel owner)`.
   - Preservar métodos públicos `JPanel buildPanel()` e `void refresh()`.
   - Preservar `ModernPanel(16)` e `UIHelper.tableCardTop("Lotes & Validades", ...)`.
   - Preservar `UIHelper.ensureHeadersFit(owner.batchesTable)`.
   - Preservar paginação canónica com `ClientTablePagination.install(owner.batchesTable)`.
   - Preservar `TableQuickPeekController`.
2. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── StockBatchesPanel.java                       (Orquestrador principal: <= 165 linhas)
│   ├── buildPanel() [ModernPanel(16), TableFilter.bar, QuickPeek, tableCardTop]
│   └── refresh()
└── StockBatchesViewCoordinator.java             (Lógica de apresentação e dados: <= 140 linhas)
    ├── applyBatches(owner, loaded)
    ├── updateBatchesSummary(owner)
    ├── filterBatches(owner)
    ├── populateQuickPeek(peek, mr, owner)
    └── exportBatchesPdf(owner)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `StockBatchesPanel.java` | $\le 165$ linhas | Estrutura da aba, controlos de filtro, tabela, paginação e cabeçalho |
| **Coordenador de Lotes** | `StockBatchesViewCoordinator.java` | $\le 140$ linhas | Resumo proativo, filtragem com mascaramento, QuickPeek e exportação PDF |
