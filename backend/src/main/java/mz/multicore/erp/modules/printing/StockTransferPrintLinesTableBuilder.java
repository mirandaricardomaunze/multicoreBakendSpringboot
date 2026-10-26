package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.inventory.repository.ProductBatchRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Construtor das tabelas de artigos transferidos e de separação física (picking) em PDF.
 */
@Component
public class StockTransferPrintLinesTableBuilder {

    private static final Font TABLE_HEADER_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, PdfTheme.BRAND);
    private static final Font TABLE_BODY_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 8f, PdfTheme.TEXT);

    private final ProductBatchRepository batchRepository;

    public StockTransferPrintLinesTableBuilder() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StockTransferPrintLinesTableBuilder(ProductBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public PdfPTable buildProfessionalLinesTable(StockTransfer transfer) {
        PdfPTable table = new PdfPTable(new float[]{8f, 11f, 19f, 6f, 6f, 7f, 9f, 8f, 7f, 9f, 10f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        header(table, "Referência", Element.ALIGN_LEFT);
        header(table, "Código de barras", Element.ALIGN_LEFT);
        header(table, "Designação", Element.ALIGN_LEFT);
        header(table, "Caixas", Element.ALIGN_RIGHT);
        header(table, "Emb.", Element.ALIGN_RIGHT);
        header(table, "Quantidade", Element.ALIGN_RIGHT);
        header(table, "Lote", Element.ALIGN_LEFT);
        header(table, "Validade", Element.ALIGN_CENTER);
        header(table, "Peso total (kg)", Element.ALIGN_RIGHT);
        header(table, "Preço unit.", Element.ALIGN_RIGHT);
        header(table, "Total", Element.ALIGN_RIGHT);

        Long warehouseId = transfer.getOriginWarehouse() == null ? null : transfer.getOriginWarehouse().getId();
        if (transfer.getLines() != null) {
            for (StockTransferLine line : transfer.getLines()) {
                Product product = line.getProduct();
                BigDecimal quantity = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
                BatchInfo batch = resolveBatchInfo(line, warehouseId);
                BigDecimal unitPrice = effectiveUnitPrice(product, quantity);

                body(table, productValue(product == null ? null : product.getReference(), product == null ? null : product.getSku()), Element.ALIGN_LEFT);
                body(table, productValue(product == null ? null : product.getBarcode(), null), Element.ALIGN_LEFT);
                body(table, productValue(product == null ? null : product.getName(), null), Element.ALIGN_LEFT);
                body(table, formatBoxes(product, quantity), Element.ALIGN_RIGHT);
                body(table, formatPackages(product, quantity), Element.ALIGN_RIGHT);
                body(table, formatQty(quantity), Element.ALIGN_RIGHT);
                body(table, batch.number(), Element.ALIGN_LEFT);
                body(table, batch.expiration(), Element.ALIGN_CENTER);
                body(table, formatWeight(product, quantity), Element.ALIGN_RIGHT);
                body(table, MoneyFormat.formatPlain(unitPrice), Element.ALIGN_RIGHT);
                body(table, MoneyFormat.formatPlain(unitPrice.multiply(quantity)), Element.ALIGN_RIGHT);
            }
        }
        return table;
    }

    public PdfPTable buildPickingLinesTable(List<StockTransferLine> lines) {
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

        if (lines != null) {
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
        }
        return table;
    }

    public BatchInfo resolveBatchInfo(StockTransferLine line, Long warehouseId) {
        if (line.getBatchNumber() != null && !line.getBatchNumber().isBlank() && !"-".equals(line.getBatchNumber())) {
            return new BatchInfo(line.getBatchNumber(), "-");
        }
        Product product = line.getProduct();
        if (batchRepository != null && product != null && warehouseId != null) {
            try {
                var batches = batchRepository.findAvailableFEFO(product.getId(), warehouseId);
                if (batches != null && !batches.isEmpty()) {
                    var batch = batches.get(0);
                    String expiration = batch.getExpirationDate() == null ? "-"
                            : batch.getExpirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    return new BatchInfo(productValue(batch.getBatchNumber(), null), expiration);
                }
            } catch (Exception ignored) {}
        }
        return new BatchInfo("-", "-");
    }

    public static BigDecimal effectiveUnitPrice(Product product, BigDecimal quantity) {
        if (product == null) return BigDecimal.ZERO;
        BigDecimal price = product.effectiveUnitPrice(quantity);
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) return price;
        return product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO;
    }

    public static String formatWeight(Product product, BigDecimal quantity) {
        BigDecimal weight = unitWeight(product);
        return weight == null ? "-" : formatQty(weight.multiply(quantity).setScale(3, RoundingMode.HALF_UP));
    }

    public static BigDecimal unitWeight(Product product) {
        if (product == null) return null;
        return product.getGrossUnitWeightKg() != null ? product.getGrossUnitWeightKg() : product.getNetUnitWeightKg();
    }

    public static String formatPackages(Product product, BigDecimal quantity) {
        if (!hasPackaging(product)) return "—";
        BigDecimal safeQty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal packages = safeQty.divide(BigDecimal.valueOf(product.getUnitsPerPackage()), 3, RoundingMode.HALF_UP);
        return packages.stripTrailingZeros().toPlainString();
    }

    public static String formatBoxes(Product product, BigDecimal quantity) {
        if (!hasPackaging(product)) return "—";
        BigDecimal safeQty = quantity == null ? BigDecimal.ZERO : quantity;
        long unitsPerBox = (long) product.getPackagesPerBox() * product.getUnitsPerPackage();
        BigDecimal boxes = safeQty.divide(BigDecimal.valueOf(unitsPerBox), 3, RoundingMode.HALF_UP);
        return boxes.stripTrailingZeros().toPlainString();
    }

    public static String formatBoxPercentage(Product product, BigDecimal quantity) {
        if (!hasPackaging(product)) return "—";
        BigDecimal safeQty = quantity == null ? BigDecimal.ZERO : quantity;
        long unitsPerBox = (long) product.getPackagesPerBox() * product.getUnitsPerPackage();
        BigDecimal percentage = safeQty.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
        return percentage.stripTrailingZeros().toPlainString() + "%";
    }

    public static boolean hasPackaging(Product product) {
        return product != null && product.getPackagesPerBox() > 0 && product.getUnitsPerPackage() > 0;
    }

    public static String formatRate(BigDecimal rate) {
        if (rate == null) return "0%";
        BigDecimal percent = rate.multiply(BigDecimal.valueOf(100));
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    public static String formatQty(BigDecimal qty) {
        if (qty == null || BigDecimal.ZERO.compareTo(qty) == 0) return "0";
        return qty.stripTrailingZeros().toPlainString();
    }

    private static String productValue(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : (fallback != null && !fallback.isBlank() ? fallback : "-");
    }

    private static void header(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3.5f);
        table.addCell(cell);
    }

    private static void body(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, TABLE_BODY_FONT));
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    public record BatchInfo(String number, String expiration) {}
}
