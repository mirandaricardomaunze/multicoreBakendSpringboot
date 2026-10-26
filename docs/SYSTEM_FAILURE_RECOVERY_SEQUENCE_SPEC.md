# SPEC — Recuperação Faseada de Falhas do Sistema

## Objectivo

Definir a sequência canónica para recuperar o sistema quando a suite integral revela falhas em
segurança, documentos PDF e interface desktop, sem enfraquecer regras de negócio nem reverter a
decomposição arquitectural dos painéis.

## Sequência obrigatória

1. **Segurança e tenant:** corrigir primeiro autenticação, política de senha, autorização HTTP e
   selecção de empresa. A negação por omissão permanece activa.
2. **Documentos operacionais:** validar conteúdo, cálculo e apresentação. Guias de remessa e
   transferência devem mostrar preço unitário e total de linha de forma legível.
3. **Contratos de interface:** restaurar terminologia canónica e manter o `MainFrame` instanciável.
4. **Observabilidade dos testes:** processos Surefire não herdam activação global de DEBUG do
   ambiente. Logs esperados de cenários negativos não contam como falha do build.
5. **Ergonomia desktop:** cabeçalhos de cards têm no máximo três acções visíveis; actualizar fica
   junto dos filtros quando necessário.
6. **Ownership após decomposição:** harnesses procuram a regra no colaborador que passou a ser o
   dono real da operação. Não se volta a colocar lógica em painéis grandes apenas para satisfazer
   uma pesquisa textual antiga.
7. **Gate:** executar harnesses dirigidos, `DesktopThinContextTest`,
   `MultiModuleArchitectureHarnessTest`, suite do módulo e finalmente a suite integral do reactor.

## Invariantes protegidas

- A política de senha forte nunca é reduzida para fazer um teste antigo passar.
- `ApiRolePolicy` continua deny-by-default e todos os endpoints permanecem classificados.
- Impressão sensível passa por `SensitivePrintCoordinator`, que termina em
  `PrintPreviewDialog`, preservando autorização e auditoria.
- Filtros aceitam tanto enums do backend como rótulos humanizados em português de Moçambique.
- Guias mantêm `BigDecimal` e não alteram movimentos de stock ou valores fiscais nesta mudança de
  apresentação.
- Validação de formulários permanece dentro do `ModernFormDialog`, com marcação e foco no campo
  inválido.
- A decomposição de `MainFrame`, `ComercialPanel` e `StockPanel` em colaboradores é preservada.

## Harnesses de aceitação

- Segurança: `SecurityAttackSurfaceTest`, `TenantAccessServiceTest`,
  `SecurityEnterpriseHardeningHarnessTest`, `ApiRolePolicyEndpointCoverageHarnessTest`.
- PDFs: `DeliveryGuidesPrintServiceTest`, `StockTransferPrintServiceTest`, `LineRowMapperTest`.
- Desktop e ergonomia: `DesktopThinContextTest`, `HrCrmUiErgonomicsHarnessTest`,
  `PhysicalInventoryPanelHarnessTest`, `PrintModalHarnessTest`, `AbsenceTypeLabelsTest`.
- Ownership e fluxos: `ActiveCreditInterceptionHarnessTest`, `ProfessionalFeedbackHarnessTest`,
  `PhcDocumentEditorHomologationHarnessTest`, `SellerExperienceHarnessTest`,
  `StockTransferReceiptDiscrepancyUiHarnessTest`.
- Arquitectura: `MultiModuleArchitectureHarnessTest`.

## Critério de pronto

- `mvn compile` aprovado.
- Suites de `backend` e `desktop` sem falhas.
- Suite integral do reactor sem falhas.
- Nenhuma regra crítica deslocada de volta para um painel monolítico.
