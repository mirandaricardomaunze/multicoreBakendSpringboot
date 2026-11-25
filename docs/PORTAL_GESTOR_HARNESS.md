# Harness — Portal web do gestor

> Cenários que provam [PORTAL_GESTOR_SPEC.md](PORTAL_GESTOR_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-25

| ID | Garantia | Teste |
|---|---|---|
| PG-01 | Login canal PORTAL só para ADMIN/MANAGER; outro perfil recusado sem token; só empresas onde é ADMIN/MANAGER (§3.1) — **verde** | `PortalLoginTest` |
| PG-02 | Conta sem 2FA activo recusada com mensagem PT-MZ; código 2FA pedido (§3.1) | `PortalLoginTest` |
| PG-03 | Token de portal expira em 12 h ou 30 min sem uso; logout invalida (§3.1) | `PortalSessionTest` |
| PG-04 | Tentativas falhadas contam no `LoginRateLimiter` (§3.1) | `PortalLoginTest` |
| PG-05 | Token de portal só aceite nas rotas do portal; resto 403; tokens desktop inalterados (§3.2) | `PortalTokenScopeTest` |
| PG-06 | Isolamento de empresa: portal só vê empresas do utilizador (§3.2) | `PortalHttpIntegrationTest` |
| PG-07 | `/api/portal/summary` devolve vendas por loja, caixas abertas, stock baixo (20), vencidas (10), aprovações pendentes (§3.3) | `PortalHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| PG-08 | Aprovar/rejeitar pelo portal respeita as regras do módulo; rejeitar exige motivo (§3.4) | `PortalHttpIntegrationTest` |
| PG-09 | Cabeçalhos CSP sem `unsafe-inline`/origens externas, `X-Frame-Options: DENY`, `Referrer-Policy` em `/portal/**` (§3.5) | `PortalSecurityHeadersTest` |
| PG-10 | Em `prod`, portal sem HTTPS recusado (§3.5) | `PortalSecurityHeadersTest` |
| PG-11 | Página: sem CDN nem script inline, token em `sessionStorage`, só `textContent`, manifest e ícone presentes (§3.5) | `PortalStaticAssetsHarnessTest` |
| PG-12 | Página: viewport móvel, layout a partir de 360 px, actualização a cada 5 min, números e datas no formato do sistema (§3.3, §3.5) | `PortalStaticAssetsHarnessTest` |
