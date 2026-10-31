package mz.multicore.erp.modules.printing;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.performance.dto.PerformanceReportDTO;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    private final PrintRequestDispatcher dispatcher;

    @Autowired
    public PrintController(PrintRequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public PrintController(
            ReceiptPrintService receiptPrintService, InvoicePrintService invoicePrintService,
            OrderPrintService orderPrintService, QuotationPrintService quotationPrintService,
            StockTransferPrintService stockTransferPrintService, PayslipPrintService payslipPrintService,
            CreditNotePrintService creditNotePrintService, DebitNotePrintService debitNotePrintService,
            InventoryReportPrintService inventoryReportPrintService, IvaDeclarationPrintService ivaDeclarationPrintService,
            GuideRemittancePrintService guideRemittancePrintService, DeliveryGuidePrintService deliveryGuidePrintService,
            PayrollFiscalMapPrintService payrollFiscalMapPrintService, ProductLabelPrintService productLabelPrintService,
            InventoryCountSheetPrintService inventoryCountSheetPrintService, POSZReportPrintService posZReportPrintService,
            WorkSheetPrintService workSheetPrintService, EmploymentContractPrintService employmentContractPrintService,
            TerminationPrintService terminationPrintService, TableExportPrintService tableExportPrintService,
            PerformanceReportPrintService performanceReportPrintService, StoreVoucherPrintService storeVoucherPrintService,
            AssetDepreciationMapPrintService assetDepreciationMapPrintService,
            ValuedInventoryBookPrintService valuedInventoryBookPrintService,
            SensitiveDocumentPrintService sensitivePrintService) {
        this(new PrintRequestDispatcher(
                receiptPrintService, invoicePrintService, orderPrintService, quotationPrintService,
                stockTransferPrintService, payslipPrintService, creditNotePrintService, debitNotePrintService,
                inventoryReportPrintService, ivaDeclarationPrintService, guideRemittancePrintService,
                deliveryGuidePrintService, payrollFiscalMapPrintService, productLabelPrintService,
                inventoryCountSheetPrintService, posZReportPrintService, workSheetPrintService,
                employmentContractPrintService, terminationPrintService, tableExportPrintService,
                performanceReportPrintService, storeVoucherPrintService, assetDepreciationMapPrintService,
                valuedInventoryBookPrintService, sensitivePrintService));
    }

    public PrintController(
            ReceiptPrintService receiptPrintService, InvoicePrintService invoicePrintService,
            OrderPrintService orderPrintService, QuotationPrintService quotationPrintService,
            StockTransferPrintService stockTransferPrintService, PayslipPrintService payslipPrintService,
            CreditNotePrintService creditNotePrintService, DebitNotePrintService debitNotePrintService,
            InventoryReportPrintService inventoryReportPrintService, IvaDeclarationPrintService ivaDeclarationPrintService,
            GuideRemittancePrintService guideRemittancePrintService, DeliveryGuidePrintService deliveryGuidePrintService,
            PayrollFiscalMapPrintService payrollFiscalMapPrintService, ProductLabelPrintService productLabelPrintService,
            InventoryCountSheetPrintService inventoryCountSheetPrintService, POSZReportPrintService posZReportPrintService,
            WorkSheetPrintService workSheetPrintService, EmploymentContractPrintService employmentContractPrintService,
            TerminationPrintService terminationPrintService, TableExportPrintService tableExportPrintService,
            PerformanceReportPrintService performanceReportPrintService, StoreVoucherPrintService storeVoucherPrintService) {
        this(receiptPrintService, invoicePrintService, orderPrintService, quotationPrintService,
                stockTransferPrintService, payslipPrintService, creditNotePrintService, debitNotePrintService,
                inventoryReportPrintService, ivaDeclarationPrintService, guideRemittancePrintService,
                deliveryGuidePrintService, payrollFiscalMapPrintService, productLabelPrintService,
                inventoryCountSheetPrintService, posZReportPrintService, workSheetPrintService,
                employmentContractPrintService, terminationPrintService, tableExportPrintService,
                performanceReportPrintService, storeVoucherPrintService, null, null, null);
    }

    @PostMapping("/table")
    public ResponseEntity<Resource> tableExport(@RequestParam Long companyId, @Valid @RequestBody TableExportRequest request) { return dispatcher.tableExport(companyId, request); }

    @GetMapping("/employment-contract/{contractId}")
    public ResponseEntity<Resource> employmentContract(@PathVariable Long contractId) { return dispatcher.employmentContract(contractId); }

    @GetMapping("/termination-settlement/{terminationId}")
    public ResponseEntity<Resource> terminationSettlement(@PathVariable Long terminationId) { return dispatcher.terminationSettlement(terminationId); }

    @GetMapping("/work-certificate/{terminationId}")
    public ResponseEntity<Resource> workCertificate(@PathVariable Long terminationId) { return dispatcher.workCertificate(terminationId); }

    @GetMapping("/receipt/{invoiceId}")
    public ResponseEntity<Resource> receipt(@PathVariable Long invoiceId) { return dispatcher.receipt(invoiceId); }

    @GetMapping("/pos-z-report/{sessionId}")
    public ResponseEntity<Resource> posZReport(@PathVariable Long sessionId) { return dispatcher.posZReport(sessionId); }

    @GetMapping("/pos-voucher/{voucherId}")
    public ResponseEntity<Resource> posVoucher(@PathVariable Long voucherId) { return dispatcher.posVoucher(voucherId); }

    @GetMapping("/pos-voucher/code/{code}")
    public ResponseEntity<Resource> posVoucherByCode(@PathVariable String code) { return dispatcher.posVoucherByCode(code); }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<Resource> invoice(@PathVariable Long invoiceId) { return dispatcher.invoice(invoiceId); }

    @GetMapping("/invoice/{invoiceId}/thermal")
    public ResponseEntity<Resource> invoiceThermal(@PathVariable Long invoiceId) { return dispatcher.invoiceThermal(invoiceId); }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Resource> order(@PathVariable Long orderId) { return dispatcher.order(orderId); }

    @GetMapping("/quotation/{quotationId}")
    public ResponseEntity<Resource> quotation(@PathVariable Long quotationId) { return dispatcher.quotation(quotationId); }

    @GetMapping("/quotation/{quotationId}/thermal")
    public ResponseEntity<Resource> quotationThermal(@PathVariable Long quotationId) { return dispatcher.quotationThermal(quotationId); }

    @GetMapping("/guide/{invoiceId}")
    public ResponseEntity<Resource> guide(@PathVariable Long invoiceId) { return dispatcher.guide(invoiceId); }

    @GetMapping("/delivery-guide/{guideId}")
    public ResponseEntity<Resource> deliveryGuide(@PathVariable Long guideId) { return dispatcher.deliveryGuide(guideId); }

    @GetMapping("/work-sheet/{workSheetId}")
    public ResponseEntity<Resource> workSheet(@PathVariable Long workSheetId) { return dispatcher.workSheet(workSheetId); }

    @GetMapping("/stock-transfer/{transferId}")
    public ResponseEntity<Resource> stockTransfer(@PathVariable Long transferId) { return dispatcher.stockTransfer(transferId); }

    @GetMapping("/stock-transfer/{transferId}/picking")
    public ResponseEntity<Resource> stockTransferPicking(@PathVariable Long transferId) { return dispatcher.stockTransferPicking(transferId); }

    @GetMapping("/payslip/{payslipId}")
    public ResponseEntity<Resource> payslip(@PathVariable Long payslipId) { return dispatcher.payslip(payslipId); }

    @GetMapping("/credit-note/{id}")
    public ResponseEntity<Resource> creditNote(@PathVariable Long id) { return dispatcher.creditNote(id); }

    @GetMapping("/debit-note/{id}")
    public ResponseEntity<Resource> debitNote(@PathVariable Long id) { return dispatcher.debitNote(id); }

    @GetMapping("/inventory")
    public ResponseEntity<Resource> inventory(@RequestParam Long companyId, @RequestParam(required = false) Long warehouseId) { return dispatcher.inventory(companyId, warehouseId); }

    @GetMapping("/iva-declaration")
    public ResponseEntity<Resource> ivaDeclaration(@RequestParam Long companyId, @RequestParam int year, @RequestParam int month, @RequestParam(required = false, defaultValue = "0") BigDecimal previousCredit) {
        return dispatcher.ivaDeclaration(companyId, year, month, previousCredit);
    }

    @GetMapping("/payroll-fiscal-map")
    public ResponseEntity<Resource> payrollFiscalMap(@RequestParam Long companyId, @RequestParam int year, @RequestParam int month) { return dispatcher.payrollFiscalMap(companyId, year, month); }

    @GetMapping("/product-label")
    public ResponseEntity<Resource> productLabel(@RequestParam Long companyId, @RequestParam List<Long> productIds, @RequestParam(defaultValue = "1") int copies, @RequestParam(required = false) String lotNumber, @RequestParam(required = false) String expiryDate, @RequestParam(defaultValue = "A4") String layout) {
        return dispatcher.productLabel(companyId, productIds, copies, lotNumber, expiryDate, layout);
    }

    @GetMapping("/inventory-count-sheet")
    public ResponseEntity<Resource> inventoryCountSheet(@RequestParam Long companyId, @RequestParam(required = false) Long warehouseId) { return dispatcher.inventoryCountSheet(companyId, warehouseId); }

    @GetMapping("/performance-report")
    public ResponseEntity<Resource> performanceReport(@RequestParam Long companyId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dispatcher.performanceReport(companyId, from, to);
    }

    @PostMapping("/performance-report")
    public ResponseEntity<Resource> performanceReportPost(@RequestParam Long companyId, @RequestBody @Valid PerformanceReportDTO report) { return dispatcher.performanceReportPost(companyId, report); }

    @GetMapping("/asset-depreciation-map")
    public ResponseEntity<Resource> assetDepreciationMap(@RequestParam Long companyId, @RequestParam(required = false) Integer fiscalYear) { return dispatcher.assetDepreciationMap(companyId, fiscalYear); }

    @GetMapping("/valued-inventory-book")
    public ResponseEntity<Resource> valuedInventoryBook(@RequestParam Long companyId, @RequestParam(required = false) Integer fiscalYear) { return dispatcher.valuedInventoryBook(companyId, fiscalYear); }

    @GetMapping("/status/{type}/{documentId}")
    public DocumentPrintStatusDTO printStatus(@PathVariable SensitiveDocumentType type, @PathVariable Long documentId) { return dispatcher.printStatus(type, documentId); }

    @GetMapping("/status/{type}")
    public List<DocumentPrintStatusDTO> printStatuses(@PathVariable SensitiveDocumentType type, @RequestParam List<Long> ids) { return dispatcher.printStatuses(type, ids); }

    @PostMapping("/{type}/{documentId}/reprint")
    public ResponseEntity<Resource> reprint(@PathVariable SensitiveDocumentType type, @PathVariable Long documentId, @Valid @RequestBody ReprintAuthorizationRequest request) {
        return dispatcher.reprint(type, documentId, request);
    }
}
