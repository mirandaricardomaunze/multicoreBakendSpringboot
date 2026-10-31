# Especificação de Decomposição: QualityEquipmentPrintService (Fase 158)

## 1. Diagnóstico do Monólito
`QualityEquipmentPrintService.java` possuía **223 linhas**, misturando:
- Validação de atribuição de EPI, resolução de `companyId` e carregamento (`WorkerEquipmentAssignmentRepository`).
- Renderização OpenPDF do Termo de Entrega e Responsabilidade de EPI (identificação do colaborador, especificações, vigência/validade, termo legal e assinaturas).

## 2. Arquitectura Alvo
1. `QualityEquipmentPrintService` (`<= 55` linhas): Fachada `@Service` que carrega a atribuição e delega ao renderer.
2. `QualityEquipmentDocumentRenderer` (`<= 190` linhas): Componente `@Component` responsável pela composição PDF A4 do termo de entrega de EPI.
