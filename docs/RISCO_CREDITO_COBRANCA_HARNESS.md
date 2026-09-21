# Harness de Conformidade — Centro de Risco de Crédito & Cobrança Formal

**Código:** `RISCO_CREDITO_COBRANCA_HARNESS`  
**Documento de Referência:** `docs/RISCO_CREDITO_COBRANCA_SPEC.md`  
**Suite de Testes:** `CreditRiskPanelHarnessTest.java`  

---

## 1. Matriz de Critérios de Conformidade

| ID | Cenário / Regra | Comportamento Esperado | Cobertura Automatizada |
|----|-----------------|------------------------|------------------------|
| **RCC-01** | Inicialização dos KPIs e Tabela | O painel instancia os 4 cartões de KPI, a barra de filtros e a tabela com as 15 colunas operacionais. | `testPanelInitializationAndColumns` |
| **RCC-02** | Atualização dos KPIs Globais | Os valores agregados de carteira a receber, mora total, clientes bloqueados e clientes críticos são refletidos fielmente. | `testKpiUpdateFromSummary` |
| **RCC-03** | Filtragem por Nível de Risco | O seletor restringe a visualização aos clientes do escalão selecionado (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `Apenas Bloqueados`, etc.). | `testRiskLevelFilter` |
| **RCC-04** | Pesquisa Dinâmica por Texto | Filtragem em tempo real atuando sobre o nome do cliente, NUIT ou email. | `testTextSearchFilter` |
| **RCC-05** | Mapeamento das Faixas de Aging | As colunas de escalão (Corrente, 1-30d, 31-60d, 61-90d, >90d) exibem a decomposição exata da dívida. | `testAgingBucketsDataMapping` |
| **RCC-06** | Destaque Visual e Semáforo de Risco | `CreditRiskCellRenderer` aplica as cores aprovadas pelo Design System nos badges de risco e situação. | `testCellRendererVisualHierarchy` |
| **RCC-07** | Decomposição e Limite de Linhas | `CreditRiskPanel.java` mantém-se estritamente abaixo do limite de 1000 linhas. | `testPanelLineCountUnderOneThousand` |

---

## 2. Execução Automatizada

```powershell
mvn test -pl desktop "-Dtest=CreditRiskPanelHarnessTest,UiPanelDecompositionTest,FinalUiUniformityHarnessTest"
mvn test -pl backend "-Dtest=CreditRiskHarnessTest"
```
