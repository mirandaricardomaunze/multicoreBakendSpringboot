# Especificação Canónica — Decomposição do PromotionsPanel

**ID:** `SPEC-PROMOTIONS-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / Promoções de Loja & Campanhas de Desconto  
**Alvo:** `mz.multicore.erp.gui.PromotionsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivo & Diagnóstico

O `PromotionsPanel` gerencia as campanhas promocionais de loja (descontos percentuais ou campanhas "Leve X, pague Y", por artigo ou por categoria de produto, com janela temporal de vigência e ativação/desativação).

### Diagnóstico de Complexidade
- **Acúmulo de Responsabilidades:** O painel acumulava montagem de tabela de 8 colunas com Quick Peek e paginação, barra de filtros com sincronização dinâmica de linhas de negócio, e formulário modal completo de criação de campanhas (`createPromotionDialog`) com carregamento assíncrono de catálogo e regras de sincronização entre percentagem e "Leve X, pague Y".
- **Tamanho original:** 305 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

1. **`PromotionsPanel` (Orquestrador / View Principal):**
   - Card principal `ModernPanel(16)` via `UIHelper.tableCardTop("Promoções de Loja", promoFilters, refreshBtn, toggleBtn, newBtn)`.
   - Filtros em linha única via `TableFilter.bar(...)`.
   - Invariantes obrigatórios: `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`, `ClientTablePagination.install(table)`.
   - Construtor público e métodos preservados: `PromotionsPanel(PromotionApiClient, ComercialApiClient)`, `reload()`.
   - Meta de linhas: $\le 175$ linhas.

2. **`PromotionFormDialog` (`mz.multicore.erp.gui`):**
   - Diálogo modal ergonómico baseado em `ModernFormDialog` para criação de promoções com seleção de produto ou categoria, campos atómicos de percentagem ou quantidade, janelas de data com `DateField` e submissão ao `PromotionApiClient`.

3. **`PromotionsOperationsCoordinator` (`mz.multicore.erp.gui`):**
   - Coordenador de operações visuais para formatação estruturada do benefício comercial (`benefit`) e configuração dinâmica do Quick Peek (`populatePeek`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Contratos de Teste Existentes:**
  - `CommercialCrmUnifiedHarnessTest` (COMM-01): Presença obrigatória de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table` e proibição de `promoFilters = new JPanel(new GridBagLayout())`.
  - `TableActionPlacementHarnessTest`: Presença de `UIHelper.tableCardTop(`.
  - `ProfessionalFeedbackHarnessTest`: Manutenção estrita de 0 chamadas a `JOptionPane`.
