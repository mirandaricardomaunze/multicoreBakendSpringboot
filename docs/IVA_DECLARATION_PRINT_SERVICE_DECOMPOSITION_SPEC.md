# Especificação de Decomposição: IvaDeclarationPrintService (Fase 146)

## 1. Diagnóstico do Monólito
`IvaDeclarationPrintService.java` possuía **236 linhas**, misturando:
- Validação de empresa e apuramento mensal de IVA (`FiscalSummaryService`).
- Renderização OpenPDF de cabeçalho, período de apuramento, quadro de operações de saída/entrada, resultado de IVA a pagar/reportar, anexos de vendas/compras e assinaturas.

## 2. Arquitectura Alvo
1. `IvaDeclarationPrintService` (`<= 60` linhas): Fachada `@Service` que valida empresa, apura o IVA mensal e delega ao renderer.
2. `IvaDeclarationDocumentRenderer` (`<= 195` linhas): Componente `@Component` responsável pela composição do PDF A4 da Declaração Mensal de IVA.
