# Desktop cliente-fino — migração para HTTPS (Track B)

**Última actualização:** 2026-07-13
**Estado:** padrão estabelecido e provado (inclui **PDF-over-HTTP**); **11 de ~26 domínios** migrados. Restam os
painéis grandes (POS/Stock/Compras/Comercial). Enquanto a migração não fechar, o desktop mantém a
ligação directa à BD para os ecrãs por migrar — logo o PostgreSQL **ainda não pode** fechar-se.

## Objectivo

Hoje o desktop é uma app Spring Boot que **chama os Services em processo** e liga directamente ao
PostgreSQL. O alvo (ver [ARCHITECTURE.md](../ARCHITECTURE.md) e [DEPLOY_VPS_SPEC.md](DEPLOY_VPS_SPEC.md))
é o desktop falar **só HTTPS** com o backend hospedado: sem `DataSource`, BD 100% privada. Esta
iteração migra os painéis, **um domínio de cada vez**, de chamadas ao Service para clientes HTTP.

## Como funciona (o padrão, por domínio)

1. **Cobertura de endpoints:** confirmar que o `@RestController` do domínio expõe tudo o que o painel
   invoca no Service. Onde faltar, adicionar endpoint (skill `multicore-new-endpoint`).
2. **Cliente tipado** `XxxApiClient` (`@Component @Profile("desktop")`) sobre o `DesktopClientFactory`,
   espelhando o [ComercialApiClient](../src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java).
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
[DesktopApiClient](../src/main/java/mz/multicore/erp/desktop/client/DesktopApiClient.java): anexa
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
| Compras     | `PurchaseApiClient` estendido (colapsa purchase+order+reorder) + `getWarehousesByCompany` | ComprasPanel (1.º gigante; entidades Supplier/Warehouse→DTO) | ✅ |
| Plataforma  | `PlatformApiClient` (colapsa empresas+utilizadores+assinaturas+suporte) + 3 endpoints de options | PlataformaPanel (superadmin; só DTOs) | ✅ |
| Stock       | `InventoryApiClient` estendido + `StockTransferApiClient`/`InventoryCountApiClient`/`ProductCategoryApiClient` novos + `ComercialApiClient` (produtos/IVA) **+ 13 endpoints novos no backend** | StockPanel (2.º gigante, 2.633 linhas; `Stock`/`StockMovement`/`Warehouse` entidades→DTO) | ✅ |
| POS         | `POSApiClient` (novo; sessões/checkout/devoluções/recibo/Z) + extensões Comercial (barcode/vendáveis/pos-sales) / Inventory (armazéns-de-venda) / Promotion (melhor-promoção) **+ 6 endpoints novos no backend** | POSPanel (checkout com concorrência; `TillSession`/`Warehouse`/`Invoice` entidades→DTO) | ✅ |
| Comercial   | `ComercialApiClient` muito estendido (faturas/encomendas/recibos + prints) + `CreditNoteApiClient`/`DebitNoteApiClient`/`MovimentosApiClient` novos **+ ~18 endpoints novos no backend** | ComercialPanel (último e maior gigante, 2.657 linhas; `Receipt`/`Warehouse`/`Company` entidades→DTO/stub) | ✅ |
| Config (empresa) | 3 controllers novos (users/audit/backup) + 6 clientes | ConfigPanel (backup server-side; audit server-side) | ✅ |

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
- **Compras** foi o **1.º gigante** (1.324 linhas): `DesktopApiClient` ganhou `patch` (para o
  `PATCH /suppliers/{id}/active`); `PurchaseApiClient` colapsou os serviços de compras/encomendas/
  reposição; o painel converteu `Supplier`/`Warehouse`/`Purchase` (entidades) para os respetivos DTOs.
  O `PurchaseDTO` não traz o nome do armazém — resolvido por lookup na lista de armazéns.
