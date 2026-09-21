# Critérios de Conformidade e Testes — Pacote de Excelência Executiva

Este documento define os critérios de conformidade e testes canónicos automatizados (harness) para as 6 melhorias executivas e de produtividade no Multicore ERP.

---

## Tabela de Critérios de Conformidade (EX-01 a EX-07)

| ID | Componente Alvo | Requisito Verificado | Critério de Sucesso |
|---|---|---|---|
| **EX-01** | `SmartAlertsDialog` & `SmartAlertsModel` | Classificação de alertas por criticidade (Crítico, Atenção, Informativo) | Alertas com `qty < minStock` ou datas vencidas são marcados como CRITICAL; itens com < 30 dias marcados como WARNING; ações diretas configuradas. |
| **EX-02** | `PosDirectPrintEngine` | Configuração e despacho de impressão direta no POS | `PosDirectPrintEngine.isDirectPrintEnabled()` alterna estado; `printDirectReceipt` despacha para `PdfPrinter` e exibe toast sem abrir modal. |
| **EX-03** | `ProfitAnalyticsWidget` & `ProfitEngine` | Cálculo de DRE, CMVMC, Lucro Bruto e Margem % | Fórmulas matemáticas de `ProfitEngine.calculateProfit(...)` calculam corretamente margem %, lucro bruto e ticket médio sem arredondamentos espúrios. |
| **EX-04** | `MultiCurrencyEngine` & `CurrencyExchangeDialog` | Conversão cambial e troco no POS (USD, ZAR, EUR) | Conversão de MZN para moeda estrangeira e cálculo do troco devolvido em Meticais com precisão decimal estrita (`BigDecimal`). |
| **EX-05** | `LoyaltyEngine` | Regras de pontuação de fidelização e resgate de pontos | Geração de 1 ponto a cada 100 MT; resgate com teto máximo limitado ao valor total da compra. |
| **EX-06** | `DatabaseBackupDialog` | Interface e verificação de backup | Interação com `BackupApiClient`, exibição de status, arquivos existentes e validação estrutural sem bloquear o EDT. |
| **EX-07** | `ExecutiveSuiteHarnessTest` | Execução integrada sem quebra de arquitetura ou limites de linhas | Todos os testes passam em ambiente headless e arquivos UI respeitam o teto de 1000 linhas. |
