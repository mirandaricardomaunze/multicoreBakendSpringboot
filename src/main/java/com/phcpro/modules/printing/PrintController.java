package com.phcpro.modules.printing;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    private final ReceiptPrintService receiptPrintService;
    private final InvoicePrintService invoicePrintService;
    private final OrderPrintService orderPrintService;
    private final StockTransferPrintService stockTransferPrintService;
    private final PayslipPrintService payslipPrintService;
    private final CreditNotePrintService creditNotePrintService;
    private final DebitNotePrintService debitNotePrintService;
    private final InventoryReportPrintService inventoryReportPrintService;
    private final IvaDeclarationPrintService ivaDeclarationPrintService;

    public PrintController(
            ReceiptPrintService receiptPrintService,
            InvoicePrintService invoicePrintService,
            OrderPrintService orderPrintService,
            StockTransferPrintService stockTransferPrintService,
            PayslipPrintService payslipPrintService,
            CreditNotePrintService creditNotePrintService,
            DebitNotePrintService debitNotePrintService,
            InventoryReportPrintService inventoryReportPrintService,
            IvaDeclarationPrintService ivaDeclarationPrintService
    ) {
        this.receiptPrintService = receiptPrintService;
        this.invoicePrintService = invoicePrintService;
        this.orderPrintService = orderPrintService;
        this.stockTransferPrintService = stockTransferPrintService;
        this.payslipPrintService = payslipPrintService;
        this.creditNotePrintService = creditNotePrintService;
        this.debitNotePrintService = debitNotePrintService;
        this.inventoryReportPrintService = inventoryReportPrintService;
        this.ivaDeclarationPrintService = ivaDeclarationPrintService;
    }

    @GetMapping("/receipt/{invoiceId}")
    public ResponseEntity<Resource> receipt(@PathVariable Long invoiceId) {
        return pdfResponse(receiptPrintService.render(invoiceId), "recibo-" + invoiceId);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<Resource> invoice(@PathVariable Long invoiceId) {
        return pdfResponse(invoicePrintService.render(invoiceId), "fatura-" + invoiceId);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Resource> order(@PathVariable Long orderId) {
        return pdfResponse(orderPrintService.render(orderId), "encomenda-" + orderId);
    }

    @GetMapping("/stock-transfer/{transferId}")
    public ResponseEntity<Resource> stockTransfer(@PathVariable Long transferId) {
        return pdfResponse(stockTransferPrintService.render(transferId), "transferencia-" + transferId);
    }

    @GetMapping("/payslip/{payslipId}")
    public ResponseEntity<Resource> payslip(@PathVariable Long payslipId) {
        return pdfResponse(payslipPrintService.render(payslipId), "recibo-salario-" + payslipId);
    }

    @GetMapping("/credit-note/{id}")
    public ResponseEntity<Resource> creditNote(@PathVariable Long id) {
        return pdfResponse(creditNotePrintService.render(id), "nota-credito-" + id);
    }

    @GetMapping("/debit-note/{id}")
    public ResponseEntity<Resource> debitNote(@PathVariable Long id) {
        return pdfResponse(debitNotePrintService.render(id), "nota-debito-" + id);
    }

    @GetMapping("/inventory")
    public ResponseEntity<Resource> inventory(
            @org.springframework.web.bind.annotation.RequestParam Long companyId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long warehouseId
    ) {
        return pdfResponse(
                inventoryReportPrintService.render(companyId, warehouseId),
                "inventario-" + companyId + (warehouseId != null ? "-w" + warehouseId : ""));
    }

    @GetMapping("/iva-declaration")
    public ResponseEntity<Resource> ivaDeclaration(
            @org.springframework.web.bind.annotation.RequestParam Long companyId,
            @org.springframework.web.bind.annotation.RequestParam int year,
            @org.springframework.web.bind.annotation.RequestParam int month
    ) {
        return pdfResponse(
                ivaDeclarationPrintService.render(companyId, year, month),
                "declaracao-iva-" + year + "-" + String.format("%02d", month));
    }

    private ResponseEntity<Resource> pdfResponse(byte[] bytes, String fileBase) {
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileBase + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(resource);
    }
}
