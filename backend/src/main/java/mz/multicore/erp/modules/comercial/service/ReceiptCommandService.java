package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandos de escrita de recibos para os outros módulos (adiantamentos): gravam por aqui, nunca pelo
 * {@link ReceiptRepository} (docs/MODULE_DECOUPLING_SPEC.md §2). Leitura: {@link ReceiptLookup}.
 */
@Service
@Transactional
public class ReceiptCommandService {

    private final ReceiptRepository receiptRepository;

    public ReceiptCommandService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    public Receipt save(Receipt receipt) {
        return receiptRepository.save(receipt);
    }
}
