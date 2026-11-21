---
id: GOAL-006
title: "Importar clientes, fornecedores e artigos de outro ERP por CSV"
status: IN_PROGRESS
priority: HIGH
spec: docs/IMPORTACAO_DADOS_SPEC.md
harness: docs/IMPORTACAO_DADOS_HARNESS.md
verify: ["CsvImportReaderTest", "ImportValueParserTest", "ClientImportServiceTest", "ClientOptionalEmailTest", "SupplierImportServiceTest", "ProductImportServiceTest", "DataImportHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "ModuleDecouplingHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "DataImportPanelHarnessTest"]
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
- [ ] IMP-CL-03 — email do cliente opcional (migração, request, registo manual) — `ClientOptionalEmailTest`
- [ ] IMP-CL-01 — cliente: NUIT obrigatório e válido, email opcional — `ClientImportServiceTest`
- [ ] IMP-CL-02 — cliente: `JÁ EXISTE`, `ASSOCIADO` e repetido no ficheiro — `ClientImportServiceTest`
- [ ] IMP-FO-01 — fornecedor: NUIT, `JÁ EXISTE` e repetido no ficheiro — `SupplierImportServiceTest`
- [ ] IMP-AR-01 — artigo: IVA existente por percentagem ou código — `ProductImportServiceTest`
- [ ] IMP-AR-02 — artigo: categoria nova criada só na confirmação — `ProductImportServiceTest`
- [ ] IMP-AR-03 — artigo: SKU existente ignorado; código de barras/referência alheios são erro — `ProductImportServiceTest`
- [ ] IMP-09 — modelo CSV por entidade (UTF-8 BOM, `;`, só cabeçalho) — `DataImportHttpIntegrationTest`
- [ ] IMP-06 — pré-visualizar não grava; confirmar revalida, tudo ou nada, uma transacção — `DataImportHttpIntegrationTest`
- [ ] IMP-07 — só a empresa activa — `DataImportHttpIntegrationTest`
- [ ] IMP-08 — auditoria `DATA_IMPORT` por confirmação — `DataImportHttpIntegrationTest`
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
