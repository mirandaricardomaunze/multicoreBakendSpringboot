package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for an inter-warehouse stock transfer note (Guia de Transferência). */
@Service
public class StockTransferPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StockTransferService stockTransferService;
    private final mz.multicore.erp.modules.inventory.repository.ProductBatchRepository batchRepository;

    public StockTransferPrintService(StockTransferService stockTransferService) {
        this(stockTransferService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StockTransferPrintService(StockTransferService stockTransferService,
                                     mz.multicore.erp.modules.inventory.repository.ProductBatchRepository batchRepository) {
        this.stockTransferService = stockTransferService;
        this.batchRepository = batchRepository;
    }

    public static final List<String> TRANSFER_COPIES = List.of(
            "1.ª VIA — ORIGINAL (Destinatário / Recepção)",
            "2.ª VIA — DUPLICADO (Viatura / Trânsito e Fiscalização)",
            "3.ª VIA — TRIPLICADO (Origem / Arquivo e Expedição)"
    );

    @Transactional(readOnly = true)
    public byte[] render(Long transferId) {
        StockTransfer transfer = stockTransferService.loadForPrint(transferId);

        return PdfDocumentBuilder.buildA4(doc -> {
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
                "Guia de Transferência",
                transfer.getTransferNumber(),
                copyLabel,
                null
        ));
        doc.add(buildRouteBlock(transfer));
        doc.add(buildLinesTable(transfer));
        doc.add(PdfDocumentBuilder.spacer(6f));
        doc.add(buildTotalsLine(transfer.getLines()));
        Paragraph load = buildLoadSummary(transfer.getLines());
        if (load != null) {
            doc.add(load);
        }
        if (transfer.getNotes() != null && !transfer.getNotes().isBlank()) {
            doc.add(PdfDocumentBuilder.spacer(6f));
            Paragraph notes = new Paragraph("Observações: " + transfer.getNotes(), PdfTheme.bodyFont());
            doc.add(notes);
        }
        doc.add(PdfDocumentBuilder.spacer(20f));
        doc.add(buildSignatureBlock());
    }

    /**
     * Emite a Guia de Separação de Transferência (documento interno de armazém/picking).
     * O conferente/separador usa esta folha para recolha física nas prateleiras e conferência de pesos.
     */
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
            doc.add(buildPickingRouteBlock(transfer));
            doc.add(buildPickingLinesTable(transfer.getLines()));
            doc.add(PdfDocumentBuilder.spacer(8f));
            Paragraph load = buildLoadSummary(transfer.getLines());
            if (load != null) {
                doc.add(load);
            }
            if (transfer.getNotes() != null && !transfer.getNotes().isBlank()) {
                doc.add(PdfDocumentBuilder.spacer(6f));
                Paragraph notes = new Paragraph("Observações: " + transfer.getNotes(), PdfTheme.bodyFont());
                doc.add(notes);
            }
            doc.add(PdfDocumentBuilder.spacer(24f));
            doc.add(buildPickingSignatureBlock());
        });
    }



    private PdfPTable buildRouteBlock(StockTransfer transfer) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell origin = new PdfPCell();
        origin.setBorder(PdfPCell.NO_BORDER);
        origin.addElement(new Paragraph("Armazém de Origem", PdfTheme.subtitleFont()));
        origin.addElement(new Paragraph(transfer.getOriginWarehouse().getName(), PdfTheme.bodyFont()));
        if (transfer.getOriginWarehouse().getLocation() != null) {
            origin.addElement(new Paragraph(transfer.getOriginWarehouse().getLocation(), PdfTheme.smallFont()));
        }
        if (transfer.getResponsible() != null && !transfer.getResponsible().isBlank()) {
            origin.addElement(new Paragraph("Responsável: " + transfer.getResponsible(), PdfTheme.bodyFont()));
        }

        String driver = transfer.getDriverName() != null && !transfer.getDriverName().isBlank()
                ? transfer.getDriverName() : null;
        String plate = transfer.getVehiclePlate() != null && !transfer.getVehiclePlate().isBlank()
                ? transfer.getVehiclePlate() : null;

        if (driver != null) {
            origin.addElement(new Paragraph("Motorista: " + driver, PdfTheme.bodyFont()));
        } else {
            origin.addElement(new Paragraph("Motorista: ____________________________", PdfTheme.bodyFont()));
        }

        if (plate != null) {
            origin.addElement(new Paragraph("Matrícula: " + plate, PdfTheme.bodyFont()));
        } else if (transfer.getVehicle() != null && !transfer.getVehicle().isBlank()) {
            origin.addElement(new Paragraph("Matrícula: " + transfer.getVehicle(), PdfTheme.bodyFont()));
        } else {
            origin.addElement(new Paragraph("Matrícula: ____________________________", PdfTheme.bodyFont()));
        }

        if (transfer.getVehicle() != null && !transfer.getVehicle().isBlank()
                && plate != null && !transfer.getVehicle().equalsIgnoreCase(plate)) {
            origin.addElement(new Paragraph("Veículo: " + transfer.getVehicle(), PdfTheme.bodyFont()));
        }

        java.math.BigDecimal totalWeight = java.math.BigDecimal.ZERO;
        if (transfer.getLines() != null) {
            for (StockTransferLine l : transfer.getLines()) {
                Product p = l.getProduct();
                if (p != null) {
                    BigDecimal w = p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg();
                    if (w != null && l.getQuantity() != null) {
                        totalWeight = totalWeight.add(w.multiply(l.getQuantity()));
                    }
                }
            }
        }
        String weightStr = totalWeight.compareTo(java.math.BigDecimal.ZERO) > 0
                ? totalWeight.setScale(3, RoundingMode.HALF_UP) + " kg"
                : "___________ kg (Aferir na balança)";
        origin.addElement(new Paragraph("Peso da Carga: " + weightStr, PdfTheme.bodyFont()));
        if (totalWeight.compareTo(java.math.BigDecimal.ZERO) > 0) {
            origin.addElement(new Paragraph("Viatura Recomendada: " + mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.recommendedVehicleType(totalWeight), PdfTheme.smallFont()));
        }

        table.addCell(origin);

        PdfPCell destination = new PdfPCell();
        destination.setBorder(PdfPCell.NO_BORDER);
        destination.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph destTitle = new Paragraph("Armazém de Destino", PdfTheme.subtitleFont());
        destTitle.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(destTitle);
        Paragraph destName = new Paragraph(transfer.getDestinationWarehouse().getName(), PdfTheme.bodyFont());
        destName.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(destName);
        if (transfer.getDestinationWarehouse().getLocation() != null) {
            Paragraph loc = new Paragraph(transfer.getDestinationWarehouse().getLocation(), PdfTheme.smallFont());
            loc.setAlignment(Element.ALIGN_RIGHT);
            destination.addElement(loc);
        }
        Paragraph date = new Paragraph("Data: " + transfer.getTransferDate().format(DATE_FMT), PdfTheme.bodyFont());
        date.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(date);
        Paragraph status = new Paragraph("Estado: " + transfer.getStatus().getLabel(), PdfTheme.boldFont());
        status.setAlignment(Element.ALIGN_RIGHT);
        destination.addElement(status);
        table.addCell(destination);

        return table;
    }

    private static final com.lowagie.text.Font TABLE_HEADER_FONT =
            com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8f, PdfTheme.BRAND);
    private static final com.lowagie.text.Font TABLE_BODY_FONT =
            com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8f, PdfTheme.TEXT);

    private PdfPTable buildLinesTable(StockTransfer transfer) {
        return buildLinesTable(transfer.getLines(),
                transfer.getOriginWarehouse() != null ? transfer.getOriginWarehouse().getId() : null);
    }

    private PdfPTable buildLinesTable(List<StockTransferLine> lines) {
        return buildLinesTable(lines, null);
    }

    private PdfPTable buildLinesTable(List<StockTransferLine> lines, Long originWarehouseId) {
        // Colunas obrigatórias da Guia:
        // Referência · Código de Barras · Produto · Quantidade · Embalagem · Caixa · % da Caixa · Valor Unitário · IVA
        PdfPTable table = new PdfPTable(new float[]{12f, 13f, 21f, 6f, 11f, 7f, 10f, 13f, 7f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Cód. Barras", Element.ALIGN_LEFT);
        header(table, "Produto", Element.ALIGN_LEFT);
        header(table, "Qtd", Element.ALIGN_RIGHT);
        header(table, "Embalagem", Element.ALIGN_RIGHT);
        header(table, "Caixa", Element.ALIGN_RIGHT);
        header(table, "% da Caixa", Element.ALIGN_RIGHT);
        header(table, "Valor Unit.", Element.ALIGN_RIGHT);
        header(table, "IVA", Element.ALIGN_RIGHT);

        for (StockTransferLine l : lines) {
            Product p = l.getProduct();
            String ref = (p != null && p.getReference() != null && !p.getReference().isBlank())
                    ? p.getReference()
                    : (p != null && p.getSku() != null ? p.getSku() : "—");
            String barcode = (p != null && p.getBarcode() != null && !p.getBarcode().isBlank())
                    ? p.getBarcode()
                    : "—";
            String productName = p != null && p.getName() != null ? p.getName() : "—";
            String batch = l.getBatchNumber();
            if ((batch == null || batch.isBlank() || "-".equals(batch)) && p != null && originWarehouseId != null) {
                batch = resolveOriginBatch(p.getId(), originWarehouseId);
            }
            if (LineRowMapper.isDisplayableBatch(batch)) {
                productName += "\n[Lote: " + batch + "]";
            }
            BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;
            BigDecimal unitPrice = BigDecimal.ZERO;
            if (p != null) {
                if (p.effectiveUnitPrice(qty) != null && p.effectiveUnitPrice(qty).compareTo(BigDecimal.ZERO) > 0) {
                    unitPrice = p.effectiveUnitPrice(qty);
                } else if (p.getPurchasePrice() != null && p.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                    unitPrice = p.getPurchasePrice();
                }
            }
            BigDecimal taxRate = p != null ? p.effectiveTaxRate() : BigDecimal.ZERO;

            body(table, ref, Element.ALIGN_LEFT);
            body(table, barcode, Element.ALIGN_LEFT);
            body(table, productName, Element.ALIGN_LEFT);
            body(table, formatQty(qty), Element.ALIGN_RIGHT);
            body(table, formatPackages(p, qty), Element.ALIGN_RIGHT);
            body(table, formatBoxes(p, qty), Element.ALIGN_RIGHT);
            body(table, formatBoxPercentage(p, qty), Element.ALIGN_RIGHT);
            body(table, MoneyFormat.formatPlain(unitPrice), Element.ALIGN_RIGHT);
            body(table, formatRate(taxRate), Element.ALIGN_RIGHT);
        }
        return table;
    }

    private String resolveOriginBatch(Long productId, Long warehouseId) {
        if (batchRepository == null || productId == null || warehouseId == null) return null;
        try {
            var batches = batchRepository.findAvailableFEFO(productId, warehouseId);
            if (batches != null && !batches.isEmpty()) {
                var b = batches.get(0);
                String exp = b.getExpirationDate() != null
                        ? " | Val: " + b.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "";
                return b.getBatchNumber() + exp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private PdfPTable buildTotalsLine(List<StockTransferLine> lines) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (StockTransferLine line : lines) {
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);
            Product p = line.getProduct();
            if (p != null) {
                BigDecimal unitPrice = (p.effectiveUnitPrice(qty) != null && p.effectiveUnitPrice(qty).compareTo(BigDecimal.ZERO) > 0)
                        ? p.effectiveUnitPrice(qty)
                        : (p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO);
                BigDecimal lineNet = unitPrice.multiply(qty);
                totalNet = totalNet.add(lineNet);
                BigDecimal taxRate = p.effectiveTaxRate();
                if (taxRate != null) {
                    totalTax = totalTax.add(lineNet.multiply(taxRate));
                }
            }
        }
        totalNet = totalNet.setScale(2, RoundingMode.HALF_UP);
        totalTax = totalTax.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGross = totalNet.add(totalTax);

        PdfPTable wrapper = new PdfPTable(new float[]{45f, 55f});
        wrapper.setWidthPercentage(100);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(empty);

        PdfPTable inner = new PdfPTable(new float[]{55f, 45f});
        addTotalsLine(inner, "Linhas", String.valueOf(lines.size()), false);
        addTotalsLine(inner, "Qtd. Total", formatQty(totalQty), false);
        addTotalsLine(inner, "Total Mercadoria (Líquido)", MoneyFormat.format(totalNet), false);
        addTotalsLine(inner, "Total IVA", MoneyFormat.format(totalTax), false);
        addTotalsLine(inner, "Total Geral", MoneyFormat.format(totalGross), true);

        PdfPCell innerWrap = new PdfPCell(inner);
        innerWrap.setBorder(PdfPCell.NO_BORDER);
        wrapper.addCell(innerWrap);
        return wrapper;
    }

    /**
     * Carga da transferência, pela mesma conta da guia de remessa ao cliente
     * ({@link LoadSummaryRenderer}). {@code null} quando nenhum artigo tem peso no cadastro —
     * imprimir "0,000 kg" diria que a carrinha vai vazia.
     */
    private Paragraph buildLoadSummary(List<StockTransferLine> lines) {
        if (lines == null || lines.isEmpty()) return null;
        return LoadSummaryRenderer.build(lines.stream()
                .map(line -> {
                    Product p = line.getProduct();
                    BigDecimal weight = p != null
                            ? (p.getGrossUnitWeightKg() != null ? p.getGrossUnitWeightKg() : p.getNetUnitWeightKg())
                            : null;
                    int unitsPerBox = p != null ? p.getUnitsPerBox() : 1;
                    String name = p != null ? p.getName() : "Artigo";
                    return new LoadSummaryRenderer.Item(name, line.getQuantity(), weight, unitsPerBox);
                })
                .toList());
    }

    private PdfPTable buildSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        table.addCell(signatureCell("Entregue por (Origem)"));
        table.addCell(signatureCell("Recebido por (Destino)"));
        return table;
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingTop(20f);
        cell.setPaddingLeft(12f);
        cell.setPaddingRight(12f);

        Paragraph line = new Paragraph("____________________________", PdfTheme.bodyFont());
        Paragraph caption = new Paragraph(label, PdfTheme.smallFont());
        cell.addElement(line);
        cell.addElement(caption);
        return cell;
    }

    private void addTotalsLine(PdfPTable t, String label, String value, boolean emphasised) {
        PdfPCell l = new PdfPCell(new Phrase(label, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        PdfPCell v = new PdfPCell(new Phrase(value, emphasised ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        l.setBorder(PdfPCell.NO_BORDER);
        v.setBorder(PdfPCell.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(3f);
        v.setPadding(3f);
        if (emphasised) {
            l.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
            v.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        }
        t.addCell(l);
        t.addCell(v);
    }

    private void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, TABLE_BODY_FONT));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    static String formatPackages(Product product, BigDecimal quantity) {
        if (!hasPackaging(product)) return "—";
        BigDecimal safeQty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal packages = safeQty.divide(BigDecimal.valueOf(product.getUnitsPerPackage()), 3, RoundingMode.HALF_UP);
        return packages.stripTrailingZeros().toPlainString();
    }

    static String formatBoxes(Product product, BigDecimal quantity) {
        if (!hasPackaging(product)) return "—";
        BigDecimal safeQty = quantity == null ? BigDecimal.ZERO : quantity;
        long unitsPerBox = (long) product.getPackagesPerBox() * product.getUnitsPerPackage();
        BigDecimal boxes = safeQty.divide(BigDecimal.valueOf(unitsPerBox), 3, RoundingMode.HALF_UP);
        return boxes.stripTrailingZeros().toPlainString();
    }

    static String formatBoxPercentage(Product product, BigDecimal quantity) {
        if (!hasPackaging(product)) return "—";
        BigDecimal safeQty = quantity == null ? BigDecimal.ZERO : quantity;
        long unitsPerBox = (long) product.getPackagesPerBox() * product.getUnitsPerPackage();
        BigDecimal percentage = safeQty.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
        return percentage.stripTrailingZeros().toPlainString() + "%";
    }

    private static boolean hasPackaging(Product product) {
        return product != null && product.getPackagesPerBox() > 0 && product.getUnitsPerPackage() > 0;
    }

    static String formatRate(BigDecimal rate) {
        if (rate == null) return "0%";
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100));
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    static String formatQty(BigDecimal qty) {
        if (qty == null || BigDecimal.ZERO.compareTo(qty) == 0) return "0";
        return qty.stripTrailingZeros().toPlainString();
    }

    private PdfPTable buildPickingRouteBlock(StockTransfer transfer) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}
        table.setSpacingAfter(10f);

        PdfPCell origin = new PdfPCell();
        origin.setBorder(PdfPCell.NO_BORDER);
        origin.addElement(new Paragraph("Local de Recolha (Origem)", PdfTheme.subtitleFont()));
        origin.addElement(new Paragraph(transfer.getOriginWarehouse().getName(), PdfTheme.boldFont()));
        if (transfer.getOriginWarehouse().getLocation() != null) {
            origin.addElement(new Paragraph(transfer.getOriginWarehouse().getLocation(), PdfTheme.smallFont()));
        }
        if (transfer.getDriverName() != null && !transfer.getDriverName().isBlank()) {
            origin.addElement(new Paragraph("Motorista Previsto: " + transfer.getDriverName(), PdfTheme.bodyFont()));
        }
        if (transfer.getVehiclePlate() != null && !transfer.getVehiclePlate().isBlank()) {
            origin.addElement(new Paragraph("Viatura Prevista: " + transfer.getVehiclePlate(), PdfTheme.bodyFont()));
        }
        table.addCell(origin);

        PdfPCell dest = new PdfPCell();
        dest.setBorder(PdfPCell.NO_BORDER);
        dest.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph destTitle = new Paragraph("Armazém de Destino", PdfTheme.subtitleFont());
        destTitle.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(destTitle);
        Paragraph destName = new Paragraph(transfer.getDestinationWarehouse().getName(), PdfTheme.boldFont());
        destName.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(destName);
        Paragraph date = new Paragraph("Data Emissão: " + transfer.getTransferDate().format(DATE_FMT), PdfTheme.bodyFont());
        date.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(date);
        Paragraph status = new Paragraph("Estado: " + transfer.getStatus().getLabel(), PdfTheme.bodyFont());
        status.setAlignment(Element.ALIGN_RIGHT);
        dest.addElement(status);
        table.addCell(dest);

        return table;
    }

    private PdfPTable buildPickingLinesTable(List<StockTransferLine> lines) {
        // Tabela de separação focada no trabalho operacional de armazém:
        // Referência · Cód. Barras · Artigo · Qtd a Separar · Embalagem · Caixa · Conferido
        PdfPTable table = new PdfPTable(new float[]{14f, 15f, 33f, 10f, 10f, 8f, 10f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(8f);

        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Cód. Barras", Element.ALIGN_LEFT);
        header(table, "Produto / Artigo", Element.ALIGN_LEFT);
        header(table, "Qtd", Element.ALIGN_RIGHT);
        header(table, "Embalagem", Element.ALIGN_RIGHT);
        header(table, "Caixa", Element.ALIGN_RIGHT);
        header(table, "Conferência", Element.ALIGN_CENTER);

        for (StockTransferLine l : lines) {
            Product p = l.getProduct();
            String ref = (p != null && p.getReference() != null && !p.getReference().isBlank())
                    ? p.getReference() : (p != null && p.getSku() != null ? p.getSku() : "—");
            String barcode = (p != null && p.getBarcode() != null && !p.getBarcode().isBlank())
                    ? p.getBarcode() : "—";
            String productName = p != null && p.getName() != null ? p.getName() : "—";
            BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;

            body(table, ref, Element.ALIGN_LEFT);
            body(table, barcode, Element.ALIGN_LEFT);
            body(table, productName, Element.ALIGN_LEFT);
            body(table, formatQty(qty), Element.ALIGN_RIGHT);
            body(table, formatPackages(p, qty), Element.ALIGN_RIGHT);
            body(table, formatBoxes(p, qty), Element.ALIGN_RIGHT);
            body(table, "[    ]", Element.ALIGN_CENTER);
        }
        return table;
    }

    private PdfPTable buildPickingSignatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try { table.setWidths(new float[]{50f, 50f}); } catch (Exception ignored) {}

        table.addCell(signatureCell("Separado por (Operador de Armazém)"));
        table.addCell(signatureCell("Conferido por (Encarregado / Gestor)"));
        return table;
    }
}

