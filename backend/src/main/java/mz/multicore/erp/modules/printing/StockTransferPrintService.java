package mz.multicore.erp.modules.printing;

import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.service.ProductBatchLookup;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestrador da geração de documentos PDF de transferências inter-armazém
 * (Guia de Transferência profissional com 3 vias nominais e Guia de Separação de Picking).
 */
@Service
public class StockTransferPrintService {

    public static final List<String> TRANSFER_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Destinatário / Recepção)",
            "2.ª VIA — DUPLICADO (Viatura / Trânsito e Fiscalização)",
            "3.ª VIA — TRIPLICADO (Origem / Arquivo e Expedição)"
    );

    private final StockTransferService stockTransferService;
    private final StockTransferPrintRouteBuilder routeBuilder;
    private final StockTransferPrintLinesTableBuilder linesTableBuilder;
    private final StockTransferPrintSummaryBuilder summaryBuilder;

    public StockTransferPrintService(StockTransferService stockTransferService) {
        this(stockTransferService, (ProductBatchLookup) null);
    }

    public StockTransferPrintService(StockTransferService stockTransferService,
                                     ProductBatchLookup batchLookup) {
        this(stockTransferService,
                new StockTransferPrintRouteBuilder(),
                new StockTransferPrintLinesTableBuilder(batchLookup),
                new StockTransferPrintSummaryBuilder());
    }

    @Autowired
    public StockTransferPrintService(StockTransferService stockTransferService,
                                     StockTransferPrintRouteBuilder routeBuilder,
                                     StockTransferPrintLinesTableBuilder linesTableBuilder,
                                     StockTransferPrintSummaryBuilder summaryBuilder) {
        this.stockTransferService = stockTransferService;
        this.routeBuilder = routeBuilder;
        this.linesTableBuilder = linesTableBuilder;
        this.summaryBuilder = summaryBuilder;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long transferId) {
        StockTransfer transfer = stockTransferService.loadForPrint(transferId);

        return PdfDocumentBuilder.build(PageSize.A4.rotate(), doc -> {
            for (int i = 0; i < TRANSFER_COPIES.size(); i++) {
                if (i > 0) {
                    doc.newPage();
                }
                renderTransferCopy(doc, transfer, TRANSFER_COPIES.get(i));
            }
        });
    }

    private void renderTransferCopy(com.lowagie.text.Document doc, StockTransfer transfer, String copyLabel) {
        doc.add(CompanyHeaderRenderer.build(
                transfer.getCompany(),
                "Guia de Transferência de Armazém",
                "N.º GT " + transfer.getTransferNumber(),
                copyLabel,
                null,
                40f,
                60f
        ));
        doc.add(routeBuilder.buildHorizontalRule());
        doc.add(routeBuilder.buildProfessionalRouteBlock(transfer));
        doc.add(linesTableBuilder.buildProfessionalLinesTable(transfer));
        doc.add(PdfDocumentBuilder.spacer(7f));
        doc.add(summaryBuilder.buildProfessionalSummary(transfer));
        doc.add(PdfDocumentBuilder.spacer(17f));
        doc.add(summaryBuilder.buildProfessionalSignatureBlock());
        Paragraph notice = new Paragraph(
                "O destinatário deve conferir a mercadoria no acto da recepção e comunicar quaisquer divergências de quantidades, lotes, validades ou danos.",
                PdfTheme.smallFont());
        notice.setSpacingBefore(8f);
        doc.add(notice);
    }

    @Transactional
    public byte[] renderPickingSheet(Long transferId) {
        StockTransfer transfer = stockTransferService.loadForPickingPrint(transferId);

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    transfer.getCompany(),
                    "Guia de Separação",
                    transfer.getTransferNumber(),
                    "DOCUMENTO INTERNO DE ARMAZÉM — SEPARAÇÃO E CONFERÊNCIA FÍSICA",
                    null
            ));
            doc.add(routeBuilder.buildPickingRouteBlock(transfer));
            doc.add(linesTableBuilder.buildPickingLinesTable(transfer.getLines()));
            doc.add(PdfDocumentBuilder.spacer(8f));
            Paragraph load = summaryBuilder.buildLoadSummary(transfer.getLines());
            if (load != null) {
                doc.add(load);
            }
            if (transfer.getNotes() != null && !transfer.getNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                Paragraph notes = new Paragraph("Observações: " + transfer.getNotes(), PdfTheme.bodyFont());
                doc.add(notes);
            }
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(summaryBuilder.buildPickingSignatureBlock());
        });
    }

    // Delegados estáticos utilitários retrocompatíveis
    static String formatPackages(Product product, BigDecimal quantity) {
        return StockTransferPrintLinesTableBuilder.formatPackages(product, quantity);
    }

    static String formatBoxes(Product product, BigDecimal quantity) {
        return StockTransferPrintLinesTableBuilder.formatBoxes(product, quantity);
    }

    static String formatBoxPercentage(Product product, BigDecimal quantity) {
        return StockTransferPrintLinesTableBuilder.formatBoxPercentage(product, quantity);
    }

    static String formatRate(BigDecimal rate) {
        return StockTransferPrintLinesTableBuilder.formatRate(rate);
    }

    static String formatQty(BigDecimal qty) {
        return StockTransferPrintLinesTableBuilder.formatQty(qty);
    }
}
