package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.service.CompanyMovementProbe;
import org.springframework.stereotype.Component;

/** Any sales document (invoice, POS sale, credit/debit note) freezes the company's base currency (FX-01). */
@Component
class InvoiceMovementProbe implements CompanyMovementProbe {

    private final InvoiceRepository invoices;

    InvoiceMovementProbe(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    @Override
    public boolean hasMovements(Long companyId) {
        return invoices.existsByCompanyId(companyId);
    }
}
