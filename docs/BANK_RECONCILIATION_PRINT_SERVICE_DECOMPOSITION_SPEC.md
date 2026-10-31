# Especificação de Decomposição: BankReconciliationPrintService (Fase 152)

## 1. Diagnóstico do Monólito
`BankReconciliationPrintService.java` possuía **232 linhas**, misturando:
- Validação de empresa e carregamento do extrato bancário e movimentos (`BankReconciliationService`).
- Renderização OpenPDF de cabeçalho, metadados da conta, cartões KPI de saldos/entradas/saídas, tabela de movimentos e assinaturas.

## 2. Arquitectura Alvo
1. `BankReconciliationPrintService` (`<= 55` linhas): Fachada `@Service` que valida empresa, carrega o extrato e movimentos e delega ao renderer.
2. `BankReconciliationDocumentRenderer` (`<= 195` linhas): Componente `@Component` que constrói o PDF A4 de reconciliação bancária.
