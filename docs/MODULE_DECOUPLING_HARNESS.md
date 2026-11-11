# Harness — Desacoplamento entre Módulos

SPEC: `docs/MODULE_DECOUPLING_SPEC.md` · regras de base: `docs/CLEAN_ARCHITECTURE_RULES_SPEC.md` (CA-07, DG-08).

Em todos os critérios de migração: o módulo consumidor deixa de importar o Repository alheio, usa o Lookup
ou o Service dono, apaga as suas linhas da baseline do CA-07 e mantém a suite desse módulo verde.
A coluna "Teste" indica a prova nova; `CleanArchitectureHarnessTest` e `ModuleDecouplingHarnessTest`
correm sempre no gate.

## GOAL-001 — Empresa (`company.repository.*`, 106)

| ID | Garantia | Teste |
|---|---|---|
| MD-C01 | `CompanyLookup` em `company.service`: `require`, `reference`, `findAll`, `findByTaxId`, `lockForUpdate` (exige a transacção do chamador), mais `requireActive`, `find` e `count`; mensagens PT-MZ normalizadas; escritas alheias com `CompanyService.saveCompany` — **feito** | `CompanyLookupTest` |
| MD-C02 | `hr` (21 ficheiros) sem `CompanyRepository`; linhas removidas do baseline CA-07 — **feito** | `ModuleDecouplingHarnessTest` |
| MD-C03 | `inventory` (11) e `comercial` (9) sem `CompanyRepository`; escrita do trancamento de stock via `CompanyService.saveCompany` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-C04 | `purchases`, `platform`, `fleet`, `users` sem `CompanyRepository` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-C05 | `manufacturing`, `crm`, `assets`, `architecture`, `support`, `subscription`, `recurring`, `promotions`, `pos`, `performance`, `intelligence`, `backup`, `approvals` sem `CompanyRepository`; abertura de caixa POS tranca a empresa por `CompanyLookup.lockForUpdate` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-C06 | Restantes módulos com 1 ficheiro (weighbridge, variants, units, returnables, projects, printing, pricing, numbering, monitoring, fiscal, expiry, butchery, bulkstock, advances, activity, accounting) sem `CompanyRepository` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-C07 | Zero ligações a `company.repository.*` fora de `company` (verificado pela onda GOAL-001 em `DONE`); suite completa verde — **feito** | `ModuleDecouplingHarnessTest` |

## GOAL-002 — Produto e categoria (50)

| ID | Garantia | Teste |
|---|---|---|
| MD-P01 | `ProductLookup` (inclui categorias) em `comercial.service`: `requireForCompany`, `listForCompany`, `findBySkuForCompany`, `findByBarcode`, `findAllById` (mais `findForCompany`, `require`/`find`, `findBySku`, `findByReferenceForCompany`, `findByBarcodeForCompany`, `countByBusinessUnit` e categorias `require/findCategoryForCompany`, `findAllCategories`, `countCategories`); gravações alheias via `ProductCommandService.saveProduct`/`saveCategory` — **feito** | `ProductLookupTest` |
| MD-P02 | `inventory` (12) sem Product/ProductCategory Repository; custo médio grava via `ProductCommandService.saveProduct` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-P03 | `purchases`, `crm`, `architecture`, `promotions`, `pricing`, `pos`, `manufacturing`, `backup` | `ModuleDecouplingHarnessTest` |
| MD-P04 | Restantes (weighbridge, variants, units, returnables, expiry, consignment, butchery, bulkstock, advances) | `ModuleDecouplingHarnessTest` |
| MD-P05 | Zero ligações a Product/ProductCategory Repository fora de `comercial`; suite verde | `ModuleDecouplingHarnessTest` |

## GOAL-003 — Documentos comerciais e clientes (79)

| ID | Garantia | Teste |
|---|---|---|
| MD-V01 | `ClientLookup` e migração de todos os leitores de `ClientRepository` (15) | `ClientLookupTest` |
| MD-V02 | `InvoiceLookup` e migração de `inventory` e `printing` | `InvoiceLookupTest` |
| MD-V03 | Restantes leitores de `InvoiceRepository` (30 no total) | `ModuleDecouplingHarnessTest` |
| MD-V04 | `OrderLookup` cobre encomendas, cotações e guias de entrega; leitores migrados | `OrderLookupTest` |
| MD-V05 | Recibos, notas de crédito e de débito por Lookup do agregado mais próximo; leitores migrados | `ModuleDecouplingHarnessTest` |
| MD-V06 | Zero ligações a `comercial.repository.*` fora de `comercial`; suite verde | `ModuleDecouplingHarnessTest` |

## GOAL-004 — Inventário (65)

| ID | Garantia | Teste |
|---|---|---|
| MD-I01 | `WarehouseLookup` e migração dos leitores de `WarehouseRepository` (29) | `WarehouseLookupTest` |
| MD-I02 | `StockLookup` só de leitura; toda a escrita de stock alheia passa pelo serviço de movimentos (ledger) | `StockLookupTest` |
| MD-I03 | Leitores de `StockRepository` (20) migrados sem mudar saldos nem movimentos | `ModuleDecouplingHarnessTest` |
| MD-I04 | Quebras, transferências, lotes e movimentos (16) por Lookup/Service do inventário | `ModuleDecouplingHarnessTest` |
| MD-I05 | Zero ligações a `inventory.repository.*` fora de `inventory`; suite verde | `ModuleDecouplingHarnessTest` |

## GOAL-005 — Restantes módulos (95)

| ID | Garantia | Teste |
|---|---|---|
| MD-R01 | `users`: `AppUserLookup` e leitores migrados | `AppUserLookupTest` |
| MD-R02 | `hr`: `EmployeeLookup` e leitores migrados | `EmployeeLookupTest` |
| MD-R03 | `purchases`: `SupplierLookup` (e documentos de compra) e leitores migrados | `SupplierLookupTest` |
| MD-R04 | `pos`: Lookup das sessões/vendas POS e leitores migrados | `ModuleDecouplingHarnessTest` |
| MD-R05 | `financeira`: `TreasuryAccountLookup` e leitores migrados | `TreasuryAccountLookupTest` |
| MD-R06 | `fleet`, `fiscal`, `approvals`, `manufacturing`, `assets`, `recurring`, `performance`, `audit`, `quality`, `crm` | `ModuleDecouplingHarnessTest` |
| MD-R07 | Zero imports de Repository de outro módulo em todo o backend; baseline do CA-07 vazia; suite verde | `ModuleDecouplingHarnessTest` |
