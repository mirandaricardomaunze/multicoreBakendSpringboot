# Especificação de Decomposição: SupportService (Fase 163)

## Contexto e Problema
`SupportService` (218 linhas) concentrava num único ficheiro as operações do lado da empresa (abertura de tickets, listagem e resposta com reabertura automática) e as operações de superadmin da plataforma (listagem global, resposta com atribuição automática, transição de estados e mapeamento DTO).

## Solução Arquitectural
1. **`SupportService` (Fachada — `<= 95` linhas)**:
   - Aplica os guards de segurança (`PermissionGuard.requireManagerOrAdmin` / `requireSuperAdmin`) e delega para `CompanySupportTicketService` e `PlatformSupportAdminService`.
   - Mantém `priorityOptions()`, `statusOptions()` e o construtor de 3 argumentos compatível.
2. **`CompanySupportTicketService` (`@Component` — `<= 125` linhas)**:
   - Gere a abertura de tickets, listagem e mensagens do tenant, incluindo reabertura automática de tickets `RESOLVED`.
3. **`PlatformSupportAdminService` (`@Component` — `<= 120` linhas)**:
   - Gere a listagem global, resposta de superadmin, alteração de estado e mapeamento de `SupportTicketDTO` / `SupportMessageDTO`.
