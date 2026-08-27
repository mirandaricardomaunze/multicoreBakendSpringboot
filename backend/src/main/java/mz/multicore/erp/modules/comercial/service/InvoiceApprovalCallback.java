package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.approvals.service.ApprovalCallback;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class InvoiceApprovalCallback implements ApprovalCallback {

    private final InvoiceRepository invoiceRepository;
    private final InventoryService inventoryService;

    public InvoiceApprovalCallback(InvoiceRepository invoiceRepository, InventoryService inventoryService) {
        this.invoiceRepository = invoiceRepository;
        this.inventoryService = inventoryService;
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

            // Deduct stock for each invoice line in the warehouse
            invoice.getLines().forEach(line -> {
                String desc = String.format("Saída Fatura %s - Cliente %s", invoice.getInvoiceNumber(), invoice.getClient().getName());
                inventoryService.registerMovement(
                        line.getProduct(),
                        invoice.getWarehouse(),
                        line.getQuantity().negate(),
                        "SALE",
                        line.getBatchNumber(),
                        line.getSerialNumber(),
                        desc
                );
            });
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
