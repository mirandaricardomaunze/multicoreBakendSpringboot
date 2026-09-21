package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemDTO;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import mz.multicore.erp.modules.financeira.service.BankReconciliationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço de geração de Folha Oficial de Reconciliação Bancária em PDF A4.
 */
@Service
public class BankReconciliationPrintService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final java.awt.Color COLOR_GREEN = new java.awt.Color(16, 185, 129);
    private static final java.awt.Color COLOR_RED = new java.awt.Color(239, 68, 68);
    private static final java.awt.Color COLOR_YELLOW = new java.awt.Color(245, 158, 11);

    private final BankReconciliationService reconciliationService;
    private final CompanyService companyService;

    public BankReconciliationPrintService(
            BankReconciliationService reconciliationService,
            CompanyService companyService
    ) {
        this.reconciliationService = reconciliationService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long statementId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        BankStatementDTO statement = reconciliationService.getStatement(statementId);
        List<BankStatementItemDTO> items = reconciliationService.getStatementItems(statementId);

        return PdfDocumentBuilder.buildA4(doc -> {
            // Cabeçalho institucional
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Mapa de Reconciliação Bancária",
                    "REC-" + statement.statementReference()
            ));

            // Bloco de Identificação
            doc.add(buildMetaBlock(statement));
            doc.add(PdfDocumentBuilder.spacer(6f));

            // Tabela de Balanço e Conciliação (Saldos)
            doc.add(buildBalancesTable(statement, items));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // Tabela de Movimentos do Extracto
            doc.add(buildItemsTable(items));
            doc.add(PdfDocumentBuilder.spacer(18f));

            // Bloco de Assinaturas e Carimbo
            doc.add(buildSignaturesBlock());
        });
    }

    private PdfPTable buildMetaBlock(BankStatementDTO st) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{55f, 45f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Conta Bancária:", PdfTheme.boldFont()));
        left.addElement(new Paragraph(st.accountName(), PdfTheme.subtitleFont()));
        left.addElement(new Paragraph("Período: " + st.startDate().format(DAY_FMT) + " a " + st.endDate().format(DAY_FMT), PdfTheme.bodyFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("Referência Extracto: " + st.statementReference(), PdfTheme.boldFont()));
        right.addElement(new Paragraph("Estado: " + st.status().label(), PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Data de Emissão: " + LocalDate.now().format(DAY_FMT), PdfTheme.bodyFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildBalancesTable(BankStatementDTO st, List<BankStatementItemDTO> items) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}

        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        for (BankStatementItemDTO it : items) {
            if (it.amount().signum() > 0) totalCredits = totalCredits.add(it.amount());
            else totalDebits = totalDebits.add(it.amount().abs());
        }

        addKpiBox(table, "Saldo Inicial Extracto", formatMoney(st.openingBalance()) + " MT", PdfTheme.TEXT);
        addKpiBox(table, "Total Entradas (Créditos)", "+" + formatMoney(totalCredits) + " MT", COLOR_GREEN);
        addKpiBox(table, "Total Saídas (Débitos)", "-" + formatMoney(totalDebits) + " MT", COLOR_RED);
        addKpiBox(table, "Saldo Final Extracto", formatMoney(st.closingBalance()) + " MT", PdfTheme.BRAND);

        return table;
    }

    private void addKpiBox(PdfPTable table, String title, String value, java.awt.Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph t = new Paragraph(title, PdfTheme.smallFont());
        t.setAlignment(Element.ALIGN_CENTER);
        Paragraph v = new Paragraph(value, PdfTheme.boldFont());
        v.setAlignment(Element.ALIGN_CENTER);
        v.getFont().setColor(color);

        cell.addElement(t);
        cell.addElement(v);
        table.addCell(cell);
    }

    private PdfPTable buildItemsTable(List<BankStatementItemDTO> items) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{14f, 32f, 16f, 15f, 13f, 10f}); } catch (Exception ignored) {}

        addHeaderCell(table, "Data");
        addHeaderCell(table, "Descrição do Movimento");
        addHeaderCell(table, "Referência");
        addHeaderCell(table, "Valor (MT)");
        addHeaderCell(table, "Saldo Após");
        addHeaderCell(table, "Estado");

        for (BankStatementItemDTO it : items) {
            addDataCell(table, it.transactionDate().format(DAY_FMT), Element.ALIGN_CENTER);
            addDataCell(table, it.description(), Element.ALIGN_LEFT);
            addDataCell(table, it.reference() != null ? it.reference() : "-", Element.ALIGN_LEFT);

            PdfPCell amountCell = new PdfPCell(new Phrase(formatMoney(it.amount()) + " MT", PdfTheme.bodyFont()));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amountCell.setBorderColor(PdfTheme.BORDER);
            amountCell.setPadding(4f);
            if (it.amount().signum() < 0) {
                amountCell.getPhrase().getFont().setColor(COLOR_RED);
            } else {
                amountCell.getPhrase().getFont().setColor(COLOR_GREEN);
            }
            table.addCell(amountCell);

            addDataCell(table, it.balanceAfter() != null ? formatMoney(it.balanceAfter()) + " MT" : "-", Element.ALIGN_RIGHT);

            PdfPCell statusCell = new PdfPCell(new Phrase(it.status() == BankStatementItemStatus.MATCHED ? "OK" : "Pendente", PdfTheme.boldFont()));
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setBorderColor(PdfTheme.BORDER);
            statusCell.setPadding(4f);
            if (it.status() == BankStatementItemStatus.MATCHED) {
                statusCell.getPhrase().getFont().setColor(COLOR_GREEN);
            } else {
                statusCell.getPhrase().getFont().setColor(COLOR_YELLOW);
            }
            table.addCell(statusCell);
        }

        return table;
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_CENTER);
        left.addElement(new Paragraph("____________________________________", PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Responsável de Tesouraria / Contabilidade", PdfTheme.boldFont()));
        left.addElement(new Paragraph("Multicore Financial Management", PdfTheme.smallFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        right.addElement(new Paragraph("____________________________________", PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Direcção Geral / Auditoria Financeira", PdfTheme.boldFont()));
        right.addElement(new Paragraph("Carimbo / Visto de Conformidade", PdfTheme.smallFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static String formatMoney(BigDecimal val) {
        BigDecimal safe = val == null ? BigDecimal.ZERO : val;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
