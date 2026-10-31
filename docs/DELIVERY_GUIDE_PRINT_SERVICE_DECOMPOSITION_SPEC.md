# Especificação de Decomposição: DeliveryGuidePrintService (Fase 143)

## 1. Diagnóstico do Monólito
`DeliveryGuidePrintService.java` possuía **247 linhas**, misturando:
- Obtenção da Guia de Remessa (`DeliveryGuideRepository`), validação de tenant e configuração de colunas (`DocumentConfigService`).
- Renderização OpenPDF de múltiplas vias (quadruplicado A4 paisagem), bloco de entrega, tabela de artigos, resumo de carga, transporte e assinaturas.

## 2. Arquitectura Alvo
1. `DeliveryGuidePrintService` (`<= 60` linhas): Fachada `@Service` que carrega a guia, valida a empresa e delega ao renderer, mantendo a constante `DELIVERY_GUIDE_COPIES`.
2. `DeliveryGuideDocumentRenderer` (`<= 210` linhas): Componente `@Component` que mapeia as linhas e constrói o PDF em quadruplicado A4 paisagem.
