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
- [ ] DT-02 — logótipo, notas, contas bancárias e QR — `DocumentTemplateRulesTest`
- [ ] DT-03 — elementos fiscais nunca escondidos — `DocumentTemplateFiscalElementsTest`
- [ ] DT-04 — aplicação em A4 e térmico — `DocumentTemplateRenderingTest`
- [ ] DT-05 — 2.ª vias com o modelo actual — `DocumentTemplateRenderingTest`
- [ ] DT-06 — ADMIN, auditoria e pré-visualização — `DocumentTemplateHttpIntegrationTest`
- [ ] DT-UI-01 — ecrã Configurações › Documentos — `DocumentTemplateUiHarnessTest`

## Fora de âmbito

- Editor visual livre, vários modelos por tipo, relatórios internos (SPEC §4).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-26 · DT-01 · `DocumentAccentColorPolicy` valida `#RRGGBB` com contraste WCAG ≥ 4,5:1 com branco (mensagens PT-MZ) e `PdfTheme.accentOrDefault` cai no `BRAND` sem cor própria; DocumentTemplateRulesTest 6/6 verde. Persistência da cor fica com o DT-02 (migração das novas colunas).
