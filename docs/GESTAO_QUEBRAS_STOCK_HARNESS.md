# Harness de Conformidade — Gestão e Aprovação de Quebras de Stock

**Código:** `GESTAO_QUEBRAS_STOCK_HARNESS`  
**Documento de Referência:** `docs/GESTAO_QUEBRAS_STOCK_SPEC.md`  
**Suite de Testes:** `StockWastePanelHarnessTest.java`  

---

## 1. Matriz de Critérios de Conformidade

| ID | Requisito / Comportamento | Resultado Esperado | Cobertura Automatizada |
|----|---------------------------|-------------------|------------------------|
| **GQS-01** | Inicialização do Painel de Quebras | Os 3 separadores (`Registo & Validação`, `Radar`, `Métricas`) e os 4 KPIs de topo são instanciados corretamente. | `testPanelTabsAndKpisInitialized` |
| **GQS-02** | Filtragem por Estado | Filtrar por `Pendente`, `Aprovado` ou `Rejeitado` restringe a tabela aos registos correspondentes. | `testFilterByStatus` |
| **GQS-03** | Filtragem por Motivo | Filtrar por motivo (ex.: `EXPIRED`, `DAMAGED`) exibe apenas as quebras associadas. | `testFilterByReason` |
| **GQS-04** | Filtragem Universal por Período | O combo de período universal (`Hoje`, `Esta semana`, `Este mês`, etc.) filtra a tabela pela data de registo da quebra. | `testFilterByPeriod` |
| **GQS-05** | Habilitação dos Botões de Aprovação/Rejeição | Botões `approveBtn` e `rejectBtn` só ficam ativos (`setEnabled(true)`) quando uma quebra `PENDING` está selecionada. | `testApprovalButtonsEnabledOnlyForPending` |
| **GQS-06** | Bloqueio de Quebras Já Decididas | Linhas com estado `APPROVED` ou `REJECTED` mantêm os botões de ação desativados. | `testApprovalButtonsDisabledForDecidedRows` |
| **GQS-07** | Radar de Validades | O radar classifica corretamente os lotes por prazo de validade (vencidos, críticos e sob atenção). | `testRadarClassification` |
| **GQS-08** | Decomposição de Painel | `StockWastePanel.java` permanece estritamente abaixo do limite de 1000 linhas. | `testPanelLineCountUnderOneThousand` |

---

## 2. Execução Automatizada

```powershell
mvn test -pl desktop "-Dtest=StockWastePanelHarnessTest,UiPanelDecompositionTest,FinalUiUniformityHarnessTest"
```
