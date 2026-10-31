# Especificação Técnica: Decomposição Modular do AppUserService (Fase 121)

## 1. Contexto e Diagnóstico
O `AppUserService.java` (327 linhas, `mz.multicore.erp.modules.users.service`) acumulava:
- Autenticação com verificação de utilizador ativo, suporte BCrypt e migração suave de credenciais legadas em texto-plano.
- Gestão e verificação de PIN de gestor (4 dígitos) com controlo de rate limiting contra força bruta (`ManagerPinRateLimiter`) e bloqueio por IP/empresa.
- Administração completa de contas (criação, edição de nome, atribuição de acessos multiempresa, alteração de perfis, reposição de senha, ativação/desativação).
- Regras críticas de segurança como barreira do último administrador da empresa, proteção contra manipulação de administradores de plataforma e revogação em cascata de sessões ativas (`AuthSessionService.revokeUser`).
- Barreira de autorização administrativa (`requireAdmin`) e registo em auditoria forense (`AuditLogService`).

## 2. Decisão Arquitetural
Segregação das responsabilidades em colaboradores de responsabilidade única (SRP), preservando integralmente o comportamento funcional e as barreiras de segurança:

1. **`UserAdminGuard.java`** ($\le 35$ linhas):
   - Métodos estáticos puros de guarda de autorização: `requireAdmin()` e `requireManagedUser(user, companyId)`.
2. **`UserAuthenticationService.java`** ($\le 65$ linhas):
   - Autenticação segura sem enumeração de contas (`authenticate`), suporte a hashes BCrypt e re-encriptação suave de passwords legadas.
3. **`ManagerPinService.java`** ($\le 110$ linhas):
   - Atribuição de PIN com validação estrita de 4 dígitos numéricos (`setManagerPin`).
   - Verificação com rate limiting e mensagens amigáveis de bloqueio temporal em português de Moçambique (`verifyManagerPin`).
4. **`UserAccountAdministrationService.java`** ($\le 185$ linhas):
   - Listagem tenant-scoped de utilizadores (`getAllUsers`).
   - Criação com `PasswordPolicy.validate` e associação à empresa ativa (`createUser`).
   - Gestão de nomes, perfis e permissões multiempresa (`updateUserName`, `grantCompanyAccess`, `updateCompanyRole`).
   - Reposição de senhas com revogação de tokens de sessão (`resetPassword`).
   - Ativação/desativação com barreira de proteção do último administrador ativo da empresa (`toggleUserStatus`).
   - Rastreio de eventos auditados via `AuditLogService`.
5. **`AppUserService.java`** ($\le 155$ linhas):
   - Fachada executiva que orquestra as operações delegando nos colaboradores especializados.
   - Construtor canónico `@Autowired` recebendo os 3 serviços especializados.
   - Preservação estrita dos 4 construtores sobrecarregados retrocompatíveis (4, 5, 6 e 7 argumentos) para compatibilidade total com suites de testes existentes.

## 3. Conformidade
- Redução de 327 linhas para ~125 linhas no orquestrador `AppUserService.java`.
- 100% de compatibilidade retrocompatível com `UserManagementHarnessTest`, `RateLimitingAndBruteForceHarnessTest`, `SecurityAttackSurfaceTest`, `SecurityEnterpriseHardeningHarnessTest` e `AuditTrailCriticalEventsHarnessTest`.
- Backend headless (zero importação ou menção de UI/Swing).
- Mensagens ao utilizador em português de Moçambique.
