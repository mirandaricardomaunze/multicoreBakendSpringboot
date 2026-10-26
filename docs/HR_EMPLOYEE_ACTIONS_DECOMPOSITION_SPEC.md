# Especificação de Decomposição: Ações e Diálogos de Colaboradores de RH (HREmployeeActions.java)

- **Document ID**: SPEC-HR-EMPLOYEE-ACTIONS-DECOMPOSITION-001
- **Fase**: 72
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Módulo**: `desktop`
- **Domínio**: Recursos Humanos (`mz.multicore.erp.gui`)

---

## 1. Contexto e Diagnóstico

O ficheiro `desktop/src/main/java/mz/multicore/erp/gui/HREmployeeActions.java` acumulava **663 linhas de código** e continha **5 chamadas legadas a `JOptionPane`** (`showOptionDialog`, `showConfirmDialog`, `showMessageDialog`), violando o padrão corporativo Multicore ERP de diálogos ergonómicos baseados em `ModernFormDialog` e `ModernMessageDialog`.

Além disso, o ficheiro acumulava quatro responsabilidades distintas:
1. Gestão e histórico de evolução salarial do colaborador (§B4);
2. Gestão de documentos de identidade, vistos e validades (§B8.8);
3. Gestão e conformidade de saúde ocupacional, exames de aptidão, prestadores, custos e pagamentos;
4. Justificação formal de faltas do colaborador (§B2 / RHC-25).

---

## 2. Decisão Arquitetural e Decomposição

Para manter a coesão, eliminar todos os diálogos legados `JOptionPane` e respeitar o teto estrito de linhas por classe, a decomposição é estruturada em 4 classes:

1. **`HREmployeeSalaryDialog.java`** ($\le 180$ linhas):
   - Diálogo modal de consulta de histórico de evolução salarial com gráfico temporal `SimpleBarChart` e tabela formatada de 7 colunas;
   - Diálogo modal corporativo para registo de nova alteração salarial (`CreateSalaryChangeRequest`).
   - Zero chamadas a `JOptionPane`.

2. **`HREmployeeDocumentsDialog.java`** ($\le 160$ linhas):
   - Diálogo modal corporativo de consulta de documentos do trabalhador com cálculo de situação de validade (`situationOf`);
   - Diálogo modal corporativo para registo de novo documento (`SaveEmployeeDocumentRequest`).
   - Zero chamadas a `JOptionPane`.

3. **`HREmployeeOccupationalHealthDialog.java`** ($\le 320$ linhas):
   - Diálogo de histórico de saúde ocupacional com tabela de 8 colunas e resumo de custos suportados pela empresa;
   - Modal corporativo de conformidade e custos anuais por prestador (`JTabbedPane`);
   - Modal de registo de exame com upload/anexo e seleção de prestador credenciado;
   - Abertura segura de comprovativo e pagamento via `ModernMessageDialog.confirm`.
   - Zero chamadas a `JOptionPane`.

4. **`HREmployeeActions.java`** ($\le 100$ linhas, reduzido de 663 linhas):
   - Orquestrador canónico mantendo a API pública inalterada para retrocompatibilidade com `HRPanel`, `HREmployeeDialogsDriver` e `OccupationalHealthScreensDriver`.
   - Implementa a justificação de faltas (`justifyAbsence`).
   - Zero chamadas a `JOptionPane`.

---

## 3. Avanço da Catraca de Qualidade

- Remoção de `HREmployeeActions.java` da allowlist de `ProfessionalFeedbackHarnessTest.java`.
- Redução do teto global de `JOptionPane` de 60 para 55.
