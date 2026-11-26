# Harness — Modelos de documento por empresa

> Cenários que provam [MODELOS_DOCUMENTO_SPEC.md](MODELOS_DOCUMENTO_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-26 (DT-01, DT-02 verdes)

| ID | Garantia | Teste |
|---|---|---|
| DT-01 | Cor `#RRGGBB` com contraste ≥ 4,5:1 com branco; inválida recusada; omissão do `PdfTheme` (§3.1) | `DocumentTemplateRulesTest` (verde: `DocumentAccentColorPolicy`, `PdfTheme.accentOrDefault`) |
| DT-02 | Posição do logótipo, notas (≤ 500), até 3 contas bancárias, QR; migração das novas colunas (§3.1) | `DocumentTemplateRulesTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` (verde: `DocumentTemplatePolicy`, V156) |
| DT-03 | Elementos fiscais obrigatórios presentes com todas as colunas escondidas, incluindo IVA por taxa (§3.2) | `DocumentTemplateFiscalElementsTest` |
| DT-04 | PDFs A4 usam cor, logótipo, notas e bancos do modelo; térmicos só logótipo, notas e rodapé (§3.3) | `DocumentTemplateRenderingTest` |
| DT-05 | 2.ª via de documento antigo usa o modelo actual sem mudar conteúdo fiscal (§3.3) | `DocumentTemplateRenderingTest` |
| DT-06 | Alterar modelo só ADMIN com auditoria; pré-visualização gera PDF de exemplo sem gravar (§3.3, §3.4) | `DocumentTemplateHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| DT-UI-01 | Configurações › Documentos com cor, logótipo, notas, bancos, QR e pré-visualização (§3.4) | `DocumentTemplateUiHarnessTest` |
