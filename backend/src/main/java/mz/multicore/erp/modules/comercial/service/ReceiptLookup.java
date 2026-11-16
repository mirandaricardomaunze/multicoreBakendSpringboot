package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura de recibos para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). As entidades
 * devolvidas servem só para relações JPA e renderização dentro do backend e nunca saem para um Controller.
 * Escrita: {@link ReceiptCommandService}.
 */
@Service
@Transactional(readOnly = true)
public class ReceiptLookup {

    private final ReceiptRepository receiptRepository;

    public ReceiptLookup(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    public Optional<Receipt> find(Long id) {
        return id == null ? Optional.empty() : receiptRepository.findById(id);
    }

    /** Todos os recibos da empresa (todas as situações, incluindo anulados). */
    public List<Receipt> listForCompany(Long companyId) {
        return receiptRepository.findByCompanyId(companyId);
    }
}
