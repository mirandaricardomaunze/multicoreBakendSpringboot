# Especificação de Decomposição: SupplierStatementPrintService (Fase 147)

## 1. Diagnóstico do Monólito
`SupplierStatementPrintService.java` possuía **235 linhas**, misturando:
- Validação de empresa e obtenção do extrato de fornecedor (`SupplierStatementService`).
- Renderização OpenPDF de cabeçalho, bloco de fornecedor/período, cartões de resumo de saldos, tabela de movimentos com saldo corrido, termo de circularização e assinaturas.

## 2. Arquitectura Alvo
1. `SupplierStatementPrintService` (`<= 55` linhas): Fachada `@Service` que valida empresa, obtém o `SupplierStatementDTO` e delega ao renderer.
2. `SupplierStatementDocumentRenderer` (`<= 195` linhas): Componente `@Component` responsável pela composição do documento PDF A4 do Extrato de Fornecedor.
