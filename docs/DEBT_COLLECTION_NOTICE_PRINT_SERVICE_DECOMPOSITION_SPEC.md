# Especificação de Decomposição: DebtCollectionNoticePrintService (Fase 142)

## 1. Diagnóstico do Monólito
`DebtCollectionNoticePrintService.java` possuía **251 linhas**, misturando:
- Validação de empresa e obtenção do aviso de cobrança (`CreditRiskService`).
- Renderização OpenPDF de destinatário, introdução da carta, tabela de faturas em mora, contas bancárias, conclusão e assinaturas.

## 2. Arquitectura Alvo
1. `DebtCollectionNoticePrintService` (`<= 55` linhas): Fachada `@Service` que valida empresa, obtém o `DebtCollectionNoticeDTO` e delega ao renderer.
2. `DebtCollectionNoticeDocumentRenderer` (`<= 210` linhas): Componente `@Component` que constrói o documento PDF A4 da notificação de cobrança.
