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
| MD-P03 | `purchases`, `crm`, `architecture`, `promotions`, `pricing`, `pos`, `manufacturing`, `backup`; CRM e seeders gravam via `ProductCommandService` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-P04 | Restantes (weighbridge, variants, units, returnables, expiry, consignment, butchery, bulkstock, advances); units, variants e advances gravam via `ProductCommandService` — **feito** | `ModuleDecouplingHarnessTest` |
| MD-P05 | Zero ligações a Product/ProductCategory Repository fora de `comercial` (verificado pela onda GOAL-002 em `DONE`); suite verde — **feito** | `ModuleDecouplingHarnessTest` |

## GOAL-003 — Documentos comerciais e clientes (79)

| ID | Garantia | Teste |
|---|---|---|
| MD-V01 | `ClientLookup` em `comercial.service`: `requireForCompany`/`findForCompany`, `require`/`find`, `listForCompany`; "Cliente não encontrado." normalizado; escrita do seeder via `ClientCommandService.saveClient`; migração de todos os leitores de `ClientRepository` (15) — **feito** | `ClientLookupTest` |
| MD-V02 | `InvoiceLookup` (`require`/`find`, "Fatura não encontrada.", `listCreatedBetween`) e migração de `inventory` (2) e `printing` (3) — **feito** | `InvoiceLookupTest` |
| MD-V03 | Restantes leitores de `InvoiceRepository` (30 no total): `InvoiceLookup` alargado (`listForCompany`/`listAll`/`listByStatus`/`findByContingencyReference`/`sumQuantitySoldByProductSince`) e `InvoiceCommandService` (`lockForCompany`/`save`) para as escritas — **feito** | `ModuleDecouplingHarnessTest`, `InvoiceCommandServiceTest` |
| MD-V04 | `OrderLookup` cobre encomendas, cotações e guias de entrega; `OrderCommandService` (`saveOrder`, `markQuotationConvertedByPos`) para as escritas; leitores migrados (20, incluindo o wildcard do `PosCheckoutAndPaymentService`) — **feito** | `OrderLookupTest` |
| MD-V05 | Recibos, notas de crédito e de débito por Lookup do agregado mais próximo: `ReceiptLookup` (`find`, `listForCompany`) + `ReceiptCommandService.save`; `InvoiceNoteLookup` (crédito e débito) + `InvoiceNoteCommandService.markCreditNotePosReturn`; leitores migrados (14) — **feito** | `ModuleDecouplingHarnessTest`, `ReceiptLookupTest`, `InvoiceNoteLookupTest` |
| MD-V06 | Zero ligações a `comercial.repository.*` fora de `comercial`; suite verde — **feito** (onda GOAL-003 imposta pelo harness: 0 ligações) | `ModuleDecouplingHarnessTest` |

## GOAL-004 — Inventário (65)

| ID | Garantia | Teste |
|---|---|---|
| MD-I01 | `WarehouseLookup` em `inventory.service` (`require`/`find`, `requireForCompany`/`findForCompany`, `listForCompany`) e migração dos leitores de `WarehouseRepository` (29); escrita do seeder via `WarehouseCommandService.saveWarehouse` — **feito** | `WarehouseLookupTest` |
| MD-I02 | `StockLookup` só de leitura (`find`, `quantityOf`, `listForCompany`/`listForWarehouse`/`listForProduct`); toda a escrita de stock alheia passa pelo serviço de movimentos (ledger) — seeder demo migrado — **feito** | `StockLookupTest` |
| MD-I03 | Leitores de `StockRepository` (18) migrados sem mudar saldos nem movimentos; `StockLookup.findForReservation` (lock pessimista, exige a transacção do chamador) para a verificação de faltas da produção — **feito** | `ModuleDecouplingHarnessTest`, `StockLookupTest` |
| MD-I04 | Quebras, transferências, lotes e movimentos (16) por Lookup/Service do inventário: `StockWasteLookup`, `StockTransferLookup`, `ProductBatchLookup` (`requireForCompany`: "Lote não encontrado." / "Lote não encontrado nesta empresa."), `StockMovementLookup` — **feito** | `ModuleDecouplingHarnessTest`, `InventoryDocumentLookupsTest` |
| MD-I05 | Zero ligações a `inventory.repository.*` fora de `inventory`; suite verde — **feito** (onda GOAL-004 imposta pelo harness: 0 ligações) | `ModuleDecouplingHarnessTest` |

