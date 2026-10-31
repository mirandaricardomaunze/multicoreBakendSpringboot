# Harness de Decomposição do PayslipPrintService

O `PayslipPrintServiceDecompositionHarnessTest` protege permanentemente a fase 124:

- fachada limitada a 60 linhas e obrigada a delegar para `PayslipDocumentRenderer`;
- presença dos dois colaboradores especializados;
- compositor do documento limitado a 220 linhas;
- renderer remuneratório limitado a 150 linhas;
- acesso a descontos restrito ao renderer remuneratório;
- ausência de `PayrollDeductionService` no compositor visual.

O teste funcional existente continua responsável por verificar a geração e o conteúdo do PDF.