- **Fiscal** foi o primeiro domínio que exigiu **endpoints novos no backend** (não só migração de UI):
  `GET /api/fiscal/saft/export` (DTO com metadados, além do `/saft` que só dá XML cru),
  `GET /api/fiscal/saft/validate` (validação contra a XSD) e `GET /api/print/payroll-fiscal-map` (PDF).
  Um `FiscalApiClient` colapsou os 8 serviços que o painel usava num só cliente.
- **Stock** foi o **2.º gigante** (2.633 linhas) e o mais pesado em navegação de grafo de entidades.
  Exigiu **13 endpoints novos no backend** (produtos criar/editar/imagem, taxas de IVA, armazéns
  all/editar/activar, movimentos, FEFO, esgotados, bloqueio de contagem) e **4 clientes**:
  `InventoryApiClient` (estendido, ~18 métodos incl. 3 PDFs via `getBytes`), `StockTransferApiClient`,
  `InventoryCountApiClient`, `ProductCategoryApiClient`; o `ComercialApiClient` ganhou
  criar/editar produto, imagem (`postBytes`) e `getActiveVatRates()`. O `DesktopApiClient` ganhou
  `postBytes` (octet-stream, para a imagem do produto). Os DTOs de resposta `StockDTO` e `WarehouseDTO`
  foram **enriquecidos** (flat-DTO) — `StockDTO` +categoryName/unitPrice/unitsPerBox; `WarehouseDTO`
  +type/allowsSales/manager/phone/active — para o painel deixar de navegar `stock.getProduct()…`.
  A entrada de lote deixou de construir um `Product` entidade no cliente e passa a `RegisterMovementRequest`.
- **Concorrência de stock (verificado no código, não só assumido):** faturar a descoberto está fechado
  **no servidor**, e a migração para cliente-fino **não o enfraqueceu** (o mesmo `ComercialService.createInvoice`
  corre no backend). Garantias: `@Version` (optimistic lock) em `Stock` **e** `ProductBatch`;
  `ProductBatchService.consumeFEFO` lança `BusinessRuleException("Stock insuficiente…")` dentro da
  transação da fatura. Dois utilizadores a vender o mesmo saldo → o 2.º é recusado (via consumeFEFO ou
  `OptimisticLockException`+rollback). O défice restante é só **UX** (tabela desactualizada): mitigar com
  botões *Atualizar* e recarregar-ao-abrir nos seletores de produto de Faturação/POS quando forem migrados.
- **POS** foi o **3.º gigante** (1.673 linhas). O `POSController` já existia (sessões/checkout/
  devoluções); precisou de **6 endpoints novos**: `GET /comercial/products/sellable`,
  `GET /comercial/products/by-barcode` (corpo `null` = não encontrado, preservando a semântica do
  painel), `GET /comercial/pos-sales`, `GET /inventory/warehouses/sales`, `GET /promotions/best`
  (com `AppliedPromotionDTO` novo) e `GET /print/pos-z-report/{sessionId}`. O `checkout` passou a
  devolver **`InvoiceDTO`** (antes só `Long`) para o painel ter número+total do documento para o recibo
  — o `POSController` passou a injectar `ComercialService` para o `toDTO(Invoice)`. Cliente novo
  `POSApiClient` (sessão activa via 204→`Optional.empty`, checkout, devolução, recibo/Z via `getBytes`);
  operador/barcode vão **URL-encoded**. `ScaleBarcodeParser` (parser puro, sem BD) permanece bean local
  do desktop. Entidades `TillSession`/`Warehouse`/`Invoice` → DTO; `CompanyService` (injectado mas nunca
  usado) foi removido do painel. No `MainFrame` removeram-se 3 params só-POS (receipt/promotion/Z-report).
  A **mesma garantia de concorrência do Stock** aplica-se ao checkout POS (`POSService.checkout` corre no
  servidor, dentro de transação, com `consumeFEFO`+`@Version`).