## GOAL-005 — Restantes módulos (95)

| ID | Garantia | Teste |
|---|---|---|
| MD-R01 | `users`: `AppUserLookup` (`require`/`find`, `requireByUsername`/`findByUsername`, `listAll`, `listForCompany`, `countActiveByCompanyAndRole`, `countAccessesForCompany`; "Utilizador não encontrado.") e leitores migrados (15 ligações); escritas alheias (plataforma, seeders) via `AppUserCommandService.saveUser`/`saveUsers` — **feito** | `AppUserLookupTest` |
| MD-R02 | `hr`: `EmployeeLookup` (`require`/`find`, `requireForCompany`/`findForCompany`, `listForCompany`, `listByStatusForCompany`, mais `findPayslip` e `requireContractForCompany`; "Colaborador não encontrado." / "Colaborador não encontrado nesta empresa." / "Contrato não encontrado.") e leitores migrados (14 ligações); escritas alheias (seeder, prémio na folha) via `EmployeeCommandService.saveEmployee`/`savePayslip` — **feito** | `EmployeeLookupTest` |
| MD-R03 | `purchases`: `SupplierLookup` (`require`/`find`, "Fornecedor não encontrado."; `listForCompany`, `listActiveForCompany`; compras e encomendas: `listPurchasesForCompany`, `listAllPurchases`, `listPurchaseOrdersForCompany`) e `BusinessUnitLookup.findForCompany`; leitores migrados (16 ligações); escrita do seeder via `SupplierCommandService.saveSupplier` — **feito** | `SupplierLookupTest` |
| MD-R04 | `pos`: `TillSessionLookup` (`listForCompany`, `listMovementsForCompany`) e `PosPaymentLookup` (`listForInvoice`, `listPaidForCompanyBetween`; vales e pagamentos móveis: `requireVoucher` "Vale de compras não encontrado.", `findVoucherByCode`, `findMobilePaymentForCompany`); leitores migrados (12 ligações) — **feito** | `ModuleDecouplingHarnessTest`, `PosLookupsTest` |
| MD-R05 | `financeira`: `TreasuryAccountLookup` (`requireForCompany`, "Conta de tesouraria não encontrada."; `listForCompany`) e leitores migrados (9 ligações); escrita do seeder via `TreasuryAccountProvisioningService.openBankAccount(..., openingBalance)` — **feito** | `TreasuryAccountLookupTest` |
| MD-R06 | `fleet`, `fiscal`, `approvals`, `manufacturing`, `assets`, `recurring`, `performance`, `audit`, `quality`, `crm`: `TaxRateLookup` (+ `TaxRateCommandService.saveTaxRate`), `AuditLogLookup`, `ApprovalRequestLookup`, `AssetLookup`, `RecurringContractLookup`, `VehicleLookup` (abastecimentos e manifestos), `ProductionOrderLookup`, `BillOfMaterialsLookup`, `SalesGoalBonusLookup` (+ `SalesGoalBonusCommandService.settleOnPayslip`), `WorkerEquipmentAssignmentLookup`, `WorkSheetLookup`; leitores migrados (28 ligações) — **feito** | `ModuleDecouplingHarnessTest`, `RemainingOwnerLookupsTest` |
| MD-R07 | Zero imports de Repository de outro módulo em todo o backend; baseline do CA-07 vazia; suite verde | `ModuleDecouplingHarnessTest` |
