package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.fiscal.dto.FiscalSalesExportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class FiscalSalesExportService {

    private static final Set<InvoiceStatus> ISSUED = EnumSet.of(
            InvoiceStatus.APPROVED, InvoiceStatus.PAID, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.CANCELLED);

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final FiscalSalesXmlBuilder xmlBuilder;

    @Autowired
    public FiscalSalesExportService(InvoiceRepository invoiceRepository, CompanyRepository companyRepository, FiscalSalesXmlBuilder xmlBuilder) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.xmlBuilder = xmlBuilder;
    }

    public FiscalSalesExportService(InvoiceRepository invoiceRepository, CompanyRepository companyRepository) {
        this(invoiceRepository, companyRepository, new FiscalSalesXmlBuilder());
    }

    @Transactional(readOnly = true)
    public FiscalSalesExportDTO exportSales(Long companyId, LocalDate from, LocalDate to) {
        PermissionGuard.requireManagerOrAdmin("exportar ficheiro fiscal de vendas");
        CurrentUserContext.requireCompany(companyId);
        if (from == null || to == null) throw new BusinessRuleException("Indique o período (datas de início e fim) da exportação.");
        if (to.isBefore(from)) throw new BusinessRuleException("A data final não pode ser anterior à data inicial.");
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        List<Invoice> invoices = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(i -> ISSUED.contains(i.getStatus()) && withinPeriod(i.getCreatedAt(), start, end))
                .sorted(Comparator.comparing(Invoice::getInvoiceNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        BigDecimal totalNet = BigDecimal.ZERO, totalTax = BigDecimal.ZERO, totalGross = BigDecimal.ZERO;
        Map<Long, Client> customers = new LinkedHashMap<>();
        Set<BigDecimal> taxRates = new TreeSet<>();

        for (Invoice inv : invoices) {
            if (inv.getStatus() != InvoiceStatus.CANCELLED) {
                totalNet = totalNet.add(xmlBuilder.nz(inv.getTotalBeforeTax()));
                totalTax = totalTax.add(xmlBuilder.nz(inv.getTaxAmount()));
                totalGross = totalGross.add(xmlBuilder.nz(inv.getTotalAmount()));
            }
            if (inv.getClient() != null) customers.putIfAbsent(inv.getClient().getId(), inv.getClient());
            for (InvoiceLine line : inv.getLines()) taxRates.add(xmlBuilder.percent(line.getTaxRate()));
        }

        String xml = xmlBuilder.buildXml(company, from, to, invoices, customers.values(), taxRates, totalNet);
        return new FiscalSalesExportDTO(xml, invoices.size(), xmlBuilder.scale(totalNet), xmlBuilder.scale(totalTax), xmlBuilder.scale(totalGross));
    }

    private boolean withinPeriod(LocalDateTime when, LocalDateTime start, LocalDateTime end) {
        return when != null && !when.isBefore(start) && !when.isAfter(end);
    }

    static String escape(String value) {
        return FiscalSalesXmlBuilder.escape(value);
    }
}
