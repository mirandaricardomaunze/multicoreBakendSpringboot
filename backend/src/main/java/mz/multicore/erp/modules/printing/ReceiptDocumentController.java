package mz.multicore.erp.modules.printing;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A4 receipt (RC) by receipt id; same access as the other receipt prints. */
@RestController
@RequestMapping("/api/print/receipt/document")
public class ReceiptDocumentController {

    private final ReceiptDocumentPrintService printService;

    public ReceiptDocumentController(ReceiptDocumentPrintService printService) {
        this.printService = printService;
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<byte[]> receipt(@PathVariable Long receiptId) {
        byte[] pdf = printService.render(receiptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recibo-" + receiptId + ".pdf\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
