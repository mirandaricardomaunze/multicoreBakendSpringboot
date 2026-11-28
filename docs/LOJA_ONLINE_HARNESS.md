# Harness — Loja online

> Cenários que provam [LOJA_ONLINE_SPEC.md](LOJA_ONLINE_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-28 (LO-01 verde: `OnlineShopSettingsTest`, migração V162; LO-02 verde: `OnlineShopCatalogTest`, migração V163; LO-03 verde: `OnlineShopCatalogTest`)

| ID | Garantia | Teste |
|---|---|---|
| LO-01 | Activação: código público único e válido, lojas de levantamento, zonas com taxa ≥ 0, prazo (omissão 7); só ADMIN (§3.1) | `OnlineShopSettingsTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| LO-02 | Só artigos activos com "Publicar na loja online"; campo desligado por omissão (§3.1) | `OnlineShopCatalogTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| LO-03 | Disponível/Esgotado pelo stock disponível nas lojas de levantamento; sem quantidades, custos nem fornecedores (§3.2) | `OnlineShopCatalogTest` |
| LO-04 | `/api/shop/{código}` sem login; loja desactivada indisponível; limites por IP e honeypot (§3.2) | `OnlineShopHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| LO-05 | Checkout valida nome, telemóvel MZ, NUIT opcional, modo, zona e morada (§3.3) | `OnlineShopOrderTest` |
| LO-06 | Encomenda pelo serviço da API pública: preços do sistema, reserva, cliente por NUIT ou Consumidor Final com contacto, origem `LOJA_ONLINE` (§3.3) | `OnlineShopOrderTest` |
| LO-07 | Armazém pela loja de levantamento ou primeira com stock na entrega; sem stock recusa sem criar (§3.3) | `OnlineShopOrderTest` |
| LO-08 | Linha "Taxa de entrega — zona" com a taxa e o IVA normal (§3.3) | `OnlineShopOrderTest` |
| LO-09 | Consulta por número + telemóvel; telemóvel errado não revela a encomenda (§3.3) | `OnlineShopHttpIntegrationTest` |
| LO-10 | Cancelamento automático após o prazo, liberta reserva, com auditoria (§3.4) | `OnlineShopExpiryTest` |
| LO-11 | Página pública: sem CDN/inline, CSP, `textContent`, 360 px, carrinho só com SKUs e quantidades (§3.2) | `OnlineShopStaticAssetsHarnessTest` |
| LO-UI-01 | Desktop: activação e definições da loja, publicar artigo, encomendas marcadas "Loja online" com alerta (§3.1, §3.4) | `OnlineShopUiHarnessTest` |
