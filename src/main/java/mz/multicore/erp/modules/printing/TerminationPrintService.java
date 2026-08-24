package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.model.TerminationSettlementLine;
import mz.multicore.erp.modules.hr.service.TerminationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PDF do <b>acerto final</b> e do <b>certificado de trabalho</b>. Ver docs/RH_COMPLETO_SPEC.md §B3.
 *
 * <p>O acerto tem linhas e um total — ao contrário do contrato — mas <b>não usa</b> o
 * {@code LineItemsTableRenderer}/{@code TotalsBlockRenderer}: esses renderizam quantidade, preço
 * unitário e IVA, que um acerto não tem. Imprimir "IVA 0,00" num papel que o trabalhador assina
 * seria dizer-lhe uma coisa que não é verdade — a mesma decisão já tomada na folha de obra do CRM.
 *
 * <p>O certificado é deliberadamente <b>seco</b>: função, datas e nada mais. Um certificado que
 * opine sobre o desempenho de quem saiu deixa de ser um documento e passa a ser uma referência —
 * e a lei laboral não permite que a saída venha carimbada com juízos.
 */
@Service
public class TerminationPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TerminationService terminationService;

    public TerminationPrintService(TerminationService terminationService) {
        this.terminationService = terminationService;
    }

    @Transactional(readOnly = true)
    public byte[] renderSettlement(Long terminationId) {
        Termination t = terminationService.loadForPrint(terminationId);
        Company company = t.getEmployee().getCompany();

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Acerto Final de Contas",
                    t.getSettlementNumber()));
            doc.add(buildEmployeeBlock(t));
            doc.add(buildLinesTable(t));
            doc.add(PdfDocumentBuilder.spacer(6f));
            doc.add(buildNetBlock(t));
            if (t.getNotes() != null && !t.getNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(10f));
                doc.add(new Paragraph("Observações: " + t.getNotes(), PdfTheme.bodyFont()));
            }
            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(new Paragraph(
                    "Documento de acerto de contas por cessação do vínculo laboral. Não é documento "
                            + "fiscal.", PdfTheme.smallFont()));
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(SignatureBlockRenderer.build("Pela entidade empregadora", "O(A) trabalhador(a)"));
        });
    }

    @Transactional(readOnly = true)
    public byte[] renderCertificate(Long terminationId) {
        Termination t = terminationService.loadForPrint(terminationId);
        Company company = t.getEmployee().getCompany();

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Certificado de Trabalho",
                    t.getSettlementNumber()));
            doc.add(PdfDocumentBuilder.spacer(14f));
            doc.add(new Paragraph("Para os devidos efeitos se declara que:", PdfTheme.bodyFont()));
            doc.add(PdfDocumentBuilder.spacer(10f));

            String admission = t.getEmployee().getHireDate() == null
                    ? "data não registada" : t.getEmployee().getHireDate().format(DATE_FMT);
            Paragraph body = new Paragraph(String.format(
                    "%s, portador(a) do nº interno %s%s, prestou serviço nesta entidade desde %s até "
                            + "%s, exercendo as funções de %s.",
                    t.getEmployee().getName(), t.getEmployee().getEmployeeNumber(),
                    t.getEmployee().getTaxId() == null ? "" : " e NUIT " + t.getEmployee().getTaxId(),
                    admission, t.getTerminationDate().format(DATE_FMT),
                    jobTitleOf(t)), PdfTheme.bodyFont());
            body.setAlignment(Element.ALIGN_JUSTIFIED);
            doc.add(body);

            doc.add(PdfDocumentBuilder.spacer(10f));
            doc.add(new Paragraph("Motivo da cessação: " + t.getReason().getLabel(), PdfTheme.bodyFont()));
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(new Paragraph(LocalDate.now().format(DATE_FMT), PdfTheme.bodyFont()));
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(SignatureBlockRenderer.build("Pela entidade empregadora", ""));
        });
    }

    private String jobTitleOf(Termination t) {
        if (t.getContract() != null && t.getContract().getJobTitle() != null) {
            return t.getContract().getJobTitle();
        }
        return t.getEmployee().getRole() == null ? "não registada" : t.getEmployee().getRole();
    }

    private PdfPTable buildEmployeeBlock(Termination t) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{60f, 40f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Colaborador", PdfTheme.subtitleFont()));
        left.addElement(new Paragraph(t.getEmployee().getName(), PdfTheme.boldFont()));
        left.addElement(new Paragraph("Nº interno: " + t.getEmployee().getEmployeeNumber(),
                PdfTheme.bodyFont()));
        if (t.getContract() != null) {
            left.addElement(new Paragraph("Contrato: " + t.getContract().getContractNumber(),
                    PdfTheme.bodyFont()));
        }
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.addElement(rightAligned("Cessação", PdfTheme.subtitleFont()));
        right.addElement(rightAligned(t.getTerminationDate().format(DATE_FMT), PdfTheme.bodyFont()));
        right.addElement(rightAligned(t.getReason().getLabel(), PdfTheme.bodyFont()));
        right.addElement(rightAligned("Aviso prévio: " + (t.isNoticeServed() ? "cumprido" : "não cumprido"),
                PdfTheme.bodyFont()));
        right.addElement(rightAligned("Estado: " + t.getStatus().getLabel(), PdfTheme.boldFont()));
        table.addCell(right);
        return table;
    }

    private Paragraph rightAligned(String text, com.lowagie.text.Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        return paragraph;
    }

    private PdfPTable buildLinesTable(Termination t) {
        PdfPTable table = new PdfPTable(new float[]{70f, 30f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(4f);

        header(table, "Descrição", Element.ALIGN_LEFT);
        header(table, "Valor (MT)", Element.ALIGN_RIGHT);

        for (TerminationSettlementLine line : t.getLines()) {
            // O desconto sai com sinal negativo em vez de coluna própria: uma linha de acerto lê-se
            // de cima a baixo como uma conta, e é assim que o trabalhador a confere em papel.
            BigDecimal shown = line.isEarning() ? line.getAmount() : line.getAmount().negate();
            row(table, line.getDescription(), shown);
        }
        totalRow(table, "Total de ganhos", t.getTotalEarnings());
        totalRow(table, "Total de descontos", t.getTotalDeductions().negate());
        return table;
    }

    private PdfPTable buildNetBlock(Termination t) {
        PdfPTable wrapper = new PdfPTable(new float[]{50f, 50f});
        wrapper.setWidthPercentage(100);
        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        boolean owesCompany = t.getNetAmount().signum() < 0;
        PdfPTable inner = new PdfPTable(new float[]{60f, 40f});
        PdfPCell label = new PdfPCell(new Phrase(
                owesCompany ? "SALDO A FAVOR DA EMPRESA" : "LÍQUIDO A RECEBER", PdfTheme.subtitleFont()));
        PdfPCell value = new PdfPCell(new Phrase(
                MoneyFormat.format(t.getNetAmount().abs()), PdfTheme.subtitleFont()));
        label.setBorder(PdfPCell.NO_BORDER);
        value.setBorder(PdfPCell.NO_BORDER);
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        label.setPadding(6f);
        value.setPadding(6f);
        label.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        value.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        inner.addCell(label);
        inner.addCell(value);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void row(PdfPTable table, String label, BigDecimal value) {
        PdfPCell l = new PdfPCell(new Phrase(label, PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(MoneyFormat.formatPlain(value), PdfTheme.bodyFont()));
        l.setBorderColor(PdfTheme.BORDER);
        v.setBorderColor(PdfTheme.BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(4f);
        v.setPadding(4f);
        table.addCell(l);
        table.addCell(v);
    }

    private void totalRow(PdfPTable table, String label, BigDecimal value) {
        PdfPCell l = new PdfPCell(new Phrase(label, PdfTheme.boldFont()));
        PdfPCell v = new PdfPCell(new Phrase(MoneyFormat.formatPlain(value), PdfTheme.boldFont()));
        l.setBorderColor(PdfTheme.BORDER);
        v.setBorderColor(PdfTheme.BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(5f);
        v.setPadding(5f);
        l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        table.addCell(l);
        table.addCell(v);
    }
}
