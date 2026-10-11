package mz.multicore.erp.modules.printing;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    private final ReceiptPrintService receiptPrintService;
    private final InvoicePrintService invoicePrintService;
    private final OrderPrintService orderPrintService;
    private final QuotationPrintService quotationPrintService;
    private final StockTransferPrintService stockTransferPrintService;
    private final PayslipPrintService payslipPrintService;
    private final CreditNotePrintService creditNotePrintService;
    private final DebitNotePrintService debitNotePrintService;
    private final InventoryReportPrintService inventoryReportPrintService;
    private final IvaDeclarationPrintService ivaDeclarationPrintService;
    private final GuideRemittancePrintService guideRemittancePrintService;
    private final DeliveryGuidePrintService deliveryGuidePrintService;
    private final PayrollFiscalMapPrintService payrollFiscalMapPrintService;
    private final ProductLabelPrintService productLabelPrintService;
    private final InventoryCountSheetPrintService inventoryCountSheetPrintService;
    private final POSZReportPrintService posZReportPrintService;
    private final WorkSheetPrintService workSheetPrintService;
    private final EmploymentContractPrintService employmentContractPrintService;
    private final TerminationPrintService terminationPrintService;
    private final TableExportPrintService tableExportPrintService;
    private final PerformanceReportPrintService performanceReportPrintService;
    private final StoreVoucherPrintService storeVoucherPrintService;
    private final AssetDepreciationMapPrintService assetDepreciationMapPrintService;
    private final ValuedInventoryBookPrintService valuedInventoryBookPrintService;
    private final SensitiveDocumentPrintService sensitivePrintService;

    @org.springframework.beans.factory.annotation.Autowired
    public PrintController(
            ReceiptPrintService receiptPrintService,
            InvoicePrintService invoicePrintService,
            OrderPrintService orderPrintService,
            QuotationPrintService quotationPrintService,
            StockTransferPrintService stockTransferPrintService,
            PayslipPrintService payslipPrintService,
            CreditNotePrintService creditNotePrintService,
            DebitNotePrintService debitNotePrintService,
            InventoryReportPrintService inventoryReportPrintService,
            IvaDeclarationPrintService ivaDeclarationPrintService,
            GuideRemittancePrintService guideRemittancePrintService,
            DeliveryGuidePrintService deliveryGuidePrintService,
            PayrollFiscalMapPrintService payrollFiscalMapPrintService,
            ProductLabelPrintService productLabelPrintService,
            InventoryCountSheetPrintService inventoryCountSheetPrintService,
            POSZReportPrintService posZReportPrintService,
            WorkSheetPrintService workSheetPrintService,
            EmploymentContractPrintService employmentContractPrintService,
            TerminationPrintService terminationPrintService,
            TableExportPrintService tableExportPrintService,
            PerformanceReportPrintService performanceReportPrintService,
            StoreVoucherPrintService storeVoucherPrintService,
            AssetDepreciationMapPrintService assetDepreciationMapPrintService,
            ValuedInventoryBookPrintService valuedInventoryBookPrintService,
            SensitiveDocumentPrintService sensitivePrintService
    ) {
        this.receiptPrintService = receiptPrintService;
        this.invoicePrintService = invoicePrintService;
        this.orderPrintService = orderPrintService;
        this.quotationPrintService = quotationPrintService;
        this.stockTransferPrintService = stockTransferPrintService;
        this.payslipPrintService = payslipPrintService;
        this.creditNotePrintService = creditNotePrintService;
        this.debitNotePrintService = debitNotePrintService;
        this.inventoryReportPrintService = inventoryReportPrintService;
        this.ivaDeclarationPrintService = ivaDeclarationPrintService;
        this.guideRemittancePrintService = guideRemittancePrintService;
        this.deliveryGuidePrintService = deliveryGuidePrintService;
        this.payrollFiscalMapPrintService = payrollFiscalMapPrintService;
        this.productLabelPrintService = productLabelPrintService;
        this.inventoryCountSheetPrintService = inventoryCountSheetPrintService;
        this.posZReportPrintService = posZReportPrintService;
        this.workSheetPrintService = workSheetPrintService;
        this.employmentContractPrintService = employmentContractPrintService;
        this.terminationPrintService = terminationPrintService;
        this.tableExportPrintService = tableExportPrintService;
        this.performanceReportPrintService = performanceReportPrintService;
        this.storeVoucherPrintService = storeVoucherPrintService;
        this.assetDepreciationMapPrintService = assetDepreciationMapPrintService;
        this.valuedInventoryBookPrintService = valuedInventoryBookPrintService;
        this.sensitivePrintService = sensitivePrintService;
    }

    public PrintController(
            ReceiptPrintService receiptPrintService,
            InvoicePrintService invoicePrintService,
            OrderPrintService orderPrintService,
            QuotationPrintService quotationPrintService,
            StockTransferPrintService stockTransferPrintService,
            PayslipPrintService payslipPrintService,
            CreditNotePrintService creditNotePrintService,
            DebitNotePrintService debitNotePrintService,
            InventoryReportPrintService inventoryReportPrintService,
            IvaDeclarationPrintService ivaDeclarationPrintService,
            GuideRemittancePrintService guideRemittancePrintService,
            DeliveryGuidePrintService deliveryGuidePrintService,
            PayrollFiscalMapPrintService payrollFiscalMapPrintService,
            ProductLabelPrintService productLabelPrintService,
            InventoryCountSheetPrintService inventoryCountSheetPrintService,
            POSZReportPrintService posZReportPrintService,
            WorkSheetPrintService workSheetPrintService,
            EmploymentContractPrintService employmentContractPrintService,
            TerminationPrintService terminationPrintService,
            TableExportPrintService tableExportPrintService,
            PerformanceReportPrintService performanceReportPrintService,
            StoreVoucherPrintService storeVoucherPrintService
    ) {
        this(receiptPrintService, invoicePrintService, orderPrintService, quotationPrintService,
                stockTransferPrintService, payslipPrintService, creditNotePrintService, debitNotePrintService,
                inventoryReportPrintService, ivaDeclarationPrintService, guideRemittancePrintService,
                deliveryGuidePrintService, payrollFiscalMapPrintService, productLabelPrintService,
                inventoryCountSheetPrintService, posZReportPrintService, workSheetPrintService,
                employmentContractPrintService, terminationPrintService, tableExportPrintService,
                performanceReportPrintService, storeVoucherPrintService, null, null, null);
    }

    /**
     * Exportação de uma listagem do desktop. É {@code POST} porque as linhas vêm no corpo: o
     * cliente manda o que está no ecrã e o servidor devolve-o em PDF com o cabeçalho da empresa.
     */
    @PostMapping("/table")
    public ResponseEntity<Resource> tableExport(
            @RequestParam Long companyId,
            @Valid @RequestBody TableExportRequest request
    ) {
        return pdfResponse(tableExportPrintService.render(companyId, request), "listagem");
    }

    @GetMapping("/employment-contract/{contractId}")
    public ResponseEntity<Resource> employmentContract(@PathVariable Long contractId) {
        return pdfResponse(first(SensitiveDocumentType.EMPLOYMENT_CONTRACT, contractId,
                () -> employmentContractPrintService.render(contractId)), "contrato-" + contractId);
    }

    @GetMapping("/termination-settlement/{terminationId}")
    public ResponseEntity<Resource> terminationSettlement(@PathVariable Long terminationId) {
        return pdfResponse(first(SensitiveDocumentType.TERMINATION_SETTLEMENT, terminationId,
                        () -> terminationPrintService.renderSettlement(terminationId)),
                "acerto-final-" + terminationId);
    }

    @GetMapping("/work-certificate/{terminationId}")
    public ResponseEntity<Resource> workCertificate(@PathVariable Long terminationId) {
        return pdfResponse(first(SensitiveDocumentType.TERMINATION_CERTIFICATE, terminationId,
                        () -> terminationPrintService.renderCertificate(terminationId)),
                "certificado-trabalho-" + terminationId);
    }

    @GetMapping("/receipt/{invoiceId}")
    public ResponseEntity<Resource> receipt(@PathVariable Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.RECEIPT, invoiceId,
                () -> receiptPrintService.render(invoiceId)), "recibo-" + invoiceId);
    }

    @GetMapping("/pos-z-report/{sessionId}")
    public ResponseEntity<Resource> posZReport(@PathVariable Long sessionId) {
        return pdfResponse(first(SensitiveDocumentType.POS_Z_REPORT, sessionId,
                () -> posZReportPrintService.render(sessionId)), "fecho-caixa-Z-" + sessionId);
    }

    @GetMapping("/pos-voucher/{voucherId}")
    public ResponseEntity<Resource> posVoucher(@PathVariable Long voucherId) {
        return pdfResponse(first(SensitiveDocumentType.STORE_VOUCHER, voucherId,
                () -> storeVoucherPrintService.render(voucherId)), "vale-compras-" + voucherId);
    }

    @GetMapping("/pos-voucher/code/{code}")
    public ResponseEntity<Resource> posVoucherByCode(@PathVariable String code) {
        Long voucherId = storeVoucherPrintService.findIdByCode(code);
        return pdfResponse(first(SensitiveDocumentType.STORE_VOUCHER, voucherId,
                () -> storeVoucherPrintService.render(voucherId)), "vale-compras-" + code);
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<Resource> invoice(@PathVariable Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.INVOICE, invoiceId,
                () -> invoicePrintService.render(invoiceId)), "fatura-" + invoiceId);
    }

    @GetMapping("/invoice/{invoiceId}/thermal")
    public ResponseEntity<Resource> invoiceThermal(@PathVariable Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.INVOICE, invoiceId,
                () -> invoicePrintService.renderThermal(invoiceId)), "fatura-termica-" + invoiceId);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Resource> order(@PathVariable Long orderId) {
        return pdfResponse(first(SensitiveDocumentType.ORDER, orderId,
                () -> orderPrintService.render(orderId)), "encomenda-" + orderId);
    }

    @GetMapping("/quotation/{quotationId}")
    public ResponseEntity<Resource> quotation(@PathVariable Long quotationId) {
        return pdfResponse(quotationPrintService.render(quotationId), "cotacao-" + quotationId);
    }

    @GetMapping("/quotation/{quotationId}/thermal")
    public ResponseEntity<Resource> quotationThermal(@PathVariable Long quotationId) {
        return pdfResponse(quotationPrintService.renderThermal(quotationId), "cotacao-termica-" + quotationId);
    }

    @GetMapping("/guide/{invoiceId}")
    public ResponseEntity<Resource> guide(@PathVariable Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.REMITTANCE_GUIDE, invoiceId,
                () -> guideRemittancePrintService.render(invoiceId)), "guia-remessa-" + invoiceId);
    }

    @GetMapping("/delivery-guide/{guideId}")
    public ResponseEntity<Resource> deliveryGuide(@PathVariable Long guideId) {
        return pdfResponse(first(SensitiveDocumentType.DELIVERY_GUIDE, guideId,
                () -> deliveryGuidePrintService.render(guideId)), "guia-remessa-" + guideId);
    }

    @GetMapping("/work-sheet/{workSheetId}")
    public ResponseEntity<Resource> workSheet(@PathVariable Long workSheetId) {
        return pdfResponse(workSheetPrintService.render(workSheetId), "folha-obra-" + workSheetId);
    }

    @GetMapping("/stock-transfer/{transferId}")
    public ResponseEntity<Resource> stockTransfer(@PathVariable Long transferId) {
        return pdfResponse(first(SensitiveDocumentType.STOCK_TRANSFER, transferId,
                () -> stockTransferPrintService.render(transferId)), "transferencia-" + transferId);
    }

    @GetMapping("/stock-transfer/{transferId}/picking")
    public ResponseEntity<Resource> stockTransferPicking(@PathVariable Long transferId) {
        return pdfResponse(first(SensitiveDocumentType.STOCK_TRANSFER_PICKING, transferId,
                () -> stockTransferPrintService.renderPickingSheet(transferId)), "separacao-transferencia-" + transferId);
    }

    @GetMapping("/payslip/{payslipId}")
    public ResponseEntity<Resource> payslip(@PathVariable Long payslipId) {
        return pdfResponse(first(SensitiveDocumentType.PAYSLIP, payslipId,
                () -> payslipPrintService.render(payslipId)), "recibo-salario-" + payslipId);
    }

    @GetMapping("/credit-note/{id}")
    public ResponseEntity<Resource> creditNote(@PathVariable Long id) {
        return pdfResponse(first(SensitiveDocumentType.CREDIT_NOTE, id,
                () -> creditNotePrintService.render(id)), "nota-credito-" + id);
    }

    @GetMapping("/debit-note/{id}")
    public ResponseEntity<Resource> debitNote(@PathVariable Long id) {
        return pdfResponse(first(SensitiveDocumentType.DEBIT_NOTE, id,
                () -> debitNotePrintService.render(id)), "nota-debito-" + id);
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
            @org.springframework.web.bind.annotation.RequestParam int month,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0") java.math.BigDecimal previousCredit
    ) {
        return pdfResponse(
                ivaDeclarationPrintService.render(companyId, year, month, previousCredit),
                "declaracao-iva-" + year + "-" + String.format("%02d", month));
    }

    @GetMapping("/payroll-fiscal-map")
    public ResponseEntity<Resource> payrollFiscalMap(
            @org.springframework.web.bind.annotation.RequestParam Long companyId,
            @org.springframework.web.bind.annotation.RequestParam int year,
            @org.springframework.web.bind.annotation.RequestParam int month
    ) {
        return pdfResponse(
                payrollFiscalMapPrintService.render(companyId, year, month),
                "mapa-fiscal-salarial-" + year + "-" + String.format("%02d", month));
    }

    @GetMapping("/product-label")
    public ResponseEntity<Resource> productLabel(
            @org.springframework.web.bind.annotation.RequestParam Long companyId,
            @org.springframework.web.bind.annotation.RequestParam java.util.List<Long> productIds,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int copies,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String lotNumber,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String expiryDate,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "A4") String layout
    ) {
        return pdfResponse(productLabelPrintService.render(companyId, productIds, copies, lotNumber, expiryDate, layout), "etiquetas-" + companyId);
    }

    @GetMapping("/inventory-count-sheet")
    public ResponseEntity<Resource> inventoryCountSheet(
            @org.springframework.web.bind.annotation.RequestParam Long companyId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long warehouseId
    ) {
        return pdfResponse(inventoryCountSheetPrintService.render(companyId, warehouseId),
                "folha-contagem-" + companyId);
    }

    @GetMapping("/performance-report")
    public ResponseEntity<Resource> performanceReport(
            @RequestParam Long companyId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to
    ) {
        return pdfResponse(performanceReportPrintService.render(companyId, from, to),
                "relatorio-desempenho-" + companyId);
    }

    @PostMapping("/performance-report")
    public ResponseEntity<Resource> performanceReportPost(
            @RequestParam Long companyId,
            @RequestBody @Valid mz.multicore.erp.modules.performance.dto.PerformanceReportDTO report
    ) {
        return pdfResponse(performanceReportPrintService.renderReport(companyId, report),
                "relatorio-desempenho-" + companyId);
    }

    @GetMapping("/asset-depreciation-map")
    public ResponseEntity<Resource> assetDepreciationMap(
            @RequestParam Long companyId,
            @RequestParam(required = false) Integer fiscalYear
    ) {
        int year = fiscalYear != null ? fiscalYear : java.time.LocalDate.now().getYear();
        return pdfResponse(
                assetDepreciationMapPrintService.render(companyId, year),
                "mapa-amortizacoes-mod22-" + companyId + "-" + year);
    }

    @GetMapping("/valued-inventory-book")
    public ResponseEntity<Resource> valuedInventoryBook(
            @RequestParam Long companyId,
            @RequestParam(required = false) Integer fiscalYear
    ) {
        int year = fiscalYear != null ? fiscalYear : java.time.LocalDate.now().getYear();
        return pdfResponse(
                valuedInventoryBookPrintService.render(companyId, year),
                "livro-inventario-valorizado-" + companyId + "-" + year);
    }

    @GetMapping("/status/{type}/{documentId}")
    public DocumentPrintStatusDTO printStatus(@PathVariable SensitiveDocumentType type,
                                              @PathVariable Long documentId) {
        return sensitivePrintService.status(type, documentId);
    }

    @GetMapping("/status/{type}")
    public java.util.List<DocumentPrintStatusDTO> printStatuses(
            @PathVariable SensitiveDocumentType type,
            @RequestParam java.util.List<Long> ids) {
        return sensitivePrintService.statuses(type, ids);
    }

    @PostMapping("/{type}/{documentId}/reprint")
    public ResponseEntity<Resource> reprint(
            @PathVariable SensitiveDocumentType type,
            @PathVariable Long documentId,
            @Valid @RequestBody ReprintAuthorizationRequest request) {
        byte[] pdf = sensitivePrintService.reprint(type, documentId, request,
                () -> renderSensitive(type, documentId));
        return pdfResponse(pdf, "reimpressao-" + fileBase(type) + "-" + documentId);
    }

    private byte[] first(SensitiveDocumentType type, Long documentId,
                         java.util.function.Supplier<byte[]> renderer) {
        if (sensitivePrintService == null) {
            return renderer.get();
        }
        return sensitivePrintService.firstPrint(type, documentId, null, renderer);
    }

    private byte[] renderSensitive(SensitiveDocumentType type, Long documentId) {
        return switch (type) {
            case INVOICE -> invoicePrintService.render(documentId);
            case RECEIPT -> receiptPrintService.render(documentId);
            case CREDIT_NOTE -> creditNotePrintService.render(documentId);
            case DEBIT_NOTE -> debitNotePrintService.render(documentId);
            case DELIVERY_GUIDE -> deliveryGuidePrintService.render(documentId);
            case REMITTANCE_GUIDE -> guideRemittancePrintService.render(documentId);
            case STOCK_TRANSFER -> stockTransferPrintService.render(documentId);
            case STOCK_TRANSFER_PICKING -> stockTransferPrintService.renderPickingSheet(documentId);
            case ORDER -> orderPrintService.render(documentId);
            case PAYSLIP -> payslipPrintService.render(documentId);
            case EMPLOYMENT_CONTRACT -> employmentContractPrintService.render(documentId);
            case TERMINATION_SETTLEMENT -> terminationPrintService.renderSettlement(documentId);
            case TERMINATION_CERTIFICATE -> terminationPrintService.renderCertificate(documentId);
            case STORE_VOUCHER -> storeVoucherPrintService.render(documentId);
            case POS_Z_REPORT -> posZReportPrintService.render(documentId);
            case ORDER_PICKING -> throw new mz.multicore.erp.architecture.exception.BusinessRuleException(
                    "A reimpressão da guia de separação deve ser autorizada na lista de encomendas.");
        };
    }

    private String fileBase(SensitiveDocumentType type) {
        return type.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    private ResponseEntity<Resource> pdfResponse(byte[] bytes, String fileBase) {
        String safeFileBase = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeFileName(fileBase);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileBase + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(resource);
    }
}
