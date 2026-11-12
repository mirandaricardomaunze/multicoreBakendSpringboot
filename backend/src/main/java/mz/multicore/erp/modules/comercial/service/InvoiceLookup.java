package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de facturas para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2).
 * É o único ponto pelo qual um módulo alheio lê {@link Invoice}. As entidades devolvidas servem só para
 * relações JPA e renderização dentro do backend e nunca saem para um Controller.
 */
@Service
@Transactional(readOnly = true)
public class InvoiceLookup {

    static final String NOT_FOUND = "Fatura não encontrada.";

    private final InvoiceRepository invoiceRepository;

    public InvoiceLookup(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /** Factura pelo id; {@code BusinessRuleException("Fatura não encontrada.")} se não existir. */
    public Invoice require(Long id) {
        return find(id).orElseThrow(() -> new BusinessRuleException(NOT_FOUND));
    }

    public Optional<Invoice> find(Long id) {
        return id == null ? Optional.empty() : invoiceRepository.findById(id);
    }

    /** Facturas da empresa criadas no intervalo [from, to] (todas as situações, incluindo anuladas). */
    public List<Invoice> listCreatedBetween(Long companyId, LocalDateTime from, LocalDateTime to) {
        return invoiceRepository.findByCompanyIdAndCreatedAtBetween(companyId, from, to);
    }
}
