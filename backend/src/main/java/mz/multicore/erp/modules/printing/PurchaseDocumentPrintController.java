package mz.multicore.erp.modules.printing;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Purchase-order and supplier-purchase PDFs (SPEC moeda estrangeira §3.3.1); MANAGER/ADMIN via ApiRolePolicy. */
@RestController
@RequestMapping("/api/print")
public class PurchaseDocumentPrintController {

    private final PurchaseDocumentPrintService printService;

    public PurchaseDocumentPrintController(PurchaseDocumentPrintService printService) {
        this.printService = printService;
    }

    @GetMapping("/purchase-order/{orderId}")
    public ResponseEntity<byte[]> purchaseOrder(@PathVariable Long orderId) {
        return pdf(printService.renderPurchaseOrder(orderId), "encomenda-fornecedor-" + orderId);
    }

    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<byte[]> purchase(@PathVariable Long purchaseId) {
        return pdf(printService.renderPurchase(purchaseId), "compra-fornecedor-" + purchaseId);
    }

    private static ResponseEntity<byte[]> pdf(byte[] bytes, String fileBase) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileBase + ".pdf\"")
                .contentLength(bytes.length)
                .body(bytes);
    }
}
