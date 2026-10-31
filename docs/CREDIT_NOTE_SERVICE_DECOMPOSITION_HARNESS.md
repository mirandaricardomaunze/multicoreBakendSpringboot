# Harness de Testes - CreditNoteService Decomposição (Fase 116)

## Casos de Teste Estruturais e Funcionais
1. **Verificação de Limites Físicos (Linhas de Código):**
   - `CreditNoteService.java`: $\le 105$ linhas.
   - `CreditNoteCreationService.java`: $\le 175$ linhas.
   - `CreditNoteLifecycleService.java`: $\le 165$ linhas.
   - `CreditNoteQueryService.java`: $\le 145$ linhas.
2. **Delegação e Preservação de Assinatura:**
   - Garantir delegação para `CreditNoteCreationService.create`.
   - Garantir delegação para `CreditNoteLifecycleService.approve`, `reject`, `cancel`.
   - Garantir delegação para `CreditNoteQueryService.findByCompany`, `findById`, `getReturnedQuantitiesByInvoiceLine`.
3. **Casos Funcionais:**
   - Sucesso em `CreditNoteServiceTest` (8/8 testes cobrindo criação, devolução física com FEFO, teto de valor, teto de quantidade, aprovação, rejeição e permissões).
4. **Verificação Arquitetural Global:**
   - `MultiModuleArchitectureHarnessTest`: 100% aprovado.
   - `DesktopThinContextTest`: 100% aprovado.
