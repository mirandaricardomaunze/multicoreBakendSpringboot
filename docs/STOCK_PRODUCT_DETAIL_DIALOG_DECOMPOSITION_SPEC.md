# Especificação de Decomposição Modular — StockProductDetailDialog (Fase 83)

## 1. Contexto e Diagnóstico

A classe `StockProductDetailDialog.java` no módulo `desktop` acumulava 430 linhas, misturando num único ficheiro:
1. Orquestração do modal executivo e ciclo de vida visual da ficha de produto.
2. Construção do Hero com visualização e zoom modal em alta definição de imagem de produto (`buildImageCard`, `openFullImageModal`).
3. Construção dos cartões de KPI consolidados com cálculo de margens, composição de embalagens e preços (`buildKpiGrid`).
4. Construção das abas de detalhe:
   - Especificações técnicas e logística via folha de propriedades (`RecordDetailsDialog.PropertyItem`).
   - Saldos discriminados por armazém com cálculo de caixas (`buildWarehousesTab`).
   - Rastreabilidade de lotes com cálculo temporal de expiração e criticidade (`buildBatchesTab`).

## 2. Princípios Arquiteturais e Restrições Não Negociáveis

1. **Separação de Camadas (SOLID / SRP):**
   - `StockProductDetailDialog`: Orquestrador leve do modal ($\le 160$ linhas).
   - `StockProductDetailHeroBuilder`: Construtor do bloco hero (imagem HD com modal de ampliação e grelha padronizada de KPIs).
   - `StockProductDetailTabsBuilder`: Construtor das abas corporativas (especificações, saldos por armazém e lotes/validades).
2. **Design Corporativo e Ergonómico:**
   - Dimensões estritamente contidas via `UIHelper.applyStandardModalBounds(dialog, 960, 610)`.
   - Modais com foco e tecla ESC automática.
   - Tabelas de saldos e lotes com `ClientTablePagination.DISABLED` para não espremer a altura vertical.
   - Cartões de KPI com `STANDARD_CARD_HEIGHT = 96px`.
3. **Internacionalização e Formatação:**
   - Vocabulário e mensagens em português de Moçambique (pt-MZ).
   - Zero `JOptionPane`.
   - Comparação com zero usando `val.compareTo(BigDecimal.ZERO) == 0` ou `val.signum() == 0`.
4. **Compatibilidade Total:**
   - Preservar construtor público `StockProductDetailDialog(StockPanel owner, ProductDTO product)`, método `showDialog()` e `showFor(StockPanel owner, Long productId)`.
