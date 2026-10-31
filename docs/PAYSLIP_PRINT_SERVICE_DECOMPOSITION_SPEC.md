# Especificação Técnica: Decomposição do PayslipPrintService (Fase 124)

## 1. Diagnóstico

`PayslipPrintService.java` acumulava 302 linhas e três razões independentes para mudar:

- carregamento do recibo salarial no domínio de RH;
- composição das duas vias e dos blocos visuais do PDF;
- discriminação e apresentação de vencimentos e descontos comprometidos.

Essa concentração aumentava o acoplamento entre a fachada de impressão, OpenPDF e
`PayrollDeductionService`, tornando alterações de apresentação difíceis de testar e manter.

## 2. Decisão arquitectural

A implementação foi dividida por responsabilidade, sem alterar cálculos, contratos HTTP ou conteúdo:

1. `PayslipPrintService` (44 linhas): fachada transaccional que carrega o agregado e delega a emissão.
2. `PayslipDocumentRenderer` (200 linhas): compõe vias, cabeçalho, colaborador, período, líquido,
   observações, configuração fiscal e assinaturas.
3. `PayslipCompensationTableRenderer` (105 linhas): renderiza vencimentos, descontos discriminados,
   totais e INSS patronal informativo.

O construtor anterior `(HRService, PayrollDeductionService)` permanece disponível para compatibilidade
com testes e integrações. O construtor canónico Spring recebe `HRService` e `PayslipDocumentRenderer`.

## 3. Invariantes preservadas

- duas vias por recibo;
- valores monetários continuam em `BigDecimal` e formatados por `MoneyFormat`;
- descontos comprometidos continuam discriminados linha a linha;
- qualquer remanescente continua identificado como `Outros Descontos`;
- nenhum cálculo salarial, fiscal ou contributivo foi alterado;
- o backend continua responsável pelo PDF e o desktop apenas o consome.

## 4. Validação

- `CommercialNotesAndPayslipPrintServiceTest`;
- `PayslipPrintServiceDecompositionHarnessTest`;
- `MultiModuleArchitectureHarnessTest`;
- `DesktopThinContextTest`;
- `mvn clean compile`.
