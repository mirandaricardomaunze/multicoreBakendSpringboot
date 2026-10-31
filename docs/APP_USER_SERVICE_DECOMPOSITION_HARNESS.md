# Harness de Decomposição - AppUserService (Fase 121)

## 1. Objectivo

Validar estrutural e funcionalmente a decomposição de `AppUserService.java` em componentes especializados de autenticação, PIN de gestor, administração de contas e guardas de segurança, mantendo contratos retrocompatíveis e conformidade headless.

## 2. Casos de Teste

| ID | Descrição | Critério de Aceitação |
|---|---|---|
| **AUSD-01** | Limites físicos de linhas | `AppUserService` $\le 155$, `UserAccountAdministrationService` $\le 185$, `ManagerPinService` $\le 110$, `UserAuthenticationService` $\le 65$, `UserAdminGuard` $\le 35$. |
| **AUSD-02** | Backend headless | Zero dependências ou chamadas de `JOptionPane` ou `javax.swing`. |
| **AUSD-03** | Delegação canónica e barreiras | Fachada delega em `authenticationService`, `managerPinService` e `accountAdministrationService`; colaboradores preservam regras de último admin, rate limiting e BCrypt. |
| **AUSD-04** | Preservação de contratos | Preservação de métodos públicos, anotação `@Autowired` e construtores sobrecarregados retrocompatíveis. |

## 3. Regressão Funcional

- `UserManagementHarnessTest`
- `RateLimitingAndBruteForceHarnessTest`
- `SecurityAttackSurfaceTest`
- `SecurityEnterpriseHardeningHarnessTest`
- `AuditTrailCriticalEventsHarnessTest`
- `MultiModuleArchitectureHarnessTest`
- `DesktopThinContextTest`
