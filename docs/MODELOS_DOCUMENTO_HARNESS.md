# Harness — Modelos de documento por empresa

> Cenários que provam [MODELOS_DOCUMENTO_SPEC.md](MODELOS_DOCUMENTO_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-26 (DT-01 a DT-06 e DT-UI-01 verdes)

| ID | Garantia | Teste |
|---|---|---|
| DT-01 | Cor `#RRGGBB` com contraste ≥ 4,5:1 com branco; inválida recusada; omissão do `PdfTheme` (§3.1) | `DocumentTemplateRulesTest` (verde: `DocumentAccentColorPolicy`, `PdfTheme.accentOrDefault`) |
| DT-02 | Posição do logótipo, notas (≤ 500), até 3 contas bancárias, QR; migração das novas colunas (§3.1) | `DocumentTemplateRulesTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` (verde: `DocumentTemplatePolicy`, V156) |
| DT-03 | Elementos fiscais obrigatórios presentes com todas as colunas escondidas, incluindo IVA por taxa (§3.2) | `DocumentTemplateFiscalElementsTest` (verde: `FiscalElements` em factura A4/térmica, NC, ND, POS e recibo) |
| DT-04 | PDFs A4 usam cor, logótipo, notas e bancos do modelo; térmicos só logótipo, notas e rodapé (§3.3) | `DocumentTemplateRenderingTest` (verde: `DocumentConfigService.getTemplate`, `DocumentTemplateBlocks` em factura/NC/ND/recibo A4, factura térmica e recibo POS) |
| DT-05 | 2.ª via de documento antigo usa o modelo actual sem mudar conteúdo fiscal (§3.3) | `DocumentTemplateRenderingTest` (verde: 2.ª via de factura/NC antigas com o modelo actual e texto fiscal idêntico entre modelos) |
| DT-06 | Alterar modelo só ADMIN com auditoria; pré-visualização gera PDF de exemplo sem gravar (§3.3, §3.4) | `DocumentTemplateHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` (verde: `DocumentConfigService.saveTemplate`/`validateTemplate`, `DocumentTemplatePreviewService`, `ApiRolePolicy` só ADMIN em `/api/documents/template`) |
| DT-UI-01 | Configurações › Documentos com cor, logótipo, notas, bancos, QR e pré-visualização (§3.4) | `DocumentTemplateUiHarnessTest` (verde: `ConfigDocumentTemplatePanel`/`ConfigDocumentTemplateOperations` no ecrã de documentos, `DocumentConfigApiClient` GET/PUT/preview) |
