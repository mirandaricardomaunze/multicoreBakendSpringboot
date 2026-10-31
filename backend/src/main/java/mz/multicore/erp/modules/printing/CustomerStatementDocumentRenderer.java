package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.company.model.Company;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Renderizador A4 do Extrato Canónico de Conta Corrente e Reconciliação de Clientes. */
@Component
public class CustomerStatementDocumentRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] render(Company company, CustomerStatementDTO statement) {
        return PdfDocumentBuilder.buildA4(doc -> {
            String docNumber = "EXT/CLI-" + statement.clientId() + "/" + statement.startDate().getYear();
            doc.add(CompanyHeaderRenderer.build(company, "Extrato de Conta Corrente", docNumber));
            doc.add(buildClientAndPeriodBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(buildSummaryBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(buildTransactionsTable(statement));
            doc.add(PdfDocumentBuilder.spacer(12f));
            doc.add(buildReconciliationNoticeBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(15f));
            doc.add(buildSignaturesBlock(company, statement));
        });
    }

    private PdfPTable buildClientAndPeriodBlock(CustomerStatementDTO stmt) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.BOX);
        left.setBorderColor(PdfTheme.BORDER);
        left.setPadding(8f);
        left.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        left.addElement(new Paragraph("CLIENTE / DESTINATÁRIO:", PdfTheme.boldFont()));
        left.addElement(new Paragraph(stmt.clientName(), PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("NUIT: " + (stmt.clientTaxId() != null ? stmt.clientTaxId() : "N/D"), PdfTheme.bodyFont()));
        if (stmt.address() != null && !stmt.address().isBlank()) {
            left.addElement(new Paragraph("Endereço: " + stmt.address(), PdfTheme.smallFont()));
        }
        StringBuilder contact = new StringBuilder();
        if (stmt.phone() != null && !stmt.phone().isBlank()) {
            contact.append("Tel: ").append(stmt.phone());
        }
        if (stmt.email() != null && !stmt.email().isBlank()) {
            if (contact.length() > 0) contact.append(" | ");
            contact.append("Email: ").append(stmt.email());
        }
        if (contact.length() > 0) {
            left.addElement(new Paragraph("Contacto: " + contact, PdfTheme.smallFont()));
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

    private PdfPTable buildSummaryBlock(CustomerStatementDTO stmt) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}

        addSummaryCard(table, "Saldo Anterior", stmt.openingBalance());
        addSummaryCard(table, "Total Facturado (+)", stmt.totalDebits());
        addSummaryCard(table, "Total Liquidado (-)", stmt.totalCredits());
        addSummaryCard(table, "Saldo em Aberto", stmt.closingBalance());
        return table;
    }

    private void addSummaryCard(PdfPTable table, String label, BigDecimal amount) {
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

    private PdfPTable buildTransactionsTable(CustomerStatementDTO stmt) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{12f, 8f, 15f, 25f, 13f, 13f, 14f}); } catch (Exception ignored) {}

        addHeaderCell(table, "Data");
        addHeaderCell(table, "Tipo");
        addHeaderCell(table, "Documento");
        addHeaderCell(table, "Descrição");
        addHeaderCell(table, "Débito (+)");
        addHeaderCell(table, "Crédito (-)");
        addHeaderCell(table, "Saldo Acum.");

        addZebraCell(table, stmt.startDate().format(DATE_FMT), false, Element.ALIGN_CENTER);
        addZebraCell(table, "TRANS", false, Element.ALIGN_CENTER);
        addZebraCell(table, "SALDO-ANT", false, Element.ALIGN_LEFT);
        addZebraCell(table, "Saldo Anterior Transmitido", false, Element.ALIGN_LEFT);
        addZebraCell(table, "-", false, Element.ALIGN_RIGHT);
        addZebraCell(table, "-", false, Element.ALIGN_RIGHT);
        addZebraCell(table, MoneyFormat.format(stmt.openingBalance()), false, Element.ALIGN_RIGHT);

        boolean zebra = true;
        for (CustomerStatementLineDTO line : stmt.lines()) {
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

    private PdfPTable buildReconciliationNoticeBlock(CustomerStatementDTO stmt) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setBackgroundColor(new Color(248, 250, 252));

        cell.addElement(new Paragraph("TERMO DE CIRCULARIZAÇÃO E RECONCILIAÇÃO DE SALDOS:", PdfTheme.boldFont()));
        String text = "Para efeitos de auditoria, fecho contabilístico e conferência de conta corrente, solicitamos a "
                + "vossa verificação do saldo acima demonstrado de " + MoneyFormat.format(stmt.closingBalance())
                + " à data de " + stmt.endDate().format(DATE_FMT) + ". Caso encontrem qualquer discrepância com os vossos "
                + "registos de tesouraria, solicitamos a devolução deste termo devidamente anotado no prazo de 15 dias.";
        cell.addElement(new Paragraph(text, PdfTheme.smallFont()));

        table.addCell(cell);
        return table;
    }

    private PdfPTable buildSignaturesBlock(Company company, CustomerStatementDTO stmt) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_CENTER);
        left.addElement(new Paragraph("Pela Empresa Emitente:", PdfTheme.boldFont()));
        left.addElement(new Paragraph(company.getName(), PdfTheme.smallFont()));
        left.addElement(new Paragraph("\n_________________________________________\nCarimbo & Assinatura", PdfTheme.smallFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        right.addElement(new Paragraph("Pelo Cliente (Conferido & Conforme):", PdfTheme.boldFont()));
        right.addElement(new Paragraph(stmt.clientName(), PdfTheme.smallFont()));
        right.addElement(new Paragraph("\n_________________________________________\nData: ____/____/20___  Carimbo & Assinatura", PdfTheme.smallFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }
}
