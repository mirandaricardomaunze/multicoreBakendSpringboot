---
id: GOAL-012
title: "Gestor vê o negócio e aprova pedidos no telemóvel pelo portal web"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/PORTAL_GESTOR_SPEC.md
harness: docs/PORTAL_GESTOR_HARNESS.md
verify: ["PortalLoginTest", "PortalSessionTest", "PortalTokenScopeTest", "PortalHttpIntegrationTest", "PortalSummaryServiceTest", "PortalSecurityHeadersTest", "PortalStaticAssetsHarnessTest", "ApiRolePolicyEndpointCoverageHarnessTest"]
blockedBy: []
---

# Objectivo

O dono ou gestor abre `https://<servidor>/portal` no telemóvel, entra com a sua conta e 2FA, vê
vendas, caixas, stock baixo e dívidas vencidas, e aprova ou rejeita pedidos pendentes.
Regras em `docs/PORTAL_GESTOR_SPEC.md`.

## Critérios de aceitação

- [x] PG-01 — login PORTAL só ADMIN/MANAGER — `PortalLoginTest`
- [x] PG-02 — 2FA obrigatório no portal — `PortalLoginTest`
- [x] PG-04 — limitador de tentativas partilhado — `PortalLoginTest`
- [x] PG-03 — expiração 12 h / 30 min e logout — `PortalSessionTest`
- [x] PG-05 — token de portal limitado às rotas do portal — `PortalTokenScopeTest`
- [x] PG-06 — isolamento de empresa — `PortalHttpIntegrationTest`
- [x] PG-07 — resumo do painel — `PortalHttpIntegrationTest`
- [x] PG-08 — aprovar/rejeitar com as regras do módulo — `PortalHttpIntegrationTest`
- [x] PG-09 — cabeçalhos de segurança — `PortalSecurityHeadersTest`
- [ ] PG-10 — só HTTPS em produção — `PortalSecurityHeadersTest`
- [ ] PG-11 — página sem CDN/inline, sessionStorage, textContent, PWA — `PortalStaticAssetsHarnessTest`
- [ ] PG-12 — layout móvel, actualização e formatos — `PortalStaticAssetsHarnessTest`

## Fora de âmbito

- Alterar dados pelo portal, app nativa, push, offline (SPEC §4).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-25 · PG-01 · Login com `channel: PORTAL` só entra para quem é ADMIN/MANAGER numa empresa (PortalLoginPolicy); EMPLOYEE recusado sem token com mensagem PT-MZ, sessão de portal só lista essas empresas, login desktop inalterado; PortalLoginTest verde 5/5.
- 2026-11-25 · PG-02 · Portal recusa ADMIN/MANAGER sem verificação em dois passos activa (mensagem PT-MZ da SPEC, sem token) e pede o código como no desktop antes de dar token; código errado recusado; login desktop inalterado; PortalLoginTest verde 9/9.
- 2026-11-25 · PG-04 · Tentativas falhadas do portal (senha errada e código 2FA errado) contam no mesmo `LoginRateLimiter` do desktop: 5 falhas bloqueiam portal e desktop na mesma origem, e falhas no desktop bloqueiam o portal; o AuthController já partilhava o limitador, os testes fixam a garantia; PortalLoginTest verde 12/12.
- 2026-11-25 · PG-03 · Sessão de portal marcada na base (migração V154, coluna `portal`): expira 30 min depois do último pedido, deslizando com o uso, e nunca passa das 12 h depois do login; logout invalida o token; sessões do desktop mantêm as 8 h sem limite de inactividade; relógio injectável no `AuthSessionService`; PortalSessionTest verde 6/6, PortalLoginTest 12/12.
- 2026-11-25 · PG-05 · Token de portal só abre `/api/auth/**`, `/api/reports/store-dashboard`, `/api/approvals/**` e `/api/portal/**` (PortalTokenScope, aplicado no TokenAuthenticationFilter); qualquer outra rota `/api/**` responde 403 com mensagem PT-MZ, incluindo plataforma e monitorização; tokens do desktop inalterados; PortalTokenScopeTest verde 5/5.
- 2026-11-25 · PG-06 · Pedido com token de portal só entra em empresa onde o utilizador é ADMIN/MANAGER (PortalLoginPolicy.requireCompanyRole no SecurityInterceptor): empresa sem acesso, empresa onde é só EMPLOYEE/SELLER e `companyId` na URL diferente do `X-Company-Id` → 403 com mensagem PT-MZ; token do desktop mantém o acesso actual; PortalHttpIntegrationTest verde 6/6.
- 2026-11-25 · PG-07 · Novo módulo `portal` com `GET /api/portal/summary` só de leitura (PortalSummaryService pelas portas Lookup): vendas realizadas de hoje e do mês por loja, caixas abertas (operador actual, desde quando), 20 artigos mais urgentes abaixo do mínimo, total vencido e top 10 clientes em atraso, aprovações pendentes; `/api/portal` só ADMIN/MANAGER no ApiRolePolicy; SPEC §3.3 detalha o contrato e anota a lacuna (a caixa ainda não guarda a loja); PortalSummaryServiceTest 5/5, PortalHttpIntegrationTest 9/9.
- 2026-11-25 · PG-08 · Portal aprova e rejeita pelos endpoints existentes de `/api/approvals` com as regras do módulo (ApprovalLifecycleService, perfil da empresa do `X-Company-Id`): lista pendentes com tipo, pedido por, valor e data; gestor aprova pedido de gestor e fica no histórico; rejeitar sem motivo → 400 com mensagem PT-MZ e pedido continua pendente; motivo gravado; gestor não decide pedido que pede administrador nem pedido de outra empresa; sem código de produção novo, os testes fixam a garantia; PortalHttpIntegrationTest verde 15/15.
- 2026-11-25 · PG-09 · Respostas de `/portal` e `/portal/**` levam `Content-Security-Policy` só com `'self'`/`'none'` (sem `unsafe-inline` nem origens externas, `frame-ancestors 'none'`), `X-Frame-Options: DENY` e `Referrer-Policy: no-referrer` (PortalSecurityHeaders ligado no SecurityConfig); o resto da aplicação mantém `SAMEORIGIN` para a consola H2; cálculo do caminho do pedido partilhado em `RequestPaths`; PortalSecurityHeadersTest verde 4/4, PortalTokenScopeTest 5/5.
