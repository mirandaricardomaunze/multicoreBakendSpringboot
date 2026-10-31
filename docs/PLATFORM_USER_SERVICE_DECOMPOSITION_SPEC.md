# Especificação de Decomposição: PlatformUserService (Fase 170)

## Contexto e Problema
`PlatformUserService` (204 linhas) concentrava a gestão global de contas de utilizador pelo superadmin (listagem, criação, edição de nome, activação/desactivação com protecção de administrador único, reposição de senha e revogação de sessão) juntamente com a concessão e revogação de acessos multi-empresa.

## Solução Arquitectural
1. **`PlatformUserService` (Fachada — `<= 105` linhas)**:
   - Aplica os guards de segurança (`PermissionGuard.requireSuperAdmin`) e delega para `PlatformUserAccountService` e `PlatformUserAccessService`.
   - Mantém os construtores retrocompatíveis de 5 e 6 argumentos.
2. **`PlatformUserAccountService` (`@Component` — `<= 135` linhas)**:
   - Gere `listUsers`, `createUser`, `updateUser`, `setUserActive`, `resetPassword`, `requireUser`, `audit` e `toDto`.
3. **`PlatformUserAccessService` (`@Component` — `<= 85` linhas)**:
   - Gere `grantAccess`, `revokeAccess` e `requireCompany`.
