# Especificação Canónica — Decomposição do CommercialMovementsPanel

**ID:** `SPEC-COMMERCIAL-MOVEMENTS-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / Movimentos & Documentos Comerciais Globais  
**Alvo:** `mz.multicore.erp.gui.commercial.CommercialMovementsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui.commercial`)  

---

## 1. Objectivo & Diagnóstico

O `CommercialMovementsPanel` constitui a central unificada de consulta, auditoria e exportação de todos os movimentos e documentos comerciais e operacionais (Faturas, Vendas POS, Cotações, Encomendas, Guias de Remessa, Notas de Crédito/Débito, Recibos e Transferências).

### Diagnóstico de Complexidade
- **Monolitismo de Agregação e Dados:** Acumulação de 368 linhas contendo carregamento e merge assíncrono de armazéns (`loadWarehouses`), povoamento dinâmico de artigos e armazéns com árvores case-insensitive (`TreeSet`), preenchimento da tabela de 9 colunas (`apply`), e cálculo reativo/agregado de rodapé (`updateFooter`) com contagem, quantidade total movimentada e valor monetário global.
- **Coordenação de Ações & QuickPeek:** Configuração estática do controlador de visualização rápida (`TableQuickPeekController`) e montagem condicional de títulos de impressão/exportação com múltiplos filtros ativos.
- **Tamanho original:** 368 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

A decomposição segue o padrão corporativo com desacoplamento rigoroso entre orquestração visual, processamento de dados e operações:

1. **`CommercialMovementsPanel` (Orquestrador / View Principal):**
   - Construção do card principal `ModernPanel(16)` contendo barra de filtros unificada em linha única via `TableFilter.bar(...)`.
   - Manutenção dos 3 construtores sobrecarregados (1, 2 e 3 parâmetros) preservando total retrocompatibilidade com `ComercialPanel`.
   - Tabela estilizada de 9 colunas com renderizadores de status, quantidade e moeda (`TableCellRenderers`).
   - Paginação via `ClientTablePagination.install(table)` e rodapé informativo integrado.
   - Meta de linhas: $\le 225$ linhas.

2. **`CommercialMovementsDataCoordinator` (`mz.multicore.erp.gui.commercial`):**
   - Povoamento da tabela e extração dinâmica de filtros de produtos e armazéns (`populateModelAndFilters`).
   - Sincronização assíncrona de armazéns corporativos com merge inteligente de transferências `Origem → Destino` (`syncWarehouses`).
   - Filtro analítico e recálculo em tempo real de métricas no rodapé (`calculateSummary` e `formatSummary`).

3. **`CommercialMovementsOperationsCoordinator` (`mz.multicore.erp.gui.commercial`):**
   - Configuração do Quick Peek lateral (`populatePeek`) com detalhes completos do documento, entidade, estado e totais.
   - Construção contextual do cabeçalho de exportação baseado nos filtros activos e despacho oficial para `TableExportAction.export(...)`.

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Construtores Canónicos:** Preservação estrita dos 3 construtores:
  - `CommercialMovementsPanel(MovimentosApiClient)`
  - `CommercialMovementsPanel(MovimentosApiClient, PrintApiClient)`
  - `CommercialMovementsPanel(MovimentosApiClient, PrintApiClient, InventoryApiClient)`
- **Contratos de Teste Existentes:**
  - `CommercialCrmUnifiedHarnessTest` (COMM-02): Presença obrigatória de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)` e ausência de `new JPanel(new GridBagLayout())`.
  - `TableActionPlacementHarnessTest`: Presença obrigatória de `UIHelper.tableCardTop(`.
- **Invariantes de Formatação & Localização:**
  - Formatação monetária e de quantidades tolerante e independente do Locale nativo do sistema operacional (`Locale.US` ou `pt-MZ`).
