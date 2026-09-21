package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;
import mz.multicore.erp.modules.purchases.service.SupplierStatementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de geração de Extrato Canónico de Conta Corrente e Reconciliação de Fornecedores em PDF A4.
 */
@Service
public class SupplierStatementPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SupplierStatementService statementService;
    private final CompanyService companyService;

    public SupplierStatementPrintService(
            SupplierStatementService statementService,
            CompanyService companyService
    ) {
        this.statementService = statementService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long supplierId, LocalDate startDate, LocalDate endDate) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        SupplierStatementDTO statement = statementService.getStatement(supplierId, startDate, endDate);

        return PdfDocumentBuilder.buildA4(doc -> {
            String docNumber = "EXT/FORN-" + statement.supplierId() + "/" + statement.startDate().getYear();
            doc.add(CompanyHeaderRenderer.build(company, "Extrato de Conta Corrente — Fornecedor", docNumber));

            doc.add(buildSupplierAndPeriodBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(8f));

            doc.add(buildSummaryBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(10f));

            doc.add(buildTransactionsTable(statement));
            doc.add(PdfDocumentBuilder.spacer(12f));

            doc.add(buildReconciliationNoticeBlock(statement, company));
            doc.add(PdfDocumentBuilder.spacer(15f));

            doc.add(buildSignaturesBlock(company, statement));
        });
    }

    private PdfPTable buildSupplierAndPeriodBlock(SupplierStatementDTO stmt) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.BOX);
        left.setBorderColor(PdfTheme.BORDER);
        left.setPadding(8f);
        left.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        left.addElement(new Paragraph("FORNECEDOR:", PdfTheme.boldFont()));
        left.addElement(new Paragraph(stmt.supplierName(), PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("NUIT/NIF: " + (stmt.supplierTaxId() != null ? stmt.supplierTaxId() : "N/D"), PdfTheme.bodyFont()));
        if (stmt.address() != null && !stmt.address().isBlank()) {
            left.addElement(new Paragraph("Endereço: " + stmt.address(), PdfTheme.smallFont()));
        }
        if (stmt.phone() != null && !stmt.phone().isBlank()) {
            left.addElement(new Paragraph("Contacto: " + stmt.phone() + (stmt.email() != null ? " | " + stmt.email() : ""), PdfTheme.smallFont()));
        }

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.BOX);
        right.setBorderColor(PdfTheme.BORDER);
        right.setPadding(8f);
        right.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        right.addElement(new Paragraph("PARÂMETROS DO EXTRATO:", PdfTheme.boldFont()));
        right.addElement(new Paragraph("Período: " + stmt.startDate().format(DATE_FMT) + " a " + stmt.endDate().format(DATE_FMT), PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Data de Emissão: " + LocalDate.now().format(DATE_FMT), PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Moeda: " + PdfTheme.CURRENCY_CODE + " (Meticais)", PdfTheme.bodyFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildSummaryBlock(SupplierStatementDTO stmt) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}

        addSummaryCard(table, "Saldo Anterior a Pagar", stmt.openingBalance(), PdfTheme.TEXT);
        addSummaryCard(table, "Total Compras (+)", stmt.totalCredits(), PdfTheme.BRAND);
        addSummaryCard(table, "Total Pagamentos (-)", stmt.totalDebits(), new Color(16, 185, 129));

        Color closingColor = stmt.closingBalance().signum() > 0 ? PdfTheme.DANGER : new Color(16, 185, 129);
        addSummaryCard(table, "Saldo em Dívida", stmt.closingBalance(), closingColor);

        return table;
    }

    private void addSummaryCard(PdfPTable table, String label, BigDecimal amount, Color valColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pLabel = new Paragraph(label, PdfTheme.smallFont());
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(MoneyFormat.format(amount), PdfTheme.boldFont());
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private PdfPTable buildTransactionsTable(SupplierStatementDTO stmt) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{12f, 8f, 15f, 25f, 13f, 13f, 14f}); } catch (Exception ignored) {}

        addHeaderCell(table, "Data");
        addHeaderCell(table, "Tipo");
        addHeaderCell(table, "Documento");
        addHeaderCell(table, "Descrição");
        addHeaderCell(table, "Pagamentos (-)");
        addHeaderCell(table, "Compras (+)");
        addHeaderCell(table, "Saldo a Pagar");

        addZebraCell(table, stmt.startDate().format(DATE_FMT), false, Element.ALIGN_CENTER);
        addZebraCell(table, "TRANS", false, Element.ALIGN_CENTER);
        addZebraCell(table, "SALDO-ANT", false, Element.ALIGN_LEFT);
        addZebraCell(table, "Saldo Anterior a Pagar", false, Element.ALIGN_LEFT);
        addZebraCell(table, "-", false, Element.ALIGN_RIGHT);
        addZebraCell(table, "-", false, Element.ALIGN_RIGHT);
        addZebraCell(table, MoneyFormat.format(stmt.openingBalance()), false, Element.ALIGN_RIGHT);

        boolean zebra = true;
        for (SupplierStatementLineDTO line : stmt.lines()) {
            addZebraCell(table, line.date().format(DATE_FMT), zebra, Element.ALIGN_CENTER);
            addZebraCell(table, line.documentType(), zebra, Element.ALIGN_CENTER);
            addZebraCell(table, line.documentNumber(), zebra, Element.ALIGN_LEFT);
            addZebraCell(table, line.description(), zebra, Element.ALIGN_LEFT);
            addZebraCell(table, line.debit().signum() > 0 ? MoneyFormat.formatPlain(line.debit()) : "-", zebra, Element.ALIGN_RIGHT);
            addZebraCell(table, line.credit().signum() > 0 ? MoneyFormat.formatPlain(line.credit()) : "-", zebra, Element.ALIGN_RIGHT);
            addZebraCell(table, MoneyFormat.format(line.runningBalance()), zebra, Element.ALIGN_RIGHT);
            zebra = !zebra;
        }

        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addZebraCell(PdfPTable table, String text, boolean zebra, int align) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, PdfTheme.bodyFont()));
        cell.setBackgroundColor(zebra ? PdfTheme.TOTAL_ROW_BG : Color.WHITE);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private PdfPTable buildReconciliationNoticeBlock(SupplierStatementDTO stmt, Company company) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setBackgroundColor(new Color(248, 250, 252));

        cell.addElement(new Paragraph("TERMO DE CIRCULARIZAÇÃO E CONFERÊNCIA DE CONTAS A PAGAR:", PdfTheme.boldFont()));
        String text = "Para efeitos de auditoria externa e reconciliação dos saldos de fornecedores, confirmamos que "
                + "o montante pendente de liquidação a favor de " + stmt.supplierName() + " ascende a "
                + MoneyFormat.format(stmt.closingBalance()) + " na data de fecho " + stmt.endDate().format(DATE_FMT) + ".";
        cell.addElement(new Paragraph(text, PdfTheme.smallFont()));

        table.addCell(cell);
        return table;
    }

    private PdfPTable buildSignaturesBlock(Company company, SupplierStatementDTO stmt) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_CENTER);
        left.addElement(new Paragraph("Pela Empresa Compradora:", PdfTheme.boldFont()));
        left.addElement(new Paragraph(company.getName(), PdfTheme.smallFont()));
        left.addElement(new Paragraph("\n_________________________________________\nCarimbo & Assinatura", PdfTheme.smallFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        right.addElement(new Paragraph("Pelo Fornecedor:", PdfTheme.boldFont()));
        right.addElement(new Paragraph(stmt.supplierName(), PdfTheme.smallFont()));
        right.addElement(new Paragraph("\n_________________________________________\nData: ____/____/20___  Carimbo & Assinatura", PdfTheme.smallFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }
}
