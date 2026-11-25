---
id: GOAL-012
title: "Gestor vê o negócio e aprova pedidos no telemóvel pelo portal web"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/PORTAL_GESTOR_SPEC.md
harness: docs/PORTAL_GESTOR_HARNESS.md
verify: ["PortalLoginTest", "PortalSessionTest", "PortalTokenScopeTest", "PortalHttpIntegrationTest", "PortalSecurityHeadersTest", "PortalStaticAssetsHarnessTest", "ApiRolePolicyEndpointCoverageHarnessTest"]
blockedBy: []
---

# Objectivo

O dono ou gestor abre `https://<servidor>/portal` no telemóvel, entra com a sua conta e 2FA, vê
vendas, caixas, stock baixo e dívidas vencidas, e aprova ou rejeita pedidos pendentes.
Regras em `docs/PORTAL_GESTOR_SPEC.md`.

## Critérios de aceitação

- [x] PG-01 — login PORTAL só ADMIN/MANAGER — `PortalLoginTest`
- [x] PG-02 — 2FA obrigatório no portal — `PortalLoginTest`
- [ ] PG-04 — limitador de tentativas partilhado — `PortalLoginTest`
- [ ] PG-03 — expiração 12 h / 30 min e logout — `PortalSessionTest`
- [ ] PG-05 — token de portal limitado às rotas do portal — `PortalTokenScopeTest`
- [ ] PG-06 — isolamento de empresa — `PortalHttpIntegrationTest`
- [ ] PG-07 — resumo do painel — `PortalHttpIntegrationTest`
- [ ] PG-08 — aprovar/rejeitar com as regras do módulo — `PortalHttpIntegrationTest`
- [ ] PG-09 — cabeçalhos de segurança — `PortalSecurityHeadersTest`
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
