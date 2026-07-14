# Desktop cliente-fino — migração para HTTPS (Track B)

**Última actualização:** 2026-07-13
**Estado:** padrão estabelecido e provado (inclui **PDF-over-HTTP**); **8 de ~26 domínios** migrados. Restam os
painéis grandes (POS/Stock/Compras/Comercial). Enquanto a migração não fechar, o desktop mantém a
ligação directa à BD para os ecrãs por migrar — logo o PostgreSQL **ainda não pode** fechar-se.

## Objectivo

Hoje o desktop é uma app Spring Boot que **chama os Services em processo** e liga directamente ao
PostgreSQL. O alvo (ver [ARCHITECTURE.md](../ARCHITECTURE.md) e [DEPLOY_VPS_SPEC.md](DEPLOY_VPS_SPEC.md))
é o desktop falar **só HTTPS** com o backend hospedado: sem `DataSource`, BD 100% privada. Esta
iteração migra os painéis, **um domínio de cada vez**, de chamadas ao Service para clientes HTTP.

## Como funciona (o padrão, por domínio)

1. **Cobertura de endpoints:** confirmar que o `@RestController` do domínio expõe tudo o que o painel
   invoca no Service. Onde faltar, adicionar endpoint (skill `phc-new-endpoint`).
2. **Cliente tipado** `XxxApiClient` (`@Component @Profile("desktop")`) sobre o `DesktopClientFactory`,
   espelhando o [ComercialApiClient](../src/main/java/com/phcpro/desktop/client/ComercialApiClient.java).
   Métodos devolvem os **mesmos DTOs** que o Service (a API serializa-os tal e qual).
3. **Painel** deixa de receber `XxxService` e passa a receber `XxxApiClient`; cada `service.m(...)`
   vira `apiClient.m(...)`.
4. **Carregamento preguiçoso:** a 1ª leitura vai para `onPanelSelected()` (chamado pelo `navigate()`),
   nunca para o construtor — uma chamada HTTP no arranque falharia para quem não tem empresa activa
   (sem `X-Company-Id` o servidor recusa). Padrão do `ClientesPanel`.
5. **Ligação no `MainFrame`:** injectar o `XxxApiClient` (bean do perfil desktop, auto-wired) e passá-lo
   ao painel. O `XxxService` **mantém-se** enquanto outro painel ainda por migrar o usar (ex.: o
   `DashboardPanel` agrega vários Services).

Toda a canalização partilhada vive no
[DesktopApiClient](../src/main/java/com/phcpro/desktop/client/DesktopApiClient.java): anexa
`Authorization: Bearer <token>` e `X-Company-Id` da sessão, serializa/deserializa JSON e traduz
respostas não-2xx em `ApiClientException` com a mensagem do servidor. Ganhou `getList`/`post`/`put`/
`delete` +, para o RH, **`postForList`** (POST → array) e **`getBytes`** (GET → PDF). Como os endpoints
de impressão já existiam (`/api/print/**`), o **PDF-over-HTTP** reduziu-se a este único método — destrava
as impressões de todos os painéis por migrar (Fiscal, Comercial, POS, Stock, Compras).

## Progresso

| Domínio     | Cliente HTTP                              | Painel           | Estado |
|-------------|-------------------------------------------|------------------|--------|
| Clientes    | `ComercialApiClient` (clients)            | ClientesPanel    | ✅ (pré-existente) |
| Aprovações  | `ApprovalApiClient`                       | ApprovalsPanel   | ✅ |
| CRM         | `CRMApiClient`                            | CRMPanel         | ✅ |
| Financeiro  | `FinanceApiClient` + `getAllInvoices()`   | FinanceiroPanel  | ✅ |
| Promoções   | `PromotionApiClient` + `getAllProducts()`/`getActiveCategories()` no ComercialApiClient | PromotionsPanel (sub-tab de Comercial) | ✅ |
| Dashboard   | `InventoryApiClient` + `PurchaseApiClient` (novos) + reutiliza os outros | DashboardPanel (só-leitura; passou a consumir DTOs, não entidades) | ✅ |
| RH          | `HRApiClient` (~16 métodos + recibo PDF via `getBytes`)  | HRPanel          | ✅ |
| Fiscal      | `FiscalApiClient` (colapsa 8 serviços) **+ 3 endpoints novos no backend** | FiscalPanel | ✅ |
| **POS / Stock / Compras / Comercial** | —             | —                | ⬜ (grandes, risco) |
| Plataforma / Config (superadmin) | —                  | —                | ⬜ |

## Peças

- Clientes novos: `ApprovalApiClient`, `CRMApiClient`, `FinanceApiClient`, `PromotionApiClient`,
  `InventoryApiClient`, `PurchaseApiClient`. `ComercialApiClient` estendido com `getAllInvoices()`,
  `getAllProducts()`, `getActiveCategories()`.
- **Dashboard** consumia **entidades JPA** (`Stock`, `Purchase`) — passou a consumir os **DTOs** dos
  endpoints (`StockDTO`, `PurchaseDTO`), alinhando com a regra "DTO na fronteira". Arranque resiliente
  (try/catch: backend em baixo não bloqueia o login). Ao migrar, os beans `ApprovalService`/`CRMService`
  deixaram de ser injectados no `MainFrame` (nenhum painel os usa já) e foram removidos de lá.
- **Promoções** é sub-tab do `ComercialPanel`: os clientes são passados através do construtor do
  `ComercialPanel` (que **não** foi migrado — continua a usar `ComercialService` nas suas tabs).
- **Fiscal** foi o primeiro domínio que exigiu **endpoints novos no backend** (não só migração de UI):
  `GET /api/fiscal/saft/export` (DTO com metadados, além do `/saft` que só dá XML cru),
  `GET /api/fiscal/saft/validate` (validação contra a XSD) e `GET /api/print/payroll-fiscal-map` (PDF).
  Um `FiscalApiClient` colapsou os 8 serviços que o painel usava num só cliente.
- `DesktopApiClientTest` — teste de contrato da camada partilhada (headers, token, empresa, parse de
  objecto/lista, mapeamento de erro).

## Limite honesto

- **Verificado: compilação + ligação.** O **ida-e-volta HTTP real** de cada painel só se confirma com o
  desktop a correr contra o backend (validação manual — ver harness TC-50+).
- A lógica de negócio (Services) **não muda** — sem risco de regressão nos testes existentes.
- **Só se pode fechar o PostgreSQL ao exterior quando TODOS os painéis estiverem migrados** e o perfil
  `desktop` deixar de configurar `DataSource`. Até lá, backend hospedado + BD acessível por VPN.
- Os painéis grandes (POS/Stock/Compras/Comercial) exigem endpoints novos e migração cuidadosa,
  um a um — são o grosso do trabalho restante.
