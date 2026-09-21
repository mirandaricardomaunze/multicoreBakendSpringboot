package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.performance.dto.EmployeeRankingDTO;
import mz.multicore.erp.modules.performance.dto.PerformanceReportDTO;
import mz.multicore.erp.modules.performance.service.PerformanceReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PerformanceReportPrintService {

    private final CompanyService companyService;
    private final PerformanceReportService reportService;

    public PerformanceReportPrintService(CompanyService companyService,
                                         PerformanceReportService reportService) {
        this.companyService = companyService;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, LocalDate from, LocalDate to) {
        PerformanceReportDTO report = reportService.generateReport(companyId, from, to);
        return renderReport(companyId, report);
    }

    @Transactional(readOnly = true)
    public byte[] renderReport(Long companyId, PerformanceReportDTO report) {
        Company company = companyService.getCompanyById(companyId);

        String title = String.format("Desempenho Comercial (%s a %s)",
                report.from() != null ? report.from() : "-",
                report.to() != null ? report.to() : "-");

        String[] headers = new String[]{
                "Pos", "Colaborador", "Receita (MZN)", "Margem (MZN)", "Ticket Médio", "Faturas", "% Meta"
        };

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);

        List<String[]> rows = new ArrayList<>();
        if (report.rankings() != null) {
            for (EmployeeRankingDTO r : report.rankings()) {
                rows.add(new String[]{
                        String.valueOf(r.rank()),
                        r.employeeName() != null ? r.employeeName() : "-",
                        df.format(r.revenue() != null ? r.revenue() : BigDecimal.ZERO),
                        df.format(r.grossMargin() != null ? r.grossMargin() : BigDecimal.ZERO),
                        df.format(r.avgTicket() != null ? r.avgTicket() : BigDecimal.ZERO),
                        String.valueOf(r.invoiceCount()),
                        (r.goalProgressPct() != null ? r.goalProgressPct() : BigDecimal.ZERO) + "%"
                });
            }
        }

        return TablePdfExporter.render(company, title, headers, rows.toArray(new String[0][]));
    }
}
