# Harness de Conformidade: Central de Auditoria Forense & Controlo de Fraude Interna

- **Identificador:** `HARNESS-AFF-001`
- **Referência:** `SPEC-AFF-001`
- **Módulo:** `backend`, `desktop`
- **Data:** 2026-09-17

---

## 1. Critérios de Validação Automatizada

| ID | Critério | Verificação |
| :--- | :--- | :--- |
| **AFF-01** | **Deteção de Cancelamentos de Fatura** | Faturas canceladas (`InvoiceStatus.CANCELLED`) devem ser detetadas com severidade `CRITICAL` se valor $> 5.000$ MT ou sem motivo, e `SUSPICIOUS` nos restantes casos. |
| **AFF-02** | **Deteção de Descontos Excessivos** | Faturas ou linhas com descontos $> 10\%$ do total devem ser classificadas como anomalias com categoria `EXCESSIVE_DISCOUNT`. |
| **AFF-03** | **Deteção de Quebras de Stock Anormais** | Registos de quebra de stock (`StockWaste`) com custo $> 10.000$ MT devem receber severidade `CRITICAL`; perdas entre $3.000$ e $10.000$ MT devem ser `SUSPICIOUS`. |
| **AFF-04** | **Cálculo da Exposição Financeira Total** | O total de risco financeiro (`totalFinancialRisk`) deve ser a soma exata dos impactos de ocorrências `CRITICAL` e `SUSPICIOUS`. |
| **AFF-05** | **Índice de Conformidade (Compliance Score)** | O índice percentual e classificação semântica devem obedecer rigorosamente às faixas de risco definidas no SPEC-AFF-001. |
| **AFF-06** | **Geração de PDF do Dossiê Forense** | O endpoint `/api/audit/forensic/pdf` deve devolver um PDF válido (iniciando com `%PDF-1.`) com cabeçalho oficial, cartões de risco e matriz de anomalias. |
| **AFF-07** | **Interface Executiva e Limite de Linhas** | `ForensicAuditPanel.java` deve renderizar os KPIs de risco, filtros por severidade/categoria, tabela zebrada e respeitar estritamente o teto de 1000 linhas e tokens do `UIHelper`. |

---

## 2. Implementação dos Testes
- Backend: `ForensicAuditHarnessTest.java` (cobrindo AFF-01 a AFF-06).
- Desktop: `ForensicAuditPanelHarnessTest.java` (cobrindo AFF-07).
