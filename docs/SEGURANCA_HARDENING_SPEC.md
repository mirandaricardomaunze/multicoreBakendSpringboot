# Endurecimento de segurança para hospedagem

**Última actualização:** 2026-07-18
**Estado:** feito e **validado ao vivo** contra o backend a correr (PostgreSQL real). Fecha o item #1
da checklist de go-live do [DEPLOY_VPS_SPEC.md](DEPLOY_VPS_SPEC.md).

## Objectivo

Antes de expor o backend à internet, o `SecurityConfig` estava em `permitAll()` — quem protegia era só
o `SecurityInterceptor` (uma fronteira). Esta iteração acrescenta **defense-in-depth** ao nível do
Spring Security, sem quebrar o desktop nem os painéis já migrados, e um **health check profissional**
para o container.

## Como funciona

1. **[TokenAuthenticationFilter](../src/main/java/mz/multicore/erp/architecture/security/TokenAuthenticationFilter.java)**
   — lê o `Authorization: Bearer <token>`, valida contra o `AuthSessionService` e, se válido, popula o
   `SecurityContext`. **Lenient**: token ausente/inválido → não autentica (não lança), deixando o
   `authorizeHttpRequests` recusar. Nunca curto-circuita o login.
2. **[SecurityConfig](../src/main/java/mz/multicore/erp/architecture/security/SecurityConfig.java)** — deixou
   de ser `permitAll()`. Agora:
   - `permitAll`: `/api/auth/login`, `/api/auth/logout`, `/actuator/health`.
   - `authenticated`: `/api/**` (recusa 401 sem token válido).
   - entry point devolve **401** quando falta autenticação.
   - o filtro é registado antes do `UsernamePasswordAuthenticationFilter`.
3. **`SecurityInterceptor`** mantém-se por cima — resolve empresa/papel (`X-Company-Id`), superadmin em
   `/api/platform/**`, e audita. Redundância deliberada (o token é validado nas duas camadas).
4. **Actuator** (nova dependência) — expõe **só** `/actuator/health` (`show-details=never`); nada de
   `env`/`beans`/`mappings`. Usado pelo `HEALTHCHECK` do [Dockerfile](../Dockerfile).

**Importante:** as chamadas **em processo** do desktop (painéis ainda não migrados) **não passam por
este filtro** — não são HTTP. Logo o endurecimento não afecta a app a correr; só fecha a superfície HTTP.

## Peças

- `TokenAuthenticationFilter` (novo), `SecurityConfig` (endurecido), `spring-boot-starter-actuator`,
  `management.*` em `application.properties`, `HEALTHCHECK` do Dockerfile → `/actuator/health`.

## Limite honesto

- As sessões continuam **em memória** (`AuthSessionService`, `Map`): uma só instância de backend; escalar
  horizontalmente exigiria um store partilhado (Redis). Aceite para deploy pequeno (documentado no DEPLOY).
- CSRF/CORS continuam desligados — correto para uma API de token consumida por um cliente Java (não-browser).
