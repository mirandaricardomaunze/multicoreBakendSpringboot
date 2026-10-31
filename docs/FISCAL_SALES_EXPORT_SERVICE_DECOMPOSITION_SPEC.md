# Especificação de Decomposição: FiscalSalesExportService (Fase 148)

## 1. Diagnóstico do Monólito
`FiscalSalesExportService.java` possuía **235 linhas**, misturando:
- Validação de permissões/período, filtragem de faturas emitidas e agregação de totais líquidos, IVA e brutos.
- Construção manual do XML SAF-T (`<AuditFile>`, `<Header>`, `<MasterFiles>`, `<SourceDocuments>`) e escape XML.

## 2. Arquitectura Alvo
1. `FiscalSalesExportService` (`<= 90` linhas): Fachada `@Service` que valida o período, recolhe e agrega as faturas do tenant e delega a geração XML.
2. `FiscalSalesXmlBuilder` (`<= 160` linhas): Componente `@Component` que constrói o XML SAF-T e formata valores fiscais/escape.
