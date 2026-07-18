# Exportação Fiscal de Vendas (SAF-T) — Especificação

> Fonte de verdade sobre a **exportação fiscal de documentos de venda** para auditoria/AT.
> Lê este ficheiro antes de mexer em `modules/fiscal/` na parte de exportação. Harness em
> [FISCAL_SAFT_EXPORT_HARNESS.md](FISCAL_SAFT_EXPORT_HARNESS.md).

**Última actualização:** 2026-07-01

---

## 1. Problema

O sistema já calcula o **apuramento de IVA mensal** (`FiscalSummaryService.computeMonth`) mas
**não exporta um ficheiro estruturado de auditoria** dos documentos de venda. Um auditor da
Autoridade Tributária (ou um contabilista) precisa de um ficheiro processável por máquina, por
período, com: cabeçalho da empresa, clientes, tabela de taxas e cada fatura com as suas linhas e
totais. Hoje isso só existe em PDF/écran.

---

## 2. Decisão e âmbito honesto

Cria-se um `FiscalSalesExportService` que produz um **ficheiro XML alinhado com a estrutura
SAF-T** (`Header` / `MasterFiles` / `SourceDocuments` → `SalesInvoices`).

> ⚠️ **Limite declarado:** este export segue a *estrutura* SAF-T mas **não é certificado**. Antes
> de submissão oficial à AT-MZ tem de ser **validado contra a XSD oficial** vigente (nomes de
> elementos, cardinalidades e regras de assinatura podem diferir). É a fundação dos dados, não um
> ficheiro certificado. Esta honestidade está aqui de propósito — não inventar conformidade.

O que entrega já hoje, real e testável:
- Um XML determinístico e bem-formado, com escaping XML correcto.
- Dados reais das faturas existentes (sem campos fabricados).
- Totais conferíveis (nº de documentos, base, IVA, total) que **batem** com a soma das linhas.

---

## 3. `FiscalSalesExportService.exportSales(companyId, from, to)`

- **Permissão:** `MANAGER/ADMIN` (`PermissionGuard.requireManagerOrAdmin`).
- **Multi-tenant:** `CurrentUserContext.requireCompany(companyId)`.
- **Período:** `[from, to]` por data inclusiva (sobre `Invoice.createdAt`).
- **Documentos incluídos:** faturas **emitidas** — `APPROVED`, `PAID`, `PARTIALLY_PAID` e
  `CANCELLED` (a anulação aparece com `InvoiceStatus` para o auditor ver). **Excluídas**: `DRAFT`,
  `PENDING_APPROVAL`, `PENDING_DISCOUNT_APPROVAL`, `REJECTED` (não são documentos fiscais emitidos).
- **Devolve** `FiscalSalesExportDTO(xml, numberOfInvoices, totalNet, totalTax, totalGross)`.

### 3.1 Estrutura do XML

```
<AuditFile>
  <Header>
    <CompanyName/> <TaxRegistrationNumber/> (NUIT)
    <FiscalYear/> <StartDate/> <EndDate/> <CurrencyCode>MZN</CurrencyCode>
    <DateCreated/> <ProductID>Multicore ERP</ProductID>
  </Header>
  <MasterFiles>
    <Customer>*      (clientes distintos do período: id, nome, NUIT)
    <TaxTableEntry>* (taxas distintas usadas, em %)
  </MasterFiles>
  <SourceDocuments>
    <SalesInvoices>
      <NumberOfEntries/> <TotalDebit>0</TotalDebit> <TotalCredit>{net}</TotalCredit>
      <Invoice>*
        <InvoiceNo/> <InvoiceDate/> <InvoiceStatus/> <CustomerID/>
        <Line>* ProductCode, Quantity, UnitPrice, TaxPercentage, CreditAmount
        <DocumentTotals> TaxPayable, NetTotal, GrossTotal </DocumentTotals>
      </Invoice>
    </SalesInvoices>
  </SourceDocuments>
</AuditFile>
```

### 3.2 Regras de valores
- Dinheiro a **2 casas decimais**; `CurrencyCode` fixo `MZN`.
- Totais do documento vêm de `Invoice.totalBeforeTax/taxAmount/totalAmount` (já calculados pela
  engine única — não recalcular aqui, para não divergir do POS/faturação).
- `TaxPercentage` da linha = `InvoiceLine.taxRate × 100`.
- `TotalCredit` = soma das bases líquidas das faturas **não anuladas**.

---

## 4. Regras / invariantes

1. **Sem dados fabricados.** Só sai o que existe nas entidades; campos ausentes saem vazios, não
   inventados.
2. **Bem-formado e escapado.** `& < > " '` escapados; o resultado faz parse por qualquer parser XML.
3. **Determinístico.** Mesma entrada → mesmo XML (ordenação estável por nº de fatura).
4. **Totais conferíveis.** `numberOfInvoices`/`totalNet`/`totalTax`/`totalGross` do DTO batem com o
   conteúdo do XML.
5. **Não recalcular impostos.** Reutiliza os valores persistidos da fatura.

---

## 5. Exposição

- **API:** `GET /api/fiscal/saft?companyId&from&to` → `application/xml` (corpo = XML).
- **UI:** botão "Exportar SAF-T (Vendas)" no `FiscalPanel` → escolhe período + grava ficheiro
  `saft_vendas_<de>_<ate>.xml` via `JFileChooser`.

---

## 6. Mapa de ficheiros

| Quero… | Ficheiro |
|--------|----------|
| Gerar a exportação | `modules/fiscal/service/FiscalSalesExportService.java` |
| Resultado (XML + totais) | `modules/fiscal/dto/FiscalSalesExportDTO.java` |
| Endpoint REST | `modules/fiscal/controller/FiscalController.java` |
| Botão na UI | `gui/FiscalPanel.java` |
| Cenários de validação | [FISCAL_SAFT_EXPORT_HARNESS.md](FISCAL_SAFT_EXPORT_HARNESS.md) |
| Apuramento IVA (relacionado) | `modules/fiscal/service/FiscalSummaryService.java` |
</content>
