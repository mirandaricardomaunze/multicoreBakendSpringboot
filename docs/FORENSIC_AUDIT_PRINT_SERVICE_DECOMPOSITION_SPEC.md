# Especificação de Decomposição: ForensicAuditPrintService (Fase 144)

## 1. Diagnóstico do Monólito
`ForensicAuditPrintService.java` possuía **244 linhas**, misturando:
- Validação de empresa e obtenção do resumo forense (`ForensicAuditService`).
- Renderização OpenPDF de cabeçalho oficial, metadados, cartões de KPI de risco, banner de conformidade, tabela de anomalias e assinaturas.

## 2. Arquitectura Alvo
1. `ForensicAuditPrintService` (`<= 60` linhas): Fachada `@Service` que valida a empresa, obtém o sumário forense e delega ao renderer.
2. `ForensicAuditDocumentRenderer` (`<= 200` linhas): Componente `@Component` responsável pela composição do documento PDF A4 de auditoria forense.
