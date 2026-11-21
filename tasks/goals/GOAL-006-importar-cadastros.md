---
id: GOAL-006
title: "Importar clientes, fornecedores e artigos de outro ERP por CSV"
status: IN_PROGRESS
priority: HIGH
spec: docs/IMPORTACAO_DADOS_SPEC.md
harness: docs/IMPORTACAO_DADOS_HARNESS.md
verify: ["CsvImportReaderTest", "ImportValueParserTest", "ClientImportServiceTest", "ClientOptionalEmailTest", "SupplierImportServiceTest", "SupplierLookupTest", "ProductImportServiceTest", "ProductLookupTest", "DataImportHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "ModuleDecouplingHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "DataImportPanelHarnessTest"]
blockedBy: []
---

# Objectivo

Uma empresa que vem do Primavera, do PHC ou do Excel carrega os seus clientes, fornecedores e artigos
a partir de um CSV, vê antes o que vai entrar e o que está errado, e só grava quando o ficheiro está
limpo. Regras em `docs/IMPORTACAO_DADOS_SPEC.md` §2–§4.

Módulo novo `modules/dataimport` (controller/service/dto), que escreve só pelos serviços donos de
cada agregado. O leitor CSV e o parser de valores são classes puras, sem Spring.

## Critérios de aceitação

- [x] IMP-01 — UTF-8 com/sem BOM e Windows-1252 com acentos intactos — `CsvImportReaderTest`
- [x] IMP-02 — separador `;`/`,` detectado, aspas e `""` — `CsvImportReaderTest`
- [x] IMP-03 — colunas por nome tolerante; obrigatória em falta recusa; extra anunciada — `CsvImportReaderTest`
- [x] IMP-05 — limites 2 MB / 5 000 linhas; linhas vazias ignoradas — `CsvImportReaderTest`
- [x] IMP-04 — números e datas nos formatos aceites; outro formato é erro com nº de linha — `ImportValueParserTest`
- [x] IMP-CL-03 — email do cliente opcional (migração, request, registo manual) — `ClientOptionalEmailTest`
- [x] IMP-CL-01 — cliente: NUIT obrigatório e válido, email opcional — `ClientImportServiceTest`
- [x] IMP-CL-02 — cliente: `JÁ EXISTE`, `ASSOCIADO` e repetido no ficheiro — `ClientImportServiceTest`
- [x] IMP-FO-01 — fornecedor: NUIT, `JÁ EXISTE` e repetido no ficheiro — `SupplierImportServiceTest`
- [x] IMP-AR-01 — artigo: IVA existente por percentagem ou código — `ProductImportServiceTest`
- [x] IMP-AR-02 — artigo: categoria nova criada só na confirmação — `ProductImportServiceTest`
- [x] IMP-AR-03 — artigo: SKU existente ignorado; código de barras/referência alheios são erro — `ProductImportServiceTest`
- [x] IMP-09 — modelo CSV por entidade (UTF-8 BOM, `;`, só cabeçalho) — `DataImportHttpIntegrationTest`
- [x] IMP-06 — pré-visualizar não grava; confirmar revalida, tudo ou nada, uma transacção — `DataImportHttpIntegrationTest`
- [x] IMP-07 — só a empresa activa — `DataImportHttpIntegrationTest`
- [x] IMP-08 — auditoria `DATA_IMPORT` por confirmação — `DataImportHttpIntegrationTest`
- [ ] IMP-10 — rotas só ADMIN e classificadas na `ApiRolePolicy` — `ApiRolePolicyEndpointCoverageHarnessTest`
- [ ] IMP-11 — sem Repository de outro módulo — `ModuleDecouplingHarnessTest`
- [ ] IMP-UI-02 — formulário de cliente aceita email vazio — `DataImportPanelHarnessTest`
- [ ] IMP-UI-01 — ecrã "Importar dados" em Configurações, Confirmar só sem erros — `DataImportPanelHarnessTest`

## Fora de âmbito

- Stock inicial e contas correntes em aberto (GOAL-007).
- Excel `.xlsx`, mapeamento livre de colunas, actualizar registos existentes (SPEC §6).

## Perguntas em aberto

- **2026-11-20 — Resposta (responsável via Claude):** falso alarme. O pacote `units/dto` está no git desde `380425a`; `mvn clean test-compile` na worktree passa (exit 0). A falha veio de um build intermédio. Desbloqueado: continuar.

