# Exportação Fiscal de Vendas (SAF-T) — Harness

> Cenários de validação. Spec em [FISCAL_SAFT_EXPORT_SPEC.md](FISCAL_SAFT_EXPORT_SPEC.md).
> SF-01..SF-1x são **automáticos** (`FiscalSalesExportServiceTest`); SF-50+ são **manuais**.

**Última actualização:** 2026-07-01

---

## Automáticos (`mvn test`)

| Id | Cenário | Espera-se |
|----|---------|-----------|
| SF-01 | Export sem faturas no período | XML bem-formado, `NumberOfEntries=0`, totais a 0 |
| SF-02 | Export com 2 faturas APPROVED | `numberOfInvoices=2`; XML contém os 2 `InvoiceNo` |
| SF-03 | Fatura fora do período (data anterior a `from`) | Não aparece no XML |
| SF-04 | Fatura `DRAFT`/`PENDING_APPROVAL` no período | **Excluída** (não é documento emitido) |
| SF-05 | Fatura `CANCELLED` no período | **Incluída** com `<InvoiceStatus>CANCELLED</InvoiceStatus>` |
| SF-06 | `CANCELLED` não conta para `TotalCredit` | `totalNet` ignora a anulada |
| SF-07 | Header | Contém `CompanyName`, `TaxRegistrationNumber` (NUIT), `CurrencyCode>MZN` |
| SF-08 | Cliente com `&`/`<` no nome | Escapado (`&amp;`/`&lt;`); XML continua a fazer parse |
| SF-09 | `MasterFiles` | Lista clientes distintos do período e taxas distintas usadas |
| SF-10 | Totais conferíveis | `totalNet+totalTax == totalGross`; batem com soma das linhas |
| SF-11 | XML faz parse | `DocumentBuilder.parse(xml)` sem excepção |
| SF-12 | Sem permissão (EMPLOYEE) | `BusinessRuleException` |
| SF-13 | Empresa diferente da activa | `BusinessRuleException` (guarda multi-tenant) |
| SF-14 | Determinismo | Duas chamadas com a mesma entrada → XML idêntico |

---

## Manuais

| Id | Passo | Espera-se |
|----|-------|-----------|
| SF-50 | UI: `FiscalPanel` → "Exportar SAF-T (Vendas)" → período do mês → gravar | Ficheiro `.xml` criado; abre em editor/navegador sem erros |
| SF-51 | Validar contra a **XSD oficial da AT-MZ** vigente | Lista de divergências de nomes/cardinalidade a fechar antes de submissão certificada |
| SF-52 | Conferir totais do XML com o **Apuramento IVA** do mesmo mês (`/api/fiscal/iva-summary`) | Base e IVA das vendas coincidem |

---

## Critério de aceitação

- SF-01..SF-14 verdes em `mvn test`.
- SF-50/SF-52 executados uma vez com dados reais.
- SF-51 documentado: a **certificação** contra a XSD oficial é trabalho explícito e separado; este
  harness garante a estrutura e a integridade dos dados, não a conformidade legal certificada.
</content>
