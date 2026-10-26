# Especificação Canónica — Decomposição do DeliveryGuidesPanel

**ID:** `SPEC-DELIVERY-GUIDES-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / Guias de Remessa & Expedição  
**Alvo:** `mz.multicore.erp.gui.commercial.DeliveryGuidesPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui.commercial`)  

---

## 1. Objectivo & Diagnóstico

O `DeliveryGuidesPanel` é o painel central da gestão de guias de remessa do módulo Comercial, gerenciando o ciclo de vida de expedição de mercadorias para clientes, emissão de guias avulsas, consulta de volumes/embalagens e aprovação com baixa atómica de stock.

### Diagnóstico de Complexidade
- **Uso Residual de `JOptionPane`:** Presença de duas confirmações com `JOptionPane.showConfirmDialog` (aprovação com saída definitiva de stock e cancelamento de guia).
- **Acúmulo de Operações:** O painel acumulava montagem de tabela de 11 colunas com coluna oculta de ID, paginação e quick peek, aprovação, rejeição com motivo obrigatório, cancelamento seguro, impressão sensível/relatório e modal de caixas e unidades (`showPackages`).
- **Tamanho original:** 328 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

A decomposição segue o padrão corporativo canónico:

1. **`DeliveryGuidesPanel` (Orquestrador / View Principal):**
   - Construção do card principal `ModernPanel(16)` contendo barra de filtros unificada em linha única via `TableFilter.bar(...)`.
   - Manutenção rigorosa dos 4 construtores sobrecarregados (2, 3, 5 e 6 parâmetros) e atalhos F3.
   - Preservação estrita dos menus semânticos `ActionMenuButton` contendo "Aprovar", "Rejeitar", "Cancelar".
   - Eliminação total de `JOptionPane` em favor de `UIHelper.confirm`.
   - Meta de linhas: $\le 250$ linhas.

2. **`DeliveryGuidesOperationsCoordinator` (`mz.multicore.erp.gui.commercial`):**
   - Coordenador de operações assíncronas para aprovação segura com saída definitiva de stock e confirmação via `UIHelper.confirm`.
   - Rejeição com motivo obrigatório via `UIHelper.promptRequiredText`.
   - Cancelamento com confirmação corporativa via `UIHelper.confirm`.
   - Impressão sensível oficial (`SensitivePrintCoordinator.print` / `PrintPreviewDialog.show`).
   - Modal corporativo de inspeção de embalagens multiníveis e caixas (`showPackagesModal`).
   - Configuração dinâmica de Quick Peek (`populatePeek`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Construtores Canónicos:** Preservação estrita dos 4 construtores:
  - `DeliveryGuidesPanel(ComercialApiClient, Runnable)`
  - `DeliveryGuidesPanel(ComercialApiClient, Runnable, Runnable)`
  - `DeliveryGuidesPanel(ComercialApiClient, Runnable, Runnable, Supplier<List<ClientDTO>>, Supplier<List<WarehouseDTO>>)`
  - `DeliveryGuidesPanel(ComercialApiClient, Runnable, Runnable, Supplier<List<ClientDTO>>, Supplier<List<WarehouseDTO>>, PrintApiClient)`
- **Contratos de Teste Existentes:**
  - `StockCommercialUiHarnessTest`: Presença literal de `ActionMenuButton`, "Aprovar", "Rejeitar", "Cancelar".
  - `CommercialOperacionalUnifiedHarnessTest` (COMM-OP-02): `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`.
  - `CommercialMultiuserRefreshHarnessTest`: `createRefreshButton(this::refresh)`.
  - `TableFilterHarnessTest`: Presença de `PeriodFilter` e `periodCombo`.
  - `TableActionPlacementHarnessTest`: Presença de `UIHelper.tableCardTop(`.
  - `ProfessionalFeedbackHarnessTest`: Redução a 0 chamadas directas a `JOptionPane` e remoção da allowlist.
