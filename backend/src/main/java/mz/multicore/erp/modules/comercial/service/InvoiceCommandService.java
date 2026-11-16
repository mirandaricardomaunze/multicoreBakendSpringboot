package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Comandos de escrita de facturas para os outros módulos (POS, tesouraria, adiantamentos): bloqueiam e
 * gravam por aqui, nunca pelo {@link InvoiceRepository} (docs/MODULE_DECOUPLING_SPEC.md §2).
 * Leitura: {@link InvoiceLookup}.
 */
@Service
@Transactional
public class InvoiceCommandService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceCommandService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /** Bloqueia a factura (PESSIMISTIC_WRITE) já filtrada pela empresa, para liquidar/anular sem corridas. */
    public Optional<Invoice> lockForCompany(Long id, Long companyId) {
        return invoiceRepository.lockByIdAndCompanyId(id, companyId);
    }

    public Invoice save(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
}
