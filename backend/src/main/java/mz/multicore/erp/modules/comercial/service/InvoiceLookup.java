package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
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

    /** Todas as facturas da empresa (todas as situações). */
    public List<Invoice> listForCompany(Long companyId) {
        return invoiceRepository.findByCompanyId(companyId);
    }

    /** Todas as facturas de todas as empresas — só para leituras de plataforma sem empresa activa. */
    public List<Invoice> listAll() {
        return invoiceRepository.findAll();
    }

    /** Facturas da empresa nos estados indicados (ex.: {@code InvoiceStatus.collectableStatuses()}). */
    public List<Invoice> listByStatus(Long companyId, Collection<InvoiceStatus> statuses) {
        return invoiceRepository.findByCompanyIdAndStatusIn(companyId, statuses);
    }

    /** Factura já gravada para a mesma referência de contingência (idempotência do POS offline). */
    public Optional<Invoice> findByContingencyReference(Long companyId, String contingencyReference) {
        return invoiceRepository.findByCompanyIdAndContingencyReference(companyId, contingencyReference);
    }

    /** Pares {@code [productId, quantidade]} vendidos desde {@code since} nos estados indicados. */
    public List<Object[]> sumQuantitySoldByProductSince(Long companyId, Collection<InvoiceStatus> statuses,
                                                       LocalDateTime since) {
        return invoiceRepository.sumQuantitySoldByProductSince(companyId, statuses, since);
    }
}
