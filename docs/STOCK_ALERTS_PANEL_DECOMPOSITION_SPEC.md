# SPEC-STOCK-ALERTS-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Alertas de Stock

## 1. Contexto & Diagnóstico
O painel `StockAlertsPanel.java` (253 linhas) gere a monitorização proativa de ruturas de stock e lotes expirados ou a expirar em $\le 30$ dias, estruturado em duas sub-abas ergonómicas: "Validade (expirados / a expirar)" e "Esgotados".

### Problemas Identificados
1. **Acoplamento de Mapeamento e Apuramento de Indicadores no Orquestrador:**
   - O orquestrador realiza a iteração e classificação de lotes vencidos vs a expirar, mascaramento seguro de stocks trancados (`MASK = "•••"`), reconstrução dinâmica dos seletores de unidades de negócio (`expBusinessUnitCombo` e `outBusinessUnitCombo`), e totalização dos sumários textuais.
2. **Duplicação de Lógica de Renderização e Quick Peek:**
   - O renderizador condicional com semáforo de expiração e o inspetor lateral Quick Peek estão incorporados na inicialização da UI.

## 2. Objectivos da Refatoração
1. **Redução de Linhas do Orquestrador:** Reduzir `StockAlertsPanel.java` de 253 linhas para $\le 165$ linhas (-38%).
2. **Extração de Coordenador de Apresentação:**
   - `StockAlertsViewCoordinator.java` ($\le 120$ linhas): Coordenador responsável pelo processamento e povoamento dos alertas (`applyAlerts`), gestão de unidades de negócio, renderizador de cores para validades e Quick Peek lateral de artigos esgotados.
3. **Erradicação Absoluta de `JOptionPane`:** Zero ocorrências mantidas em todos os ficheiros de stock.
4. **Preservação Integral de Layout e Contratos:**
   - Construtor package-private `StockAlertsPanel(StockPanel owner)`.
   - Métodos públicos `buildPanel()` e `refresh()`.
   - Sub-abas canónicas com `UIHelper.styleTabbedPaneMulticore(sub)`.
   - `UIHelper.tableCardTop("Lotes Expirados e a Expirar", expHeaderPanel, refreshExpBtn)`.
   - `UIHelper.tableCardTop("Produtos Esgotados", outHeaderPanel, refreshOutBtn)`.
   - `UIHelper.ensureHeadersFit(owner.alertsExpTable)`.
   - `UIHelper.ensureHeadersFit(owner.alertsOutTable)`.
   - `TableFilter.bar(`.
   - Zero `GridBagLayout` em formulários de filtros.
