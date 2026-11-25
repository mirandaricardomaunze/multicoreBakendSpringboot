# Harness — Portal web do gestor

> Cenários que provam [PORTAL_GESTOR_SPEC.md](PORTAL_GESTOR_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-25

| ID | Garantia | Teste |
|---|---|---|
| PG-01 | Login canal PORTAL só para ADMIN/MANAGER; outro perfil recusado sem token; só empresas onde é ADMIN/MANAGER (§3.1) — **verde** | `PortalLoginTest` |
| PG-02 | Conta sem 2FA activo recusada com mensagem PT-MZ; código 2FA pedido (§3.1) — **verde** | `PortalLoginTest` |
| PG-03 | Token de portal expira em 12 h ou 30 min sem uso; logout invalida; desktop mantém 8 h sem limite de inactividade (§3.1) — **verde** | `PortalSessionTest` |
| PG-04 | Tentativas falhadas (senha e código 2FA) contam no `LoginRateLimiter` partilhado com o desktop (§3.1) — **verde** | `PortalLoginTest` |
| PG-05 | Token de portal só aceite nas rotas do portal; resto 403; tokens desktop inalterados (§3.2) — **verde** | `PortalTokenScopeTest` |
| PG-06 | Isolamento de empresa: portal só vê empresas do utilizador onde é ADMIN/MANAGER; outra empresa ou `companyId` diferente do `X-Company-Id` → 403; desktop inalterado (§3.2) — **verde** | `PortalHttpIntegrationTest` |
| PG-07 | `/api/portal/summary` devolve vendas por loja, caixas abertas, stock baixo (20), vencidas (10), aprovações pendentes; só ADMIN/MANAGER da empresa (§3.3) — **verde** | `PortalHttpIntegrationTest` · `PortalSummaryServiceTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| PG-08 | Aprovar/rejeitar pelo portal respeita as regras do módulo; rejeitar exige motivo; gestor não decide pedido de administrador nem de outra empresa (§3.4) — **verde** | `PortalHttpIntegrationTest` |
| PG-09 | Cabeçalhos CSP sem `unsafe-inline`/origens externas, `X-Frame-Options: DENY`, `Referrer-Policy` em `/portal/**` (§3.5) — **verde** | `PortalSecurityHeadersTest` |
| PG-10 | Em `prod`, `/portal/**` e `/api/portal/**` sem HTTPS (directo ou `X-Forwarded-Proto: https`) → 403 PT-MZ; resto da aplicação e perfis não-prod inalterados (§3.5) — **verde** | `PortalSecurityHeadersTest` |
| PG-11 | Página: sem CDN nem script inline, token em `sessionStorage`, só `textContent`, manifest e ícone presentes (§3.5) | `PortalStaticAssetsHarnessTest` |
| PG-12 | Página: viewport móvel, layout a partir de 360 px, actualização a cada 5 min, números e datas no formato do sistema (§3.3, §3.5) | `PortalStaticAssetsHarnessTest` |
