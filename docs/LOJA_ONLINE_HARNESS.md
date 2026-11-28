# Harness — Loja online

> Cenários que provam [LOJA_ONLINE_SPEC.md](LOJA_ONLINE_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-28 (LO-01 verde: `OnlineShopSettingsTest`, migração V162; LO-02 verde: `OnlineShopCatalogTest`, migração V163; LO-03 verde: `OnlineShopCatalogTest`; LO-05, LO-06 e LO-07 verdes: `OnlineShopOrderTest`; LO-08 verde: `OnlineShopOrderTest` · `OnlineShopSettingsTest`; LO-10 verde: `OnlineShopExpiryTest`; LO-04 verde: `OnlineShopHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest`; LO-09 verde: `OnlineShopHttpIntegrationTest`, `POST /api/shop/{código}/orders/lookup`; LO-11 verde: `OnlineShopStaticAssetsHarnessTest`, página `/loja/{código}` em `static/loja/` e `GET /api/shop/{código}`; LO-UI-01 dividido em LO-12, LO-UI-01, LO-UI-02 e LO-UI-03 com a SPEC §3.5; LO-12 verde: `OnlineShopOrderTest`, migração V164)

| ID | Garantia | Teste |
|---|---|---|
| LO-01 | Activação: código público único e válido, lojas de levantamento, zonas com taxa ≥ 0, prazo (omissão 7); só ADMIN (§3.1) | `OnlineShopSettingsTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| LO-02 | Só artigos activos com "Publicar na loja online"; campo desligado por omissão (§3.1) | `OnlineShopCatalogTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| LO-03 | Disponível/Esgotado pelo stock disponível nas lojas de levantamento; sem quantidades, custos nem fornecedores (§3.2) | `OnlineShopCatalogTest` |
| LO-04 | `/api/shop/{código}` sem login; loja desactivada indisponível; limites por IP (60/min, 5 encomendas/hora → 429 com `Retry-After`) e honeypot `website` (recusa sem criar) (§3.2) | `OnlineShopHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| LO-05 | Checkout valida nome, telemóvel MZ, NUIT opcional, modo, zona e morada (§3.3) | `OnlineShopOrderTest` |
| LO-06 | Encomenda pelo serviço da API pública: preços do sistema, reserva, cliente por NUIT ou Consumidor Final com contacto, origem `LOJA_ONLINE` (§3.3) | `OnlineShopOrderTest` |
| LO-07 | Armazém pela loja de levantamento ou primeira com stock na entrega; sem stock recusa sem criar (§3.3) | `OnlineShopOrderTest` |
| LO-08 | Linha "Taxa de entrega — zona" com a taxa e o IVA normal; artigo de serviço da zona criado na activação (§3.3) | `OnlineShopOrderTest` · `OnlineShopSettingsTest` |
| LO-09 | Consulta por número + telemóvel (`POST /api/shop/{código}/orders/lookup`, só encomendas `LOJA_ONLINE` da loja); telemóvel errado ou inválido responde como número inexistente, "Encomenda não encontrada." (§3.3) | `OnlineShopHttpIntegrationTest` |
| LO-10 | Cancelamento automático após o prazo (encomendas `LOJA_ONLINE` canceláveis, de hora a hora, como SYSTEM da empresa), liberta reserva, com auditoria `ORDER_CANCEL` (§3.4) | `OnlineShopExpiryTest` |
| LO-11 | Página pública `/loja/{código}` (`static/loja/`): sem CDN/inline, CSP só `'self'`, `textContent`, 360 px, pesquisa/categoria/Disponível-Esgotado, carrinho no `localStorage` só com SKUs e quantidades, honeypot escondido; `GET /api/shop/{código}` só com nome, contacto, lojas de levantamento e zonas (§3.2) | `OnlineShopStaticAssetsHarnessTest` |
| LO-12 | Encomenda `LOJA_ONLINE` guarda `fulfilmentLabel` (Levantamento — loja / Entrega — zona) e `deliveryAddress` (só entrega); `OrderDTO` traz origem, rótulo, contacto, entrega e morada; desktop/API ficam nulos (§3.5) | `OnlineShopOrderTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| LO-UI-01 | Desktop: Configurações › Integrações › secção "Loja online" (activa, código com `/loja/{código}`, lojas de levantamento, zonas, telefone, prazo 7); `GET`/`PUT /api/online-shop/settings` só ADMIN; outro perfil vê o aviso e não pede (§3.5) | `OnlineShopUiHarnessTest` · `OnlineShopHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| LO-UI-02 | Desktop: caixa "Publicar na loja online" na ficha do artigo, lida do servidor, gravada por `PUT /api/online-shop/products/{id}/published` só quando muda (§3.5) | `OnlineShopUiHarnessTest` · `OnlineShopHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| LO-UI-03 | Desktop: encomendas com a marca "Loja online" (coluna Origem), detalhe com contacto, entrega e morada; alerta no sino só para os perfis de vendas, por encomenda online por facturar (§3.5) | `OnlineShopUiHarnessTest` |
