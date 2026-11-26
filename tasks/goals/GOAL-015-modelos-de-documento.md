---
id: GOAL-015
title: "Facturas e recibos com a imagem da empresa sem esconder dados fiscais"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/MODELOS_DOCUMENTO_SPEC.md
harness: docs/MODELOS_DOCUMENTO_HARNESS.md
verify: ["DocumentTemplateRulesTest", "DocumentTemplateFiscalElementsTest", "DocumentTemplateRenderingTest", "DocumentTemplateHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "DocumentTemplateUiHarnessTest"]
blockedBy: []
---

# Objectivo

O ADMIN escolhe a cor, a posição do logótipo, notas e dados bancários, vê uma factura de exemplo e
grava; todos os documentos passam a sair assim, sempre com os elementos fiscais obrigatórios.
Regras em `docs/MODELOS_DOCUMENTO_SPEC.md`. Estende o `DocumentColumnConfig` existente.

## Critérios de aceitação

- [x] DT-01 — cor validada por contraste — `DocumentTemplateRulesTest`
- [x] DT-02 — logótipo, notas, contas bancárias e QR — `DocumentTemplateRulesTest`
- [x] DT-03 — elementos fiscais nunca escondidos — `DocumentTemplateFiscalElementsTest`
- [x] DT-04 — aplicação em A4 e térmico — `DocumentTemplateRenderingTest`
- [x] DT-05 — 2.ª vias com o modelo actual — `DocumentTemplateRenderingTest`
- [ ] DT-06 — ADMIN, auditoria e pré-visualização — `DocumentTemplateHttpIntegrationTest`
- [ ] DT-UI-01 — ecrã Configurações › Documentos — `DocumentTemplateUiHarnessTest`

## Fora de âmbito

- Editor visual livre, vários modelos por tipo, relatórios internos (SPEC §4).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-26 · DT-01 · `DocumentAccentColorPolicy` valida `#RRGGBB` com contraste WCAG ≥ 4,5:1 com branco (mensagens PT-MZ) e `PdfTheme.accentOrDefault` cai no `BRAND` sem cor própria; DocumentTemplateRulesTest 6/6 verde. Persistência da cor fica com o DT-02 (migração das novas colunas).
- 2026-11-26 · DT-02 · `DocumentTemplatePolicy` (logótipo LEFT/CENTER/RIGHT com omissão LEFT, notas ≤ 500, até 3 contas com banco/titular/NIB-IBAN obrigatórios, QR sim/não), `DocumentTemplateDTO`/`DocumentBankAccountDTO`/`DocumentLogoPosition` em contracts, colunas novas em `DocumentColumnConfig` + tabela `document_template_bank_accounts` (V156); DocumentTemplateRulesTest 12/12, MigrationPostgresDialectHarnessTest e PostgresSchemaParityHarnessTest (PG 18 real) verdes. Ligação ao `DocumentConfigService`/endpoint fica com o DT-06.
- 2026-11-26 · DT-03 · `FiscalElements` (adquirente ou "Consumidor Final", IVA por taxa com motivo de isenção) aplicado a NC e ND (`TotalsBlockRenderer.build` com discriminativo), venda POS, recibo RC (totais e IVA por taxa da factura) e factura térmica; com todas as colunas escondidas os elementos fiscais continuam; DocumentTemplateFiscalElementsTest 5/5 e testes de impressão verdes. Hash/certificação ainda não existem no modelo ("quando existirem").
- 2026-11-26 · DT-04 · `DocumentConfigService.getTemplate` devolve o modelo gravado (ou omissão) e `DocumentTemplateBlocks` aplica-o: factura, NC, ND e recibo A4 com cor de destaque nos títulos, logótipo LEFT/CENTER/RIGHT, notas e bloco de contas do modelo (factura cai nas contas de tesouraria sem contas no modelo); factura térmica e recibo POS só com logótipo, notas e rodapé; assinatura das notas passa a `SignatureBlockRenderer`; DocumentTemplateRenderingTest 10/10 e testes de impressão verdes.
- 2026-11-26 · DT-04 (gate) · diff-guard corrigido: sem `@Autowired` (renderers/serviço de recibo com um só construtor, `null` nos testes sem Spring) e sem parâmetros `float` novos (`DocumentTemplateBlocks.LogoSize` e larguras em `float[]`); diff-guard e gate verdes.
- 2026-11-26 · DT-05 · 2.ª via de factura e nota de crédito antigas (emitidas em 2024) sai com o modelo actual (notas, cor, logótipo, sem as contas do modelo anterior) e o conteúdo fiscal fica igual palavra a palavra ao mudar o modelo, sem alterar o documento; comportamento já garantido pela leitura do modelo no momento da impressão (DT-04), agora provado; DocumentTemplateRenderingTest 13/13 verde.
