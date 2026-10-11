# Especificação Técnica de Padronização — Fase 3: Stock & Armazéns

## 1. Contexto e Diagnóstico

O módulo de Stock e Armazéns do Multicore ERP é crítico para as operações diárias de inventário, controlo de lotes, datas de validade, quebras e gestão multi-armazém.

### Problemas Encontrados na Auditoria
1. **Filtros empilhados com `GridBagLayout`**: Vários ecrãs (`StockPanel.java`, `StockBatchesPanel.java`, `StockAlertsPanel.java`, `StockWastePanel.java`, `StockWarehousesPanel.java`) utilizavam formulários verticais de filtro com labels no topo e inputs em baixo, consumindo 60px a 90px de espaço vertical útil da tabela.
2. **Larguras de Coluna sem Calibração**: Tabelas de movimentos, lotes, armazéns e quebras sem `UIHelper.ensureHeadersFit(table)`, causando truncagem de títulos e dados.
3. **Ausência de Quick Peek**: O operador necessita inspecionar rapidamente detalhes de produtos, lotes, armazéns e quebras sem abrir diálogos modais pesados.
4. **Uniformidade de Cartões**: Garantir que todas as tabelas residam estritamente num `ModernPanel(16)` padronizado e com topo via `UIHelper.tableCardTop(...)` ou `TableFilter.bar(...)`.

---

## 2. Painéis Abrangidos

| Painel | Ficheiro | Alterações Principais |
| :--- | :--- | :--- |
| **Stock Geral** | `StockPanel.java` | 3 abas (Níveis, Movimentos, Transferências): substituição de `GridBagLayout` por `TableFilter.bar`, `ensureHeadersFit` nas 3 tabelas, Quick Peek em `stockTable`. |
| **Lotes & Validades** | `StockBatchesPanel.java` | Linha única de filtros (`TableFilter.bar`), `ensureHeadersFit`, Quick Peek no lote selecionado. |
| **Alertas de Stock** | `StockAlertsPanel.java` | Abas de Validades e Ruturas com filtros horizontais `TableFilter.bar`, `ensureHeadersFit` em ambas as tabelas. |
| **Quebras & Desperdício** | `StockWastePanel.java` | Linha única de filtros em Quebras e Radar de Desperdício, `ensureHeadersFit`, Quick Peek. |
| **Armazéns** | `StockWarehousesPanel.java` | Linha única de filtros horizontal, `ensureHeadersFit`, Quick Peek no armazém. |
| **Categorias de Stock** | `StockCategoriesPanel.java` | Barra de pesquisa e filtro compacta, `ensureHeadersFit`. |

---

## 3. Regras Canónicas Aplicadas

1. **Card de Tabela:** Cartão com raio `16` (`ModernPanel(16)`). Título e filtros estritamente integrados.
2. **Filtros Horizontais:** Proibido `GridBagLayout` vertical com rótulo acima de cada campo. Usar `TableFilter.bar(Component...)`.
3. **Calibração de Tabela:** Todas as tabelas executam `UIHelper.ensureHeadersFit(table)` após montagem ou carregamento de dados.
4. **Quick Peek Silencioso:** Drawer lateral de 340px (`QuickPeekPanel`) e atalho de teclado `Espaço` com `TableQuickPeekController`.
5. **Reticências em Botões:** Nenhum botão exibe reticências (`…` ou `...`).

---

## 4. Testes de Harness Automatizados

O teste `mz.multicore.erp.gui.StockUnifiedHarnessTest` no módulo `desktop` assegura:
- `StockPanel`: Ausência de `GridBagLayout` nos filtros de abas; presença de `ensureHeadersFit` e `TableQuickPeekController`.
- `StockBatchesPanel`, `StockAlertsPanel`, `StockWastePanel`, `StockWarehousesPanel`, `StockCategoriesPanel`: Ausência de `GridBagLayout` nos filtros, presença de `TableFilter.bar` e `ensureHeadersFit`.
- Conectividade integral testada via `DesktopThinContextTest`.
