# Especificação de Decomposição: DebitNoteService (Fase 164)

## Contexto e Problema
`DebitNoteService` (216 linhas) concentrava a criação fiscal com cálculo de linhas, aprovação com emissão de `DebitNoteApprovedEvent`, rejeição, cancelamento, consultas multi-empresa e mapeamento para `DebitNoteDTO`.

## Solução Arquitectural
1. **`DebitNoteService` (Fachada — `<= 100` linhas)**:
   - Aplica os guards de segurança (`PermissionGuard.requireManagerOrAdmin` / `CurrentUserContext.requireCompany`) e delega para `DebitNoteLifecycleService` e `DebitNoteQueryService`.
   - Mantém o construtor de 5 argumentos compatível.
2. **`DebitNoteLifecycleService` (`@Component` — `<= 150` linhas)**:
   - Gere a criação, aprovação, rejeição e cancelamento de notas de débito.
3. **`DebitNoteQueryService` (`@Component` — `<= 85` linhas)**:
   - Gere as consultas (`findByCompany`, `findById`, `loadForPrint`) e o mapeamento `toDTO`.
