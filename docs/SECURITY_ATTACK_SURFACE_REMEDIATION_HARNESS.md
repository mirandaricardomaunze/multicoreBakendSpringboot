# HARNESS-SEC-2026-10: Verificacao do fecho de ataques

Spec: [SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md](SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md).

## Casos automatizados

| Caso | Ataque / regra | Teste |
|---|---|---|
| SEC-01 | Senhas alternativas de `admin` recusadas e mensagem de erro uniforme | `SecurityAttackSurfaceTest.adminHasNoFixedAlternativePasswordAndUnknownUserHasSameError` |
| SEC-02 | Administrador de A nao repoe senha de B ou da plataforma e nao define PIN de B | `SecurityAttackSurfaceTest.tenantAdminCannotResetPlatformOrOtherTenantAndOwnResetRevokesSessions` |
| SEC-03 | Reposicao de senha invalida imediatamente as sessoes | `SecurityAttackSurfaceTest`, `PlatformUserServiceTest.resetPassword_revokesExistingSessions` |
| SEC-04 | Empresa ou assinatura suspensa recusada no pedido seguinte | `SecurityAttackSurfaceTest.suspendedCompanyOrSubscriptionLosesAccessOnNextRequest` |
| SEC-05 | Vendedor recusado na monitorizacao; administrador limitado a sua empresa | `SecurityAttackSurfaceTest.monitoringUsesRealRoleAndCompanyScope` |
| SEC-06 | Auditoria forense recusa identificador de outra empresa | `SecurityAttackSurfaceTest.forensicSummaryRejectsForeignCompanyForTenantAdmin` |
| SEC-07 | Origem que falha nao bloqueia a mesma conta noutra origem; mapa com limite | `LoginRateLimiterTest` |
| SEC-08 | `X-Forwarded-For` ignorado sem proxy declarado | `SecurityAttackSurfaceTest.forwardedIpRequiresDeclaredProxy` |
| SEC-09 | HTTP remoto recusado e HTTPS remoto/HTTP loopback validos | `DesktopApiConfigSecurityTest` |
| SEC-10 | Desktop e API continuam a arrancar com as regras novas | `DesktopThinContextTest`, `SecurityApiIntegrationTest`, `SystemMonitoringHarnessTest` |

## Verificacao de configuracao

- `application.properties`: `spring.h2.console.settings.web-allow-others=false`.
- `application-prod.properties`: `TRUSTED_PROXY_HOST` vazio por omissao.
- `docker-compose.yml`: backend privado com `TRUSTED_PROXY_HOST=caddy`.
- `deployment/Caddyfile`: Caddy substitui `X-Forwarded-For` pelo IP remoto real.
- O desktop normal nao mostra os atalhos de demonstracao; `-Dmulticore.demo-login=true` activa-os apenas para uma demonstracao isolada.

## Comandos

```powershell
mvn clean compile
mvn test
```

Se o JAR de backend estiver aberto, `mvn clean compile` nao consegue apagar `backend/target`; usar
`mvn compile` sem encerrar a aplicacao do utilizador e registar o motivo.
