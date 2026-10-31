# Especificação de Decomposição: OrderPickingSheetPrintService (Fase 150)

## 1. Diagnóstico do Monólito
`OrderPickingSheetPrintService.java` possuía **232 linhas**, misturando:
- Obtenção do pedido (`OrderRepository`) e validação multi-tenant (`CurrentUserContext.requireCompany`).
- Renderização OpenPDF A4 da Guia de Separação (Picking List), tabela de artigos com cálculo de embalagens/caixas e assinaturas.

## 2. Arquitectura Alvo
1. `OrderPickingSheetPrintService` (`<= 55` linhas): Fachada `@Service` que carrega o pedido, valida a empresa e delega ao renderer.
2. `OrderPickingSheetDocumentRenderer` (`<= 200` linhas): Componente `@Component` que constrói o PDF A4 de picking list.
