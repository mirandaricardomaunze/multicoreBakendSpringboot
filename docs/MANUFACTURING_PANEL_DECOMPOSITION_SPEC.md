# SPEC-MANUFACTURING-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Produção & Transformação Industrial

## 1. Contexto e Diagnóstico

O painel `ManufacturingPanel.java` (543 linhas) é a central de controlo operacional da produção industrial e transformação do Multicore ERP:
- Gestão de Ordens de Produção (OPs) e ciclo de fabrico (Planeada $\rightarrow$ Em Produção $\rightarrow$ Concluída / Cancelada).
- Gestão de Fichas Técnicas (BOM - Bill of Materials) e fórmulas de matéria-prima.
- Apuramento do Custo Industrial do Produto (CIP) e custos unitários.
- Apontamento ágil por leitor óptico (`BarcodeScannerCollectorDialog` no modo `MANUFACTURING`).
- Emissão de etiquetas de rastreabilidade de lote e validade (`ShelfLabelsDialog.showWithBatch`).

### Problemas Arquiteturais Identificados no Monólito:
1. **Acumulação de Diálogos Modais no Painel:** O painel instancia internamente a lógica de construção e validação do formulário modal de emissão de novas OPs (`openNewOrderDialog` - 97 linhas), o formulário de definição de novas fichas técnicas (`openNewBomDialog` - 95 linhas), e o painel de acções de transição de estado da ordem (`showOrderActions` - 80 linhas).
2. **Mistura de Orquestração Visual e Operações de Domínio:** A renderização de KPIs e tabelas está acoplada com regras de negócio de fabrico (avaliação de alçadas, validação de armazéns de matéria-prima e produto acabado, e chamada a clientes HTTP de produção, comercial e inventário).
3. **Manutenção e Testabilidade:** O painel ultrapassa o limite canónico corporativo recomendado, dificultando testes unitários focados e auditorias ergonómicas.

---

## 2. Decisão de Arquitetura e Estrutura Decomposta

Seguindo o padrão canónico do Multicore ERP aplicado com sucesso nas Fases 1 a 16:
1. **`ManufacturingPanel.java` (Orquestrador Magro $\le 260$ linhas):**
   - Mantém estritamente o layout corporativo com cabeçalho, `InlineFeedbackPanel feedback`, grelha de 4 KPIs via `KpiCard.createGrid(4)` e card `ModernPanel(16)` contendo filtros (`TableFilter.bar`) e tabela `ordersTable`.
   - Preserva os campos package-private essenciais exigidos por testes canónicos: `ordersTable` e `feedback`.
   - Preserva o método package-private `showOrderActions()`, garantindo o aviso do operador quando nenhuma linha está selecionada (`GuiActionAlertGuidanceHarnessTest`).
   - Delega a criação de OPs, transições de estado, emissão de etiquetas e coletor de código de barras para o coordenador `ProductionOrderOperationsCoordinator`.
   - Delega a criação de fichas técnicas para o diálogo especializado `BomFormDialog`.

2. **`mz.multicore.erp.gui.ProductionOrderOperationsCoordinator.java` ($\le 260$ linhas):**
   - Responsabilidade única de coordenação de operações de ordens de fabrico:
     - `openNewOrderDialog(...)`: Construção, validação semântica e gravação de novas OPs via `ModernFormDialog`.
     - `showOrderManagementDialog(...)`: Modal de transição de estado (Iniciar produção, Concluir produção & Dar entrada em stock, Cancelar ordem) e emissão de etiquetas.
     - `openProductionLabels(...)` e `printOrderLabels(...)`: Emissão de etiquetas térmicas e A4 de lote de produção com `ShelfLabelsDialog`.
     - `openBarcodeCollector(...)`: Abertura do coletor com leitor USB/Bluetooth via `BarcodeScannerCollectorDialog`.

3. **`mz.multicore.erp.gui.BomFormDialog.java` ($\le 130$ linhas):**
   - Diálogo modal corporativo para registo de Fichas Técnicas (BOM):
     - Seleção de produto acabado a partir do catálogo em cache.
     - Quantidade base, custos estimados de mão de obra e custos gerais.
     - Atribuição de componente padrão e submissão atómica através de `ModernFormDialog` com validação de campos obrigatórios sem fecho prematuro da janela.

---

## 3. Preservação Estrita de Contratos e Restrições Não-Negociáveis

1. **Construtores Canónicos:**
   - Preservar `public ManufacturingPanel(ProductionApiClient productionApiClient, ComercialApiClient comercialApiClient, InventoryApiClient inventoryApiClient)`.
   - Preservar o ciclo de vida `onPanelSelected()`.
2. **Campos Package-Private de Teste:**
   - `ordersTable`: `final JTable ordersTable;`
   - `feedback`: `final InlineFeedbackPanel feedback = new InlineFeedbackPanel();`
   - `showOrderActions()`: invocado por duplo clique/Enter via client property `customDetailAction` e testado diretamente por `GuiActionAlertGuidanceHarnessTest`.
3. **Ergonomia e UI Corporativa:**
   - Proibido qualquer diálogo `JOptionPane`. Apenas `InlineFeedbackPanel`, `ModernFormDialog` e `ModernMessageDialog`.
   - Sem emojis Unicode em títulos, labels ou botões.
   - Tabela encapsulada no card `ModernPanel(16)` via `UIHelper.tableCardTop`.
   - Cores semânticas FontAwesome vibrantes nos botões e abas.

---

## 4. Métricas de Sucesso

| Componente | Linhas Anteriores | Linhas Alvo | Redução Esperada |
|---|---|---|---|
| `ManufacturingPanel.java` | 543 | $\le 260$ | $\ge 52\%$ |
| `ProductionOrderOperationsCoordinator.java` | 0 | $\le 260$ | Novo colaborador |
| `BomFormDialog.java` | 0 | $\le 130$ | Novo diálogo |
| **Erros de Teste / Regressões** | 0 | 0 | 100% aprovado |
