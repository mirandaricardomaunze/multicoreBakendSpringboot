# Especificação de Decomposição: EmploymentContractPrintService (Fase 167)

## Contexto e Problema
`EmploymentContractPrintService` (206 linhas) combinava a consulta do contrato com validação multi-empresa e a construção integral das secções PDF A4 (identificação das partes, grelha de condições acordadas, geração dinâmica de cláusulas contratuais e assinaturas).

## Solução Arquitectural
1. **`EmploymentContractPrintService` (Fachada — `<= 55` linhas)**:
   - Obtém `EmploymentContract` via `EmploymentContractRepository.findByIdWithEmployeeAndCompanyId(...)`.
   - Delega a renderização PDF para `EmploymentContractDocumentRenderer`.
   - Mantém construtor compatível `EmploymentContractPrintService(EmploymentContractRepository)`.
2. **`EmploymentContractDocumentRenderer` (`@Component` — `<= 185` linhas)**:
   - Constrói o documento A4 do contrato de trabalho com cláusulas dinâmicas e assinaturas.
