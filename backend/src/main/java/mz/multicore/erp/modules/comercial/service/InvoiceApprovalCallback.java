package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.approvals.service.ApprovalCallback;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class InvoiceApprovalCallback implements ApprovalCallback {

    private final InvoiceRepository invoiceRepository;
    private final ComercialInvoiceLifecycleService invoiceLifecycleService;

    public InvoiceApprovalCallback(InvoiceRepository invoiceRepository,
                                   @Lazy ComercialInvoiceLifecycleService invoiceLifecycleService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLifecycleService = invoiceLifecycleService;
    }

    @Override
    public boolean supports(String documentType) {
        return "INVOICE".equalsIgnoreCase(documentType);
    }

    @Override
    @Transactional
    public void onApproved(Long documentId) {
        invoiceRepository.findById(documentId).ifPresent(invoice -> {
            invoice.setStatus(InvoiceStatus.APPROVED);
            invoiceRepository.save(invoice);

            // Mesma saída da fatura directa, incluindo a verificação de stock reservado.
            invoiceLifecycleService.deductStockForInvoice(invoice);
            // Sem isto, a venda com desconto especial nunca chegava à contabilidade.
            invoiceLifecycleService.publishSale(invoice, BigDecimal.ZERO, false);
        });
    }

    @Override
    @Transactional
    public void onRejected(Long documentId, String reason) {
        invoiceRepository.findById(documentId).ifPresent(invoice -> {
            invoice.setStatus(InvoiceStatus.REJECTED);
            invoice.setRejectionReason(reason);
            invoiceRepository.save(invoice);
        });
    }
}
