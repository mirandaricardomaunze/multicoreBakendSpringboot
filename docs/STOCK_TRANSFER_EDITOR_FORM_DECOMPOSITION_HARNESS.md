# Documentação do Harness: Decomposição de StockTransferEditorForm

## 1. Objectivo

Documentar as garantias automatizadas fornecidas por `StockTransferEditorFormDecompositionHarnessTest` e pelos harnesses interligados (`StockTransferDraftUiHarnessTest`, `StockInTransitUiHarnessTest`, `PhcDocumentEditorHomologationHarnessTest`, `DocumentTableInteractionHarnessTest`).

## 2. Casos de Teste

1. **STED-01 (Limites Físicos de Linhas):**
   - `StockTransferEditorForm.java` $\le 240$ linhas (reduzido de 546 linhas).
   - `StockTransferEditorHeaderSection.java` $\le 85$ linhas.
   - `StockTransferEditorGridConfigurator.java` $\le 65$ linhas.
   - `StockTransferCatalogCoordinator.java` $\le 95$ linhas.
   - `StockTransferEditorRowCalculator.java` $\le 120$ linhas.
   - `StockTransferReplenishmentHelper.java` $\le 130$ linhas.
2. **STED-02 (Zero Legado JOptionPane):**
   - Todos os ficheiros da família `StockTransfer*` usam notificações não intrusivas `owner.showStockNotice` ou beeps nativos sem `JOptionPane`.
3. **STED-03 (Delegação Canónica para Colaboradores):**
   - Valida que `StockTransferEditorForm` delega recálculo, totais, montagem de cabeçalho, catálogo e configuração da grelha para os componentes especializados.
4. **STED-04 (Preservação Integral de Contratos):**
   - Preservação de nomes de colunas, opt-in de stock em trânsito, cálculo de caixas/embalagens, métodos de edição e atalhos de teclado.