- **Comercial** foi o **4.º e último gigante** (2.657 linhas) e o de maior superfície de backend.
  Os controllers de notas de crédito/débito e de movimentos **já existiam** (só precisaram de clientes);
  todos os endpoints de impressão (fatura, encomenda, guia, NC, ND) **já existiam**. O grosso foi o
  `ComercialController`: **~16 endpoints novos** — faturas (`?companyId=`, `/search`, `/outstanding`,
  `/{id}/cancel`), **encomendas** (listar/pendentes/pesquisas/`{id}`, criar, `/bill`, `/cancel`,
  `/print`) e **recibos** (listar/criar/cancelar). Mais: `GET /credit-notes/returned-quantities`
  (com `ReturnedQtyDTO`; o cliente reconstrói o `Map<Long,BigDecimal>`) e
  `POST /pos/invoices/{id}/late-payment` (pagamento de fiado). `ReceiptDTO` novo + `toDTO(Receipt)`
  (não existia). O `POSController.checkout`/`late-payment` reutiliza o padrão já criado no POS.
  Clientes novos: `CreditNoteApiClient`, `DebitNoteApiClient`, `MovimentosApiClient`; `ComercialApiClient`
  ganhou ~20 métodos. `currentCompany()` deixou de usar `CompanyService` — o `TablePdfExporter` só precisa
  do id, resolvido com um `Company` stub (padrão herdado do StockPanel). Entidades `Receipt`/`Warehouse` → DTO.
- **Fecho do Track B:** com o Comercial migrado, **todos os 14 painéis** são cliente-fino. O `MainFrame`
  deixou de injectar 14 serviços de backend (comercial/inventory/finance/pos/movimentos + 5 print services
  + 2 note services + `CompanyService` morto + `SubscriptionService`→`MySubscriptionApiClient`) — já **não
  depende de nenhum `@Service`/`@Repository`**.
- **Runtime cliente-fino (fecha o objectivo):** o `DesktopApplication` deixou de arrancar o
  `MulticoreApplication`. É agora um contexto próprio, **não-web** (`WebApplicationType.NONE`), que
  **exclui** `DataSource`/JPA/Flyway e faz scan só de `mz.multicore.erp.desktop` + `mz.multicore.erp.gui` +
  `mz.multicore.erp.modules.pos.scale`. O `application-desktop.properties` perdeu toda a configuração de BD —
  fica só `desktop.api.base-url`. É `@Configuration @Profile("desktop") @EnableAutoConfiguration` (e **não**
  `@SpringBootApplication`) de propósito: assim não é um `@SpringBootConfiguration` que polua a descoberta
  de contexto dos testes, e o `@Profile` impede que as exclusões vazem para o contexto do backend quando
  este faz scan de `mz.multicore.erp`. **Prova automática:** `DesktopThinContextTest` — o contexto arranca **sem
  nenhum `DataSource`** e sem `@Service`/`@Repository` de backend, só com os clientes HTTP.
  Consequência: **o PostgreSQL pode agora ser fechado ao exterior** (só o backend lhe acede).
- `DesktopApiClientTest` — teste de contrato da camada partilhada (headers, token, empresa, parse de
  objecto/lista, mapeamento de erro).

## Limite honesto

- **Verificado: compilação + ligação.** O **ida-e-volta HTTP real** de cada painel só se confirma com o
  desktop a correr contra o backend (validação manual — ver harness TC-50+).
- A lógica de negócio (Services) **não muda** — sem risco de regressão nos testes existentes.
- **Feito:** todos os painéis migrados **e** o runtime desktop já não configura `DataSource` (ver
  `DesktopThinContextTest`). O PostgreSQL pode ser fechado ao exterior — só o backend lhe acede.
- **Por validar ao vivo:** o ida-e-volta HTTP real de cada painel confirma-se com o desktop a correr
  contra um backend a sério (harness TC-50+). Verificado até agora: compilação, suite de testes completa,
  e o *backend* do Stock contra PostgreSQL real. Os fluxos de dinheiro (checkout POS, emissão de fatura)
  ainda não foram exercidos ao vivo ponta-a-ponta.
