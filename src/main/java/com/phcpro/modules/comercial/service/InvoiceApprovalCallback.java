package com.phcpro.modules.comercial.service;

import com.phcpro.modules.approvals.service.ApprovalCallback;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.inventory.service.InventoryService;
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
