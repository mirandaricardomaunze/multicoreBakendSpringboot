---
id: GOAL-006
title: "Importar clientes, fornecedores e artigos de outro ERP por CSV"
status: TODO
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

- [ ] IMP-01 — UTF-8 com/sem BOM e Windows-1252 com acentos intactos — `CsvImportReaderTest`
- [ ] IMP-02 — separador `;`/`,` detectado, aspas e `""` — `CsvImportReaderTest`
- [ ] IMP-03 — colunas por nome tolerante; obrigatória em falta recusa; extra anunciada — `CsvImportReaderTest`
- [ ] IMP-05 — limites 2 MB / 5 000 linhas; linhas vazias ignoradas — `CsvImportReaderTest`
- [ ] IMP-04 — números e datas nos formatos aceites; outro formato é erro com nº de linha — `ImportValueParserTest`
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

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