- **2026-11-19 — Build do backend partido por código alheio a este goal.** O commit `3218952`
  (`feat(sectores): unidades e precos no POS e nas compras`) usa o pacote
  `mz.multicore.erp.modules.units.dto` (`UnitBarcodeItemDTO`, `UnitConversionDTO`, `UnitOfMeasureDTO`,
  `CreateUnitOfMeasureRequest`, …) mas nenhum ficheiro desse pacote está no git. Com o `target/` actual
  falha `UnitBarcodeItemDTO`; num build limpo (o gate) falha o pacote inteiro. Sem compilar não dá para
  provar `CsvImportReaderTest` verde, por isso IMP-02 ficou implementado mas **não marcado**.
  Opções:
  1. **(recomendado)** Quem fez o `3218952` commita os DTOs que ficaram na sua cópia de trabalho; o
     loop retoma e só tem de correr o teste e marcar IMP-02.
  2. A IA recria os DTOs a partir dos usos (controller, serviços e `SectorFoundationHttpIntegrationTest`).
     Arriscado: forma da API inventada, e é trabalho de outro goal.
  Para desbloquear: escolher a opção, voltar a `status: IN_PROGRESS` e esvaziar `blockedBy`.

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-19 · IMP-01 · `CsvImportReader.decode` (módulo `dataimport`, classe pura): UTF-8 com/sem BOM e fallback Windows-1252 com acentos intactos; `CsvImportReaderTest` verde
- 2026-11-19 · IMP-02 · `CsvImportReader.detectSeparator`/`parseLine` + 6 testes escritos (falharam antes da implementação); não marcado porque o backend não compila (`units/dto` em falta, ver Perguntas em aberto) → BLOCKED
- 2026-11-21 · IMP-02 · build volta a compilar; `CsvImportReaderTest` verde (10/10, incl. os 6 de IMP-02) → marcado
- 2026-11-21 · IMP-03 · `CsvImportReader.readHeader` + record `CsvHeader`: colunas por nome sem maiúsculas/acentos/espaços, obrigatória em falta → `BusinessRuleException` PT-MZ, extras em `extraColumns`; `CsvImportReaderTest` verde (14/14)
- 2026-11-21 · IMP-05 · `CsvImportReader.decode` recusa > 2 MB; `lines()` + record `CsvLine` ignora linhas vazias, numera com o cabeçalho como linha 1 e recusa > 5 000 linhas de dados; `CsvImportReaderTest` verde (19/19). Nota: correr os testes a partir da raiz (`mvn -f ../pom.xml test -pl backend -am ...`) — só em `backend/` usa um `contracts` desactualizado do `~/.m2` e falha `units/dto` (era a causa do falso alarme de 2026-11-19)
- 2026-11-21 · IMP-04 · `ImportValueParser` (classe pura): `parseDecimal` aceita `1.234,56` e `1234.56`, `parseDate` aceita `dd/mm/aaaa` e `aaaa-mm-dd` (STRICT, 31/02 é erro), `parseText` retira espaços; célula vazia → `null`; outro formato → `ImportValueException` (estende `BusinessRuleException`) com linha e coluna; `ImportValueParserTest` verde (31/31)
- 2026-11-21 · IMP-CL-03 · `SaveClientRequest` sem `@NotBlank` no email (compact constructor: vazio/espaços → `null`, resto `strip()`; `@Email` mantém-se), `Client.email` nullable, migração `V137__client_email_optional.sql` (`ALTER TABLE clients ALTER COLUMN email DROP NOT NULL`); registo e edição manual aceitam cliente sem email. `ClientOptionalEmailTest` verde (6/6, H2 em memória própria — a H2 em ficheiro com `ddl-auto=update` mantém o `NOT NULL` antigo), `MigrationPostgresDialectHarnessTest` e `InputSecurityAndBackendValidationHarnessTest` verdes
- 2026-11-21 · IMP-CL-01 · `ClientImportService.preview` (sem gravar): cada linha vira o mesmo `SaveClientRequest` do registo manual e passa pelas mesmas anotações (`ImportRequestValidator`, primeira violação pela ordem das colunas) e pelo `TaxIdValidator`; valor mal formatado → `ERRO` com coluna; prazo tem de ser inteiro. `CsvImportReader.read` + record `CsvTable` (reutilizável por fornecedores/artigos); DTOs `ImportPreviewDTO`/`ImportRowDTO`/`ImportRowStatus` em `contracts`. `ClientImportServiceTest` verde (16/16), `CsvImportReaderTest`, `CleanArchitectureHarnessTest` e `ModuleDecouplingHarnessTest` verdes. Ainda só classifica `NOVO`/`ERRO` — `JÁ EXISTE`/`ASSOCIADO`/repetido é IMP-CL-02
- 2026-11-21 · IMP-CL-02 · `ClientImportService.preview` exige empresa activa e classifica pela chave NUIT através da porta de leitura do comercial (`ClientLookup.existsForCompanyByTaxId`/`existsByTaxId`, sem Repository alheio): na empresa → `JÁ EXISTE`, só noutra → `ASSOCIADO`, senão `NOVO`; `DuplicateKeys` (classe pura, reutilizável por fornecedores/artigos) marca `ERRO` na coluna `nuit` em todas as linhas com o NUIT repetido ("NUIT repetido no ficheiro (linhas 2, 4)."), mesmo que já exista; linha com erro próprio mantém esse erro. `ClientImportServiceTest` verde (22/22), `ClientOptionalEmailTest`, `ModuleDecouplingHarnessTest` e `CleanArchitectureHarnessTest` verdes
- 2026-11-21 · IMP-FO-01 · `SupplierImportService.preview` (sem gravar, exige empresa activa): cada linha vira o `CreateSupplierRequest` do registo manual (mesmas anotações, incl. `@ValidPhoneMZ` no `telefone`) + `TaxIdValidator`; chave NUIT na empresa pela porta das compras (`SupplierLookup.existsForCompanyByTaxId` → `SupplierRepository.existsByTaxIdAndCompanyId`) → `JÁ EXISTE`, senão `NOVO` (sem `ASSOCIADO`: fornecedores não são partilhados); repetido no ficheiro → `ERRO` via `DuplicateKeys`. Verificação de NUIT extraída para `ImportTaxIdRule` (partilhada com clientes). `SupplierImportServiceTest` verde (14/14), `SupplierLookupTest` (8/8), `ClientImportServiceTest` (22/22), `ModuleDecouplingHarnessTest` e `CleanArchitectureHarnessTest` verdes
- 2026-11-21 · IMP-AR-01 · `ProductImportService.preview` (sem gravar, exige empresa activa): cada linha vira o `CreateProductRequest` do registo manual; valores obrigatórios `sku`/`nome`/`preco_venda`/`iva`/`categoria` pela ordem das colunas (PT-MZ, coluna no erro); `ImportVatRateResolver` (classe pura) resolve o `iva` entre as taxas IVA activas da empresa lidas pela porta fiscal (`TaxRateLookup.listActiveForCompany`, sem Repository alheio): código sem distinguir maiúsculas ou percentagem (`16`, `16,00`, `16%`); inexistente, taxa não-IVA ou percentagem partilhada por duas taxas (ambígua, lista os códigos) → `ERRO` na coluna `iva`. Categoria e SKU ainda não classificados (IMP-AR-02/03). `ProductImportServiceTest` verde (19/19), `SupplierImportServiceTest`, `ClientImportServiceTest`, `ModuleDecouplingHarnessTest` e `CleanArchitectureHarnessTest` verdes
- 2026-11-21 · IMP-AR-02 · `ImportCategoryPlan` (classe pura) reconhece a `categoria` pelo nome sem maiúsculas/acentos/espaços entre as categorias da empresa activa, lidas pela porta do comercial (`ProductLookup.listCategoriesForCompany`, activas e inactivas, sem Repository alheio); existente → `categoryId` no `CreateProductRequest`; inexistente não é erro: só a categoria de linhas sem erro entra em `ImportPreviewDTO.categoriesToCreate` (uma vez, grafia da primeira linha, ordem do ficheiro). A pré-visualização não grava nada; a criação fica para a confirmação (IMP-06). Normalização de nomes extraída para `ImportText.comparisonKey` (partilhada com o cabeçalho do `CsvImportReader`). `ProductImportServiceTest` verde (29/29), `ProductLookupTest` (14/14), `CsvImportReaderTest`, `ClientImportServiceTest`, `SupplierImportServiceTest`, `ModuleDecouplingHarnessTest` e `CleanArchitectureHarnessTest` verdes
- 2026-11-21 · IMP-AR-03 · `ProductImportService.preview` lê de uma vez, pela porta do comercial (`ProductLookup.findByAnyKey` → `ProductRepository.findBySkuIn`/`findByBarcodeIn`/`findByReferenceIn`, sem Repository alheio), os artigos de todo o sistema com os SKU, códigos de barras e referências do ficheiro; `ImportProductKeys` (classe pura): SKU existente → `JÁ EXISTE` (ignorado, a categoria não entra em "categorias a criar"); código de barras/referência de artigo com outro SKU → `ERRO` na coluna, mesmo com SKU existente; SKU, código de barras e referência repetidos no ficheiro → `ERRO` em ambas via `DuplicateKeys` (agora recebe a frase com género: "NUIT repetido", "Referência repetida"). SPEC §4.3 precisada (âmbito global, sem `ASSOCIADO` para artigos). `ProductImportServiceTest` verde (37/37), `ProductLookupTest` (16/16), `ClientImportServiceTest`, `SupplierImportServiceTest`, `CsvImportReaderTest`, `ModuleDecouplingHarnessTest` e `CleanArchitectureHarnessTest` verdes
- 2026-11-21 · IMP-09 · `GET /api/data-import/{entity}/template` (`clients`/`suppliers`/`products`) no `DataImportController` → `DataImportTemplateService` → `ImportTemplateDTO` (contracts): UTF-8 com BOM, `;`, só o cabeçalho com as colunas da SPEC §4 pela ordem do modelo (obrigatórias primeiro), `text/csv`, `attachment; filename="modelo-clientes.csv"` (`fornecedores`, `artigos`); as colunas vêm do enum `ImportEntity`, que reutiliza as listas dos serviços de importação (sem duplicar nomes); entidade desconhecida → `BusinessRuleException` PT-MZ (400). `/api/data-import` classificado na `ApiRolePolicy` e negado a quem não é ADMIN (a prova HTTP do 403 fica para IMP-10). `DataImportHttpIntegrationTest` verde (4/4), `ApiRolePolicyEndpointCoverageHarnessTest`, `ApiRolePolicyTest`, `ModuleDecouplingHarnessTest`, `CleanArchitectureHarnessTest` e serviços de importação verdes
- 2026-11-21 · IMP-06 · `POST /api/data-import/{entity}/preview` e `/commit` (corpo binário `application/octet-stream` ou `text/csv`) → `DataImportService`: pré-visualizar só classifica (`@Transactional(readOnly)`); confirmar refaz a classificação do zero (`plan()` de cada serviço de importação → `ImportPlan<T>` com o estado e o pedido do registo manual por linha), com qualquer `ERRO` recusa com `BusinessRuleException` PT-MZ ("… nada foi gravado. Primeiro erro: linha N, coluna …") e senão grava `NOVO`/`ASSOCIADO` numa só `@Transactional` pelos serviços donos (`ComercialService.createClient`/`createProduct`, `PurchaseService.createSupplier`, `ProductCategoryService.create` para as categorias novas, criadas primeiro com código `ImportCategoryPlan.codeFor` — regra acrescentada à SPEC §4.3); devolve `ImportCommitResultDTO` (created/associated/alreadyExisting). Testes: pré-visualizar não grava, confirmar grava e repetir dá `JÁ EXISTE`, fornecedores, linha com erro → 400 sem gravar, conflito surgido depois da pré-visualização → 400 sem gravar, categorias novas criadas uma vez, falha a meio da escrita (código de categoria em colisão) desfaz a categoria já criada. `DataImportHttpIntegrationTest` verde (11/11), serviços de importação, `ModuleDecouplingHarnessTest`, `CleanArchitectureHarnessTest`, `ApiRolePolicyEndpointCoverageHarnessTest` e `RalphGoalsHarnessTest` verdes
- 2026-11-21 · IMP-07 · prova HTTP com a Ana (ADMIN nas duas empresas do arranque): confirmar clientes, fornecedores e artigos com categoria nova na 2.ª empresa deixa-os só nela — a 1.ª não os vê nas listas de clientes, fornecedores, artigos e categorias; o mesmo ficheiro pré-visualizado na 1.ª empresa dá cliente `ASSOCIADO` (não `JÁ EXISTE`) e fornecedor `NOVO`, e confirmar associa o cliente sem o tirar da 2.ª. Sem alteração de código de produção (o `X-Company-Id` já chegava a todas as portas de leitura e serviços donos). `DataImportHttpIntegrationTest` verde (13/13)
- 2026-11-21 · IMP-08 · `POST /api/data-import/{entity}/commit` aceita `fileName` opcional; `DataImportService.commit` grava, na mesma transacção e pelo serviço dono (`AuditLogService.logCurrent`), um único `DATA_IMPORT` com o texto de `ImportAuditDetails` (classe pura): `Importação de clientes do ficheiro "x.csv": 1 novo, 0 associados, 1 já existente.` (sem nome → `(sem nome)`, cortado a 200 caracteres; `ImportEntity.label()`); confirmação recusada não deixa registo. Regra do transporte do nome precisada na SPEC §3.1 passo 4. O teste passou a escolher a empresa por id ordenado (o login devolve as empresas sem ordem garantida e o IMP-07 falhava de forma intermitente). `DataImportHttpIntegrationTest` verde (15/15), `ModuleDecouplingHarnessTest`, `CleanArchitectureHarnessTest` e `ApiRolePolicyEndpointCoverageHarnessTest` verdes
