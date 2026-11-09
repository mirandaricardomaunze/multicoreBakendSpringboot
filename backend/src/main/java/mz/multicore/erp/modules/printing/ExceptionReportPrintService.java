package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;
import mz.multicore.erp.modules.exceptions.dto.ExceptionReportDTO;
import mz.multicore.erp.modules.exceptions.model.ExceptionSeverity;
import mz.multicore.erp.modules.exceptions.service.ExceptionReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Anti-fraud exception report in PDF, for the manager's file or the owner's meeting. */
@Service
public class ExceptionReportPrintService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExceptionReportService reportService;
    private final CompanyRepository companyRepository;

    public ExceptionReportPrintService(ExceptionReportService reportService, CompanyRepository companyRepository) {
        this.reportService = reportService;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);
        ExceptionReportDTO report = reportService.report(companyId, from, to);
        var company = companyRepository.findById(companyId).orElseThrow();
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Relatório de Excepções", DATE.format(from) + " a " + DATE.format(to), null));
            doc.add(new Paragraph("Alta: " + report.high() + "   ·   Média: " + report.medium() + "   ·   Baixa: " + report.low(),
                    PdfTheme.boldFont()));
            doc.add(PdfDocumentBuilder.spacer(8f));
            if (report.findings().isEmpty()) {
                doc.add(new Paragraph("Nenhuma excepção no período.", PdfTheme.bodyFont()));
                return;
            }
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            try {
                table.setWidths(new float[]{9f, 11f, 20f, 38f, 12f, 10f});
            } catch (Exception ignored) {
                // Widths are cosmetic; the default split is still readable.
            }
            for (String header : new String[]{"Gravidade", "Sector", "Controlo", "O que aconteceu", "Operador", "Data"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, PdfTheme.tableHeaderFont()));
                cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
                cell.setBorderColor(PdfTheme.BORDER);
                cell.setPadding(4f);
                table.addCell(cell);
            }
            for (ExceptionFindingDTO f : report.findings()) {
                table.addCell(cell(severity(f.severity()), f.severity() == ExceptionSeverity.HIGH));
                table.addCell(cell(f.sector(), false));
                table.addCell(cell(f.title(), false));
                table.addCell(cell(f.detail() + (f.reference() == null ? "" : " (" + f.reference() + ")"), false));
                table.addCell(cell(f.operator() == null ? "—" : f.operator(), false));
                table.addCell(cell(f.date() == null ? "—" : DATE.format(f.date()), false));
            }
            doc.add(table);
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(new Paragraph("Uma excepção não prova fraude: indica o que deve ser verificado. Os limites estão em "
                    + "docs/ANTIFRAUDE_SECTORES_SPEC.md.", PdfTheme.smallFont()));
        });
    }

    static String severity(ExceptionSeverity s) {
        return switch (s) {
            case HIGH -> "ALTA";
            case MEDIUM -> "MÉDIA";
            case LOW -> "BAIXA";
        };
    }

    private static PdfPCell cell(String text, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, bold ? PdfTheme.boldFont() : PdfTheme.tableBodyFont()));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(4f);
        return cell;
    }
}
