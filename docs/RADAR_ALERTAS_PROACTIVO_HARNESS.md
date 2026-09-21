# Critérios de Conformidade & Harness de Testes: Radar de Alertas Inteligentes & Risco Proactivo

**Identificador:** `HARNESS-RAP-001`  
**Referência:** [`docs/RADAR_ALERTAS_PROACTIVO_SPEC.md`](file:///c:/Users/miran/Desktop/manager/docs/RADAR_ALERTAS_PROACTIVO_SPEC.md)  
**Versão:** `1.0.0`  
**Data:** `2026-09-16`  

---

## 1. Matriz de Requisitos Automatizados

| ID | Cenário / Regra | Critério de Aceitação | Verificação Automatizada |
|---|---|---|---|
| **RAP-01** | Injeção & Retrocompatibilidade do `NotificationFeed` | `NotificationFeed` aceita opcionalmente `CreditRiskApiClient` e `StockWasteApiClient`, mantendo compatibilidade com construtores pré-existentes. | `testConstructorsAndBackwardsCompatibility()` |
| **RAP-02** | Alerta de Risco de Crédito Crítico | Quando existem clientes com `CRITICAL` ou bloqueados, gera `NotificationItem` de tipo `"Risco de Crédito"`, prioridade `3`, detalhe com total em mora e módulo `risco_credito`. | `testCreditRiskCriticalAlertGenerated()` |
| **RAP-03** | Alerta de Quebras Pendentes | Quando existem quebras de stock com estado `PENDING_APPROVAL`, gera `NotificationItem` de tipo `"Quebras de Stock"`, prioridade `2` ou `3`, detalhe com montante de perda e módulo `stock_waste`. | `testPendingStockWasteAlertGenerated()` |
| **RAP-04** | Mapeamento no `SmartAlertsDialog` | `SmartAlert.fromNotification(...)` mapeia `risco_credito` para ação `"Cobrar / Ver Risco"` e `stock_waste` para `"Aprovar Quebras"`. | `testSmartAlertActionMapping()` |
| **RAP-05** | Tolerância a Falhas e Degradação Suave | Se o cliente de risco ou quebras lançar `RuntimeException` / falha de rede, o método `load(companyId)` conclui sem exceção, retornando os demais alertas. | `testGracefulDegradationOnClientError()` |
| **RAP-06** | Navegação por Sub-Abas em `ClientesPanel` e `StockPanel` | `ClientesPanel.selectCreditRiskTab()` e `StockPanel.selectWasteTab()` alternam a visualização para a aba de trabalho correta. | `testSubTabSelectionNavigation()` |
| **RAP-07** | Decomposição de Linhas & Design System | `StockPanel.java` e `MainFrame.java` mantêm-se abaixo ou igual ao teto de 1000 linhas, e `FinalUiUniformityHarnessTest` continua 100% verde. | `testLineCountAndVisualUniformity()` |
