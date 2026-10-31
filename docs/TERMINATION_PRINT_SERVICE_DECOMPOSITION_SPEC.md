# Especificação de Decomposição: TerminationPrintService (Fase 156)

## 1. Diagnóstico do Monólito
`TerminationPrintService.java` possuía **226 linhas**, misturando:
- Carregamento da cessação de contrato (`TerminationService.loadForPrint`).
- Renderização OpenPDF do Acerto Final de Contas (`renderSettlement`) e do Certificado de Trabalho (`renderCertificate`).

## 2. Arquitectura Alvo
1. `TerminationPrintService` (`<= 55` linhas): Fachada `@Service` que carrega a cessação e delega a geração do Acerto Final ou do Certificado de Trabalho.
2. `TerminationDocumentRenderer` (`<= 195` linhas): Componente `@Component` que constrói ambos os documentos PDF A4.
