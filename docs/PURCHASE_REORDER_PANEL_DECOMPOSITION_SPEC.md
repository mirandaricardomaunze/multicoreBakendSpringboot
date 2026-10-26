# SPEC-PURCHASE-REORDER-PANEL-DECOMPOSITION-001: Decomposição Modular da Reposição Inteligente de Stock

## 1. Visão Geral e Objectivo
O painel `PurchaseReorderPanel.java` (339 linhas) é a ferramenta analítica de previsão de rutura e reposição inteligente do módulo de Compras do Multicore ERP.
O objetivo desta decomposição é desacoplar o povoamento e agregação analítica de métricas/combos (`PurchaseReorderDataCoordinator`) e as operações de Quick Peek e conversão em encomenda em 1 clique (`PurchaseReorderOperationsCoordinator`), reduzindo o orquestrador para $\le 190$ linhas mantendo rigorosamente todos os contratos e verificações de `PurchaseReorderUnifiedHarnessTest`.

## 2. Invariantes Arquiteturais e Restrições Não Negociáveis
1. **Camadas Físicas**: Módulo `desktop` depende do contrato de compras (`ReorderSuggestionDTO`) e consome `owner.purchaseApiClient.suggestions(companyId)`.
2. **Contratos Visuais e de Teste**:
   - Construtor package-private: `PurchaseReorderPanel(ComprasPanel owner)`.
   - Invariantes exigidos por `PurchaseReorderUnifiedHarnessTest` (REORDER-01 a 05):
     - `KpiCard.createGrid(4)` e `KpiCard.createInteractiveCard(`
     - Proibido `createKpiCard(` e proibido `new ModernPanel(10)`
     - `UIHelper.tableCardTop(` e `new ModernPanel(16)`
     - Proibido `UIHelper.createHeading(` e proibido `GridBagLayout`
     - `TableQuickPeekController.install(reorderTable`
     - `filterUrgency("ESGOTADO")`, `filterUrgency("CRÍTICO")` e `filterUrgency(null)`
     - `setPreferredWidth(widths[i])` e `UIHelper.ensureHeadersFit(reorderTable)`
   - Invariante de `TableActionPlacementHarnessTest`: presença de `UIHelper.tableCardTop(`.
   - Zero ocorrências de `JOptionPane`.

## 3. Arquitetura de Componentes e Decomposição
A reposição inteligente é decomposta em:

```
mz.multicore.erp.gui
├── PurchaseReorderPanel.java (Orquestrador UI e layout canónico: <= 190 linhas)
├── PurchaseReorderOperationsCoordinator.java (Quick Peek e geração de encomendas: <= 85 linhas)
└── PurchaseReorderDataCoordinator.java (Povoamento tabular, cálculo de KPIs e combos dinâmicos: <= 115 linhas)
```

### 3.1 `PurchaseReorderOperationsCoordinator.java`
- `populatePeek(QuickPeekPanel peek, List<ReorderSuggestionDTO> reorderList, int modelRow, Runnable onOpenFull)`: Configuração corporativa do painel lateral com semáforo de urgência, cobertura em dias, embalagem, preço unitário e custo total previsto.
- `orderSelectedOrOpen(JTable reorderTable, List<ReorderSuggestionDTO> reorderList, PurchaseOrdersPanel purchaseOrdersPanel)`: Despacho inteligente para criação de encomenda com produto, fornecedor e quantidade sugerida ou abertura do formulário padrão.

### 3.2 `PurchaseReorderDataCoordinator.java`
- `applyReorderSuggestions(...)`: Processa as sugestões recebidas do servidor, alimenta a grelha de 14 colunas, calcula os produtos esgotados, em rutura iminente e investimento acumulado, atualiza dinamicamente as listas de fornecedores e linhas de negócio sem perder a seleção anterior, e atualiza os rótulos de KPI e o rodapé da tabela.

## 4. Metas de Linhas de Código
| Ficheiro | Linhas Antes | Meta Após | Limite Máximo |
| :--- | :--- | :--- | :--- |
| `PurchaseReorderPanel.java` | 339 | ~180 | $\le 190$ |
| `PurchaseReorderOperationsCoordinator.java` | — | ~65 | $\le 85$ |
| `PurchaseReorderDataCoordinator.java` | — | ~95 | $\le 115$ |
