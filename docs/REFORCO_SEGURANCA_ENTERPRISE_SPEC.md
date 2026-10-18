# Reforço de Segurança Empresarial (Password Policy, Sessões Atómicas & Defense-in-Depth)

**Última actualização:** 2026-10-16  
**Estado:** em implementação  
**Dono:** `architecture/security` e `modules/users`

---

## 1. Objectivo

Reforçar a segurança do Multicore ERP nas três frentes mais críticas de governança corporativa e autenticação:
1. **Política Canónica de Senhas Fortes (`PasswordPolicy`):** erradicar senhas triviais e fracas em novos utilizadores e redefinições de senha.
2. **Revogação Imediata e Atómica de Sessões (`SessionRevocation`):** garantir que a desativação de um funcionário, a alteração de senha ou a alteração de perfil encerra de imediato todos os tokens Bearer ativos no `AuthSessionService`, eliminando janelas de privilégio indevido.
3. **Defense-in-Depth no Spring Security (`Authorities Enrichment`):** propagar o papel ativo da empresa (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_SELLER`, `ROLE_EMPLOYEE`, `ROLE_SUPERADMIN`) para as `GrantedAuthority` do `SecurityContextHolder`, viabilizando autorização declarativa nativa do Spring Security além do `PermissionGuard`.

---

## 2. Regras Não Negociáveis

### R1 — Política de Senhas Fortes (`PasswordPolicy`)
- Comprimento mínimo de **8 caracteres**.
- Presença obrigatória de pelo menos uma letra e pelo menos um dígito numérico.
- Proibição estrita de senhas que contenham ou sejam iguais ao `username` (case-insensitive).
- Proibição de senhas triviais comuns (ex.: `12345678`, `password`, `admin123`, `multicore`, `qwertyuiop`).
- Mensagens de erro em Português de Moçambique, informando claramente a regra violada.
- Aplicada centralmente em `createUser` e `resetPassword` em `AppUserService`.

### R2 — Revogação Atómica de Sessões
- Quando `toggleUserStatus(username, false)` é invocado (utilizador desativado):
  - Invocação obrigatória de `authSessionService.revokeUser(username)`. Nenhum pedido subsequente com o token desse utilizador pode ser aceite.
- Quando `resetPassword(username, newPassword)` é invocado:
  - Invocação de `authSessionService.revokeUser(username)` para invalidar acessos antigos.
- Quando `updateCompanyRole(username, newRole)` é invocado e o papel for alterado:
  - Invocação de `authSessionService.revokeUser(username)` para obrigar reautenticação com o novo perfil.

### R3 — Enriquecimento de Authorities no Spring Security
- No `SecurityInterceptor` (onde o par utilizador + empresa é resolvido):
  - O `SecurityContextHolder.getContext().getAuthentication()` deve ser atualizado com uma `UsernamePasswordAuthenticationToken` contendo a autoridade canónica `ROLE_<ROLE_NAME>`.
  - No caso de superadmin em endpoints `/api/platform/**`, atribui `ROLE_SUPERADMIN`.

---

## 3. Cobertura de Testes e Harness

- Validado por `SecurityEnterpriseHardeningHarnessTest` no módulo `backend`.
- Gate arquitetural protegido por `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.
