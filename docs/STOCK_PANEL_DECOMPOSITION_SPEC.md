# SPEC-STOCK-PANEL-DECOMPOSITION-001: Decomposição Modular do StockPanel

## 1. Contexto e Motivação
O painel principal de inventário e armazéns ([StockPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockPanel.java)) acumulou 937 linhas de código, atuando como um orquestrador que agrega:
1. Gestão de abas corporativas (Níveis de Stock, Alertas, Lotes & Validades, Movimentos & Rastreabilidade, Transferências entre Armazéns, Gestão de Armazéns, Categorias, Quebras & Desperdício, Inventário Físico).
2. Construção e lógica da aba de Níveis de Stock com filtros combinados e QuickPeek.
3. Construção e paginação remota da aba de Movimentos & Rastreabilidade com formatação de lotes e séries.
4. Construção e fluxo da aba de Transferências entre Armazéns com 5 KPIs e integração de editor de guias.
5. Bloqueio de stock (contagem cega) para ocultação de quantidades a utilizadores não-administradores.
6. Coordenação assíncrona de carregamento de armazéns, saldos de stock, movimentos e catálogo.

Para cumprir a arquitetura desacoplada do Multicore ERP, o painel deve ser decomposto de modo que o orquestrador `StockPanel.java` delegue as tarefas especializadas para componentes dedicados, mantendo 100% de compatibilidade com os harnesses e contratos existentes.

---

## 2. Metas de Linhas e Distribuição de Responsabilidades

| Componente | Responsabilidade Principal | Limite Máximo | Meta Típica |
| :--- | :--- | :---: | :---: |
| **`StockPanel.java`** | Orquestrador principal, inicialização das abas, barra de topo e layouts canónicos. | $\le 450$ linhas | $\sim 420$ linhas |
| **`StockMovementsCoordinator.java`** | Paginação, filtros de negócio/artigo, carregamento assíncrono e formatação de movimentos de stock. | $\le 200$ linhas | $\sim 150$ linhas |
| **`StockLockCoordinator.java`** | Gestão de bloqueio de stock para contagem cega, alternância de permissão e banner informativo. | $\le 120$ linhas | $\sim 80$ linhas |
| **`StockWarehouseCoordinator.java`** | Carregamento assíncrono de armazéns e saldos de stock, sincronização de combos e acionamento de coletores. | $\le 160$ linhas | $\sim 110$ linhas |

---

## 3. Invariantes de Negócio e Contratos a Preservar

1. **Construtores Públicos Retrocompatíveis:**
   - Preservar o construtor de 7 parâmetros e o construtor sobrecarregado com `InventoryPhysicalCountingApiClient`.
2. **Campos Package-Private auditados pelos testes:**
   - `stockTable`, `movementsTable`, `transferTable`.
   - `kpiTransfersSeparation`, `kpiTransfersPending`, `kpiTransfersInTransit`, `kpiTransfersCompleted`, `kpiTransfersTotalValue`.
   - `warehouseFilterCombo`, `warehousesList`, `stocksList`, `stockModel`, `transferModel`, `movementsModel`.
   - `transferPages`, `transferEditorHost`, `transferEditorForm`.
3. **Padrões Canónicos e Textuais Auditados:**
   - `TableFilter.bar(...)` em linha única horizontal (proibido GridBagLayout).
   - `UIHelper.ensureHeadersFit(...)` para calibração visual de colunas.
   - `TableQuickPeekController.install(...)` acionado silenciosamente com tecla Espaço.
   - `stockTable.putClientProperty("noRowInspector", Boolean.TRUE)`.
   - Botões de recarregamento com tooltips normalizados ("Recarregar dados de stock e armazéns").
   - ActionMenuButton com limite de $\le 5$ opções por menu.
   - Zero chamadas a `JOptionPane`. Modais exclusivamente via `ModernFormDialog` ou `ToastManager`.

---

## 4. Plano de Validação e Testes
1. `StockUnifiedHarnessTest` (STOCK-01 a STOCK-03).
2. `StockInteractionHarnessTest`.
3. `StockCommercialUiHarnessTest`.
4. `StockInTransitUiHarnessTest`.
5. `StockTransferDraftUiHarnessTest`.
6. `StockPanelDecompositionHarnessTest` (novo teste de conformidade de linhas e desacoplamento).
7. `DesktopThinContextTest` (garantia de inicialização em arranque).
8. `MultiModuleArchitectureHarnessTest` (validação de fronteiras físicas multi-módulo).
