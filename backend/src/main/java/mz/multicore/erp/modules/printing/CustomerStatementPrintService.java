package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementLineDTO;
import mz.multicore.erp.modules.comercial.service.CustomerStatementService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de geração de Extrato Canónico de Conta Corrente e Reconciliação de Clientes em PDF A4.
 */
@Service
public class CustomerStatementPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CustomerStatementService statementService;
    private final CompanyService companyService;

    public CustomerStatementPrintService(
            CustomerStatementService statementService,
            CompanyService companyService
    ) {
        this.statementService = statementService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long clientId, LocalDate startDate, LocalDate endDate) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        CustomerStatementDTO statement = statementService.getStatement(clientId, startDate, endDate);

        return PdfDocumentBuilder.buildA4(doc -> {
            // 1. Cabeçalho Institucional da Empresa
            String docNumber = "EXT/CLI-" + statement.clientId() + "/" + statement.startDate().getYear();
            doc.add(CompanyHeaderRenderer.build(company, "Extrato de Conta Corrente", docNumber));

            // 2. Bloco de Identificação do Cliente e Período
            doc.add(buildClientAndPeriodBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // 3. Cartões Métricos de Resumo de Saldos
            doc.add(buildSummaryBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(10f));

            // 4. Tabela de Transações com Saldo Progressivo
            doc.add(buildTransactionsTable(statement));
            doc.add(PdfDocumentBuilder.spacer(12f));

            // 5. Termo de Circularização e Reconciliação Formal
            doc.add(buildReconciliationNoticeBlock(statement, company));
            doc.add(PdfDocumentBuilder.spacer(15f));

            // 6. Bloco de Assinaturas e Carimbos
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

    private PdfPTable buildSummaryBlock(CustomerStatementDTO stmt) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}

        addSummaryCard(table, "Saldo Anterior", stmt.openingBalance(), PdfTheme.TEXT);
        addSummaryCard(table, "Total Facturado (+)", stmt.totalDebits(), PdfTheme.BRAND);
        addSummaryCard(table, "Total Liquidado (-)", stmt.totalCredits(), new Color(16, 185, 129));

        Color closingColor = stmt.closingBalance().signum() > 0 ? PdfTheme.DANGER : new Color(16, 185, 129);
        addSummaryCard(table, "Saldo em Aberto", stmt.closingBalance(), closingColor);

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

        // Linha de Saldo Inicial / Anterior
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

    private PdfPTable buildReconciliationNoticeBlock(CustomerStatementDTO stmt, Company company) {
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
