# Matriz de Testes & Harness de Conformidade: Padronização dos KPI Cards
**Código:** HARNESS-KPI-001  
**Referência:** `docs/KPI_CARDS_PADRONIZACAO_SPEC.md` (SPEC-KPI-001)  
**Data:** 2026-09-17  

---

## 1. Objectivo
Validar a refatoração universal dos cartões métricos superiores nos painéis `CreditRiskPanel`, `StockWastePanel`, `CustomerStatementPanel` e `SupplierStatementPanel`, comprovando o uso de `KpiCard.createMetricCard` e a preservação rigorosa dos limites de linhas e integridade visual.

---

## 2. Critérios de Avaliação Automatizada

| ID | Área | Descrição do Teste | Critério de Aceitação |
|---|---|---|---|
| **KPI-01** | `KpiCard` Overload | Suporte a valores como `String` e como `JLabel` dinâmico | Ambos os métodos factory devolvem instâncias válidas de `ModernPanel` |
| **KPI-02** | `CreditRiskPanel` | Adoção de `KpiCard.createMetricCard` | 4 cartões superiores gerados via `KpiCard` |
| **KPI-03** | `StockWastePanel` | Adoção de `KpiCard.createMetricCard` | 4 cartões de quebras e desperdício gerados via `KpiCard` |
| **KPI-04** | `CustomerStatementPanel` | Adoção de `KpiCard.createMetricCard` | Cartões de extrato de cliente gerados via `KpiCard` |
| **KPI-05** | `SupplierStatementPanel` | Adoção de `KpiCard.createMetricCard` | Cartões de extrato de fornecedor gerados via `KpiCard` |
| **KPI-06** | Tamanho de Classes | Todos os painéis $\le 1000$ linhas | `UiPanelDecompositionTest` passa a 100% |
| **KPI-07** | Semântica de Cores | Zero violações de cores nos ficheiros editados | `FinalUiUniformityHarnessTest` passa a 100% |

---

## 3. Testes Automatizados no Desktop
- Suite de Teste Dedicada: `desktop/src/test/java/mz/multicore/erp/gui/KpiCardStandardizationTest.java`
- Verificação de Guardas: `UiPanelDecompositionTest.java` e `FinalUiUniformityHarnessTest.java`
