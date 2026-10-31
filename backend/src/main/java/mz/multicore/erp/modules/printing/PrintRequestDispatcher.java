package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.validation.InputSanitizer;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.performance.dto.PerformanceReportDTO;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Component
public class PrintRequestDispatcher {

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

    @Autowired
    public PrintRequestDispatcher(
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
            @Autowired(required = false) AssetDepreciationMapPrintService assetDepreciationMapPrintService,
            @Autowired(required = false) ValuedInventoryBookPrintService valuedInventoryBookPrintService,
            @Autowired(required = false) SensitiveDocumentPrintService sensitivePrintService) {
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

    public ResponseEntity<Resource> tableExport(Long companyId, TableExportRequest request) {
        return pdfResponse(tableExportPrintService.render(companyId, request), "listagem");
    }

    public ResponseEntity<Resource> employmentContract(Long contractId) {
        return pdfResponse(first(SensitiveDocumentType.EMPLOYMENT_CONTRACT, contractId, () -> employmentContractPrintService.render(contractId)), "contrato-" + contractId);
    }

    public ResponseEntity<Resource> terminationSettlement(Long terminationId) {
        return pdfResponse(first(SensitiveDocumentType.TERMINATION_SETTLEMENT, terminationId, () -> terminationPrintService.renderSettlement(terminationId)), "acerto-final-" + terminationId);
    }

    public ResponseEntity<Resource> workCertificate(Long terminationId) {
        return pdfResponse(first(SensitiveDocumentType.TERMINATION_CERTIFICATE, terminationId, () -> terminationPrintService.renderCertificate(terminationId)), "certificado-trabalho-" + terminationId);
    }

    public ResponseEntity<Resource> receipt(Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.RECEIPT, invoiceId, () -> receiptPrintService.render(invoiceId)), "recibo-" + invoiceId);
    }

    public ResponseEntity<Resource> posZReport(Long sessionId) {
        return pdfResponse(first(SensitiveDocumentType.POS_Z_REPORT, sessionId, () -> posZReportPrintService.render(sessionId)), "fecho-caixa-Z-" + sessionId);
    }

    public ResponseEntity<Resource> posVoucher(Long voucherId) {
        return pdfResponse(first(SensitiveDocumentType.STORE_VOUCHER, voucherId, () -> storeVoucherPrintService.render(voucherId)), "vale-compras-" + voucherId);
    }

    public ResponseEntity<Resource> posVoucherByCode(String code) {
        Long voucherId = storeVoucherPrintService.findIdByCode(code);
        return pdfResponse(first(SensitiveDocumentType.STORE_VOUCHER, voucherId, () -> storeVoucherPrintService.render(voucherId)), "vale-compras-" + code);
    }

    public ResponseEntity<Resource> invoice(Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.INVOICE, invoiceId, () -> invoicePrintService.render(invoiceId)), "fatura-" + invoiceId);
    }

    public ResponseEntity<Resource> invoiceThermal(Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.INVOICE, invoiceId, () -> invoicePrintService.renderThermal(invoiceId)), "fatura-termica-" + invoiceId);
    }

    public ResponseEntity<Resource> order(Long orderId) {
        return pdfResponse(first(SensitiveDocumentType.ORDER, orderId, () -> orderPrintService.render(orderId)), "encomenda-" + orderId);
    }

    public ResponseEntity<Resource> quotation(Long quotationId) {
        return pdfResponse(quotationPrintService.render(quotationId), "cotacao-" + quotationId);
    }

    public ResponseEntity<Resource> quotationThermal(Long quotationId) {
        return pdfResponse(quotationPrintService.renderThermal(quotationId), "cotacao-termica-" + quotationId);
    }

    public ResponseEntity<Resource> guide(Long invoiceId) {
        return pdfResponse(first(SensitiveDocumentType.REMITTANCE_GUIDE, invoiceId, () -> guideRemittancePrintService.render(invoiceId)), "guia-remessa-" + invoiceId);
    }

    public ResponseEntity<Resource> deliveryGuide(Long guideId) {
        return pdfResponse(first(SensitiveDocumentType.DELIVERY_GUIDE, guideId, () -> deliveryGuidePrintService.render(guideId)), "guia-remessa-" + guideId);
    }

    public ResponseEntity<Resource> workSheet(Long workSheetId) {
        return pdfResponse(workSheetPrintService.render(workSheetId), "folha-obra-" + workSheetId);
    }

    public ResponseEntity<Resource> stockTransfer(Long transferId) {
        return pdfResponse(first(SensitiveDocumentType.STOCK_TRANSFER, transferId, () -> stockTransferPrintService.render(transferId)), "transferencia-" + transferId);
    }

    public ResponseEntity<Resource> stockTransferPicking(Long transferId) {
        return pdfResponse(first(SensitiveDocumentType.STOCK_TRANSFER_PICKING, transferId, () -> stockTransferPrintService.renderPickingSheet(transferId)), "separacao-transferencia-" + transferId);
    }

    public ResponseEntity<Resource> payslip(Long payslipId) {
        return pdfResponse(first(SensitiveDocumentType.PAYSLIP, payslipId, () -> payslipPrintService.render(payslipId)), "recibo-salario-" + payslipId);
    }

    public ResponseEntity<Resource> creditNote(Long id) {
        return pdfResponse(first(SensitiveDocumentType.CREDIT_NOTE, id, () -> creditNotePrintService.render(id)), "nota-credito-" + id);
    }

    public ResponseEntity<Resource> debitNote(Long id) {
        return pdfResponse(first(SensitiveDocumentType.DEBIT_NOTE, id, () -> debitNotePrintService.render(id)), "nota-debito-" + id);
    }

    public ResponseEntity<Resource> inventory(Long companyId, Long warehouseId) {
        return pdfResponse(inventoryReportPrintService.render(companyId, warehouseId), "inventario-" + companyId + (warehouseId != null ? "-w" + warehouseId : ""));
    }

    public ResponseEntity<Resource> ivaDeclaration(Long companyId, int year, int month, BigDecimal previousCredit) {
        return pdfResponse(ivaDeclarationPrintService.render(companyId, year, month, previousCredit), "declaracao-iva-" + year + "-" + String.format("%02d", month));
    }

    public ResponseEntity<Resource> payrollFiscalMap(Long companyId, int year, int month) {
        return pdfResponse(payrollFiscalMapPrintService.render(companyId, year, month), "mapa-fiscal-salarial-" + year + "-" + String.format("%02d", month));
    }

    public ResponseEntity<Resource> productLabel(Long companyId, List<Long> productIds, int copies, String lotNumber, String expiryDate, String layout) {
        return pdfResponse(productLabelPrintService.render(companyId, productIds, copies, lotNumber, expiryDate, layout), "etiquetas-" + companyId);
    }

    public ResponseEntity<Resource> inventoryCountSheet(Long companyId, Long warehouseId) {
        return pdfResponse(inventoryCountSheetPrintService.render(companyId, warehouseId), "folha-contagem-" + companyId);
    }

    public ResponseEntity<Resource> performanceReport(Long companyId, LocalDate from, LocalDate to) {
        return pdfResponse(performanceReportPrintService.render(companyId, from, to), "relatorio-desempenho-" + companyId);
    }

    public ResponseEntity<Resource> performanceReportPost(Long companyId, PerformanceReportDTO report) {
        return pdfResponse(performanceReportPrintService.renderReport(companyId, report), "relatorio-desempenho-" + companyId);
    }

    public ResponseEntity<Resource> assetDepreciationMap(Long companyId, Integer fiscalYear) {
        int year = fiscalYear != null ? fiscalYear : LocalDate.now().getYear();
        return pdfResponse(assetDepreciationMapPrintService.render(companyId, year), "mapa-amortizacoes-mod22-" + companyId + "-" + year);
    }

    public ResponseEntity<Resource> valuedInventoryBook(Long companyId, Integer fiscalYear) {
        int year = fiscalYear != null ? fiscalYear : LocalDate.now().getYear();
        return pdfResponse(valuedInventoryBookPrintService.render(companyId, year), "livro-inventario-valorizado-" + companyId + "-" + year);
    }

    public DocumentPrintStatusDTO printStatus(SensitiveDocumentType type, Long documentId) {
        return sensitivePrintService.status(type, documentId);
    }

    public List<DocumentPrintStatusDTO> printStatuses(SensitiveDocumentType type, List<Long> ids) {
        return sensitivePrintService.statuses(type, ids);
    }

    public ResponseEntity<Resource> reprint(SensitiveDocumentType type, Long documentId, ReprintAuthorizationRequest request) {
        byte[] pdf = sensitivePrintService.reprint(type, documentId, request, () -> renderSensitive(type, documentId));
        return pdfResponse(pdf, "reimpressao-" + fileBase(type) + "-" + documentId);
    }

    private byte[] first(SensitiveDocumentType type, Long documentId, Supplier<byte[]> renderer) {
        return sensitivePrintService == null ? renderer.get() : sensitivePrintService.firstPrint(type, documentId, null, renderer);
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
            case ORDER_PICKING -> throw new BusinessRuleException("A reimpressão da guia de separação deve ser autorizada na lista de encomendas.");
        };
    }

    private String fileBase(SensitiveDocumentType type) {
        return type.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public ResponseEntity<Resource> pdfResponse(byte[] bytes, String fileBase) {
        String safeFileBase = InputSanitizer.sanitizeFileName(fileBase);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileBase + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(resource);
    }
}
