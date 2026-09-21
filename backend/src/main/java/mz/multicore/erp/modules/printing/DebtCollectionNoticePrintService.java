package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.dto.OverdueInvoiceItemDTO;
import mz.multicore.erp.modules.comercial.service.CreditRiskService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Serviço de geração de Notificação Formal de Cobrança e Regularização de Dívida em PDF A4.
 */
@Service
public class DebtCollectionNoticePrintService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CreditRiskService creditRiskService;
    private final CompanyService companyService;

    public DebtCollectionNoticePrintService(
            CreditRiskService creditRiskService,
            CompanyService companyService
    ) {
        this.creditRiskService = creditRiskService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, Long clientId, LocalDate referenceDate) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        DebtCollectionNoticeDTO notice = creditRiskService.getDebtCollectionNotice(clientId, referenceDate);

        return PdfDocumentBuilder.buildA4(doc -> {
            // Cabeçalho da Empresa
            doc.add(CompanyHeaderRenderer.build(
                    company,
                    "Carta de Notificação de Cobrança",
                    notice.noticeReference()
            ));

            // Destinatário
            doc.add(buildRecipientBlock(notice));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // Corpo da Carta
            doc.add(buildLetterIntroduction(notice));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // Tabela de Facturas em Mora
            doc.add(buildInvoicesTable(notice));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // Bloco de Dados Bancários
            doc.add(buildBankAccountsBlock(notice));
            doc.add(PdfDocumentBuilder.spacer(10f));

            // Termos e Aviso
            doc.add(buildNoticeConclusion(notice));
            doc.add(PdfDocumentBuilder.spacer(20f));

            // Bloco de Assinatura
            doc.add(buildSignaturesBlock());
        });
    }

    private PdfPTable buildRecipientBlock(DebtCollectionNoticeDTO notice) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);

        cell.addElement(new Paragraph("DESTINATÁRIO / EXMO.(S) SENHOR(ES):", PdfTheme.boldFont()));
        cell.addElement(new Paragraph(notice.clientName(), PdfTheme.subtitleFont()));
        cell.addElement(new Paragraph("NUIT: " + (notice.clientTaxId() != null ? notice.clientTaxId() : "N/D"), PdfTheme.bodyFont()));
        if (notice.address() != null && !notice.address().isBlank()) {
            cell.addElement(new Paragraph("Endereço: " + notice.address(), PdfTheme.bodyFont()));
        }
        if (notice.email() != null && !notice.email().isBlank()) {
            cell.addElement(new Paragraph("Email: " + notice.email(), PdfTheme.bodyFont()));
        }

        table.addCell(cell);
        return table;
    }

    private Paragraph buildLetterIntroduction(DebtCollectionNoticeDTO notice) {
        String dateStr = notice.referenceDate() != null ? notice.referenceDate().format(DAY_FMT) : LocalDate.now().format(DAY_FMT);
        Paragraph p = new Paragraph();
        p.setFont(PdfTheme.bodyFont());
        p.setLeading(14f);
        p.add(String.format(
                "Maputo, %s.\n\n"
                        + "Assunto: Notificação de Regularização de Contas e Saldo Devedor em Atraso.\n\n"
                        + "Vimos por este meio solicitar a vossa melhor atenção para a existência de valores pendentes de liquidação "
                        + "junto da nossa tesouraria, referentes a fornecimentos de bens e/ou serviços prestados. À presente data, "
                        + "o vosso saldo em atraso atinge o montante total de %s MT, com atraso máximo acumulado de %d dias.",
                dateStr,
                formatMoney(notice.totalOverdue()),
                notice.maxDaysOverdue()
        ));
        return p;
    }

    private PdfPTable buildInvoicesTable(DebtCollectionNoticeDTO notice) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{18f, 13f, 13f, 10f, 15f, 15f, 16f});
        } catch (Exception ignored) {}

        addHeaderCell(table, "Factura N.º");
        addHeaderCell(table, "Emissão");
        addHeaderCell(table, "Vencimento");
        addHeaderCell(table, "Atraso");
        addHeaderCell(table, "Total (MT)");
        addHeaderCell(table, "Pago (MT)");
        addHeaderCell(table, "Saldo em Mora");

        for (OverdueInvoiceItemDTO item : notice.overdueInvoices()) {
            addDataCell(table, item.invoiceNumber(), Element.ALIGN_LEFT);
            addDataCell(table, item.issueDate() != null ? item.issueDate().format(DAY_FMT) : "-", Element.ALIGN_CENTER);
            addDataCell(table, item.dueDate() != null ? item.dueDate().format(DAY_FMT) : "-", Element.ALIGN_CENTER);
            addDataCell(table, item.daysOverdue() + " dias", Element.ALIGN_CENTER);
            addDataCell(table, formatMoney(item.totalAmount()), Element.ALIGN_RIGHT);
            addDataCell(table, formatMoney(item.paidAmount()), Element.ALIGN_RIGHT);

            PdfPCell saldoCell = new PdfPCell(new Phrase(formatMoney(item.outstandingAmount()) + " MT", PdfTheme.boldFont()));
            saldoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            saldoCell.setPadding(5f);
            saldoCell.setBorderColor(PdfTheme.BORDER);
            table.addCell(saldoCell);
        }

        // Linha de Total
        PdfPCell labelTotal = new PdfPCell(new Phrase("TOTAL GERAL EM MORA:", PdfTheme.boldFont()));
        labelTotal.setColspan(6);
        labelTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelTotal.setPadding(6f);
        labelTotal.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        labelTotal.setBorderColor(PdfTheme.BORDER);
        table.addCell(labelTotal);

        PdfPCell valueTotal = new PdfPCell(new Phrase(formatMoney(notice.totalOverdue()) + " MT", PdfTheme.boldFont()));
        valueTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueTotal.setPadding(6f);
        valueTotal.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        valueTotal.setBorderColor(PdfTheme.BORDER);
        table.addCell(valueTotal);

        return table;
    }

    private PdfPTable buildBankAccountsBlock(DebtCollectionNoticeDTO notice) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);

        cell.addElement(new Paragraph("DADOS BANCÁRIOS PARA LIQUIDAÇÃO:", PdfTheme.boldFont()));
        for (String bank : notice.paymentBankAccounts()) {
            cell.addElement(new Paragraph("• " + bank, PdfTheme.bodyFont()));
        }
        cell.addElement(new Paragraph("Nota: Por favor indicar o N.º de Cliente ou Factura no descritivo da transferência.", PdfTheme.smallFont()));

        table.addCell(cell);
        return table;
    }

    private Paragraph buildNoticeConclusion(DebtCollectionNoticeDTO notice) {
        Paragraph p = new Paragraph();
        p.setFont(PdfTheme.bodyFont());
        p.setLeading(14f);
        p.add(notice.customMessage() + "\n\n"
                + "Informamos que, nos termos da política de crédito em vigor, o não cumprimento desta regularização "
                + "poderá determinar a suspensão temporária do fornecimento a crédito e a instauração dos mecanismos legais cabíveis.");
        return p;
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{50f, 50f});
        } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_CENTER);
        left.addElement(new Paragraph("____________________________________", PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Departamento de Crédito e Cobrança", PdfTheme.boldFont()));
        left.addElement(new Paragraph("Multicore ERP Management", PdfTheme.smallFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        right.addElement(new Paragraph("____________________________________", PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Direcção Administrativa e Financeira", PdfTheme.boldFont()));
        right.addElement(new Paragraph("Carimbo / Assinatura Autorizada", PdfTheme.smallFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private void addHeaderCell(PdfPTable table, String title) {
        PdfPCell cell = new PdfPCell(new Phrase(title, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.bodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static String formatMoney(BigDecimal val) {
        BigDecimal safe = val == null ? BigDecimal.ZERO : val;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
