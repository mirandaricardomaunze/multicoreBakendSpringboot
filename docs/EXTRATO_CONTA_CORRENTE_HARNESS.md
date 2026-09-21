# Critérios de Conformidade & Harness de Testes: Extrato de Conta Corrente

**Identificador:** `HARNESS-ECC-001`  
**Referência:** [`docs/EXTRATO_CONTA_CORRENTE_SPEC.md`](file:///c:/Users/miran/Desktop/manager/docs/EXTRATO_CONTA_CORRENTE_SPEC.md)  
**Versão:** `1.0.0`  
**Data:** `2026-09-16`  

---

## 1. Matriz de Requisitos Automatizados

| ID | Cenário / Regra | Critério de Aceitação | Verificação Automatizada |
|---|---|---|---|
| **ECC-01** | Contratos de DTO Imutáveis | `CustomerStatementDTO`, `CustomerStatementLineDTO`, `SupplierStatementDTO` e `SupplierStatementLineDTO` são records Java puros em `contracts`, sem dependências a JPA ou Swing. | `testStatementDtoStructureAndImmutability()` |
| **ECC-02** | Cálculo Progressivo de Clientes | Para uma sequência de FT (10.000 MT), RC (4.000 MT), NC (1.000 MT) e ND (500 MT), o saldo progressivo segue estritamente 10.000 -> 6.000 -> 5.000 -> 5.500 MT. | `testCustomerRunningBalanceCalculation()` |
| **ECC-03** | Consolidação do Saldo Anterior ($S_0$) | Movimentos anteriores a `startDate` são consolidados num único saldo de abertura (`openingBalance`), e as linhas devolvidas compreendem apenas o intervalo `[startDate, endDate]`. | `testOpeningBalanceConsolidation()` |
| **ECC-04** | Saldo Progressivo de Fornecedores | Para compra V/FT (20.000 MT) e pagamento PG (15.000 MT), o saldo em dívida a pagar progride de 20.000 para 5.000 MT. | `testSupplierRunningBalanceCalculation()` |
| **ECC-05** | Emissão de PDF Canónico de Reconciliação | `CustomerStatementPrintService` gera documento PDF A4 contendo cabeçalho institucional, tabela de transações, resumo e termo formal de reconciliação de saldos. | `testCustomerStatementPdfGeneration()` |
| **ECC-06** | Endpoints REST com Suporte Multi-Tenant | Endpoints `/api/comercial/statements/*` e `/api/purchases/statements/*` filtram rigorosamente por `company_id` do utilizador autenticado e devolvem HTTP 200. | `testStatementEndpointsMultiTenantIsolation()` |
| **ECC-07** | Integração em Painéis Swing e Limite de Linhas | `ClientesPanel.java` e `ComprasPanel.java` integram abas de conta corrente sem ultrapassar o limite de 1000 linhas por ficheiro, preservando `UiPanelDecompositionTest`. | `testPanelDecompositionAndStatementUi()` |
