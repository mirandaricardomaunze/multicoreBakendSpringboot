package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.company.model.Company;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ProductLabelDocumentRenderer {

    private static final int COLUMNS = 3;

    public byte[] renderLabels(Company company, List<ProductDTO> products, int copiesClamped,
                               String lotNumber, String expiryDate, String layoutFormat) {
        String stamp = "ETQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        boolean isThermal = layoutFormat != null && layoutFormat.toUpperCase().contains("THERMAL");

        if (isThermal) {
            return PdfDocumentBuilder.buildReceipt(doc -> {
                doc.add(CompanyHeaderRenderer.build(company, "Etiquetas de Lote / Produto", stamp));
                PdfPTable grid = new PdfPTable(1);
                grid.setWidthPercentage(100);
                grid.setSpacingBefore(6f);
                for (ProductDTO p : products) {
                    for (int c = 0; c < copiesClamped; c++) {
                        grid.addCell(labelCell(p, lotNumber, expiryDate));
                    }
                }
                doc.add(grid);
            });
        }

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Etiquetas de Produtos e Lotes", stamp));
            PdfPTable grid = new PdfPTable(COLUMNS);
            grid.setWidthPercentage(100);
            grid.setSpacingBefore(8f);

            int cells = 0;
            for (ProductDTO p : products) {
                for (int c = 0; c < copiesClamped; c++) {
                    grid.addCell(labelCell(p, lotNumber, expiryDate));
                    cells++;
                }
            }
            while (cells % COLUMNS != 0) {
                grid.addCell(emptyCell());
                cells++;
            }
            doc.add(grid);
        });
    }

    private PdfPCell labelCell(ProductDTO product, String lotNumber, String expiryDate) {
        String code = codeOf(product);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setMinimumHeight(80f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph name = new Paragraph(truncate(product.name(), 30), PdfTheme.tableHeaderFont());
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingAfter(6f);
        cell.addElement(name);

        Image barcode = barcodeImage(code);
        if (barcode != null) {
            barcode.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(barcode);
        }

        Paragraph codeText = new Paragraph(code, PdfTheme.smallFont());
        codeText.setAlignment(Element.ALIGN_CENTER);
        codeText.setSpacingBefore(2f);
        cell.addElement(codeText);

        if (lotNumber != null && !lotNumber.isBlank()) {
            Paragraph lot = new Paragraph("Lote/OP: " + lotNumber.trim(), PdfTheme.smallFont());
            lot.setAlignment(Element.ALIGN_CENTER);
            lot.setSpacingBefore(2f);
            cell.addElement(lot);
        }

        if (expiryDate != null && !expiryDate.isBlank()) {
            Paragraph exp = new Paragraph("Val: " + expiryDate.trim(), PdfTheme.smallFont());
            exp.setAlignment(Element.ALIGN_CENTER);
            exp.setSpacingBefore(1f);
            cell.addElement(exp);
        }

        Paragraph price = new Paragraph(
                product.unitPrice() == null ? "" : String.format("%,.2f MT", product.unitPrice()),
                PdfTheme.subtitleFont());
        price.setAlignment(Element.ALIGN_CENTER);
        price.setSpacingBefore(4f);
        cell.addElement(price);
        return cell;
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setMinimumHeight(80f);
        return cell;
    }

    private static String codeOf(ProductDTO p) {
        if (p.barcode() != null && !p.barcode().isBlank()) return p.barcode().trim();
        if (p.reference() != null && !p.reference().isBlank()) return p.reference().trim();
        return p.sku();
    }

    private static Image barcodeImage(String code) {
        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(code);
            barcode.setCodeType(Barcode128.CODE128);
            barcode.setBarHeight(30f);
            barcode.setX(0.8f);
            barcode.setFont(null);
            java.awt.Image awt = barcode.createAwtImage(Color.BLACK, Color.WHITE);
            Image img = Image.getInstance(awt, Color.WHITE);
            img.scaleToFit(140f, 44f);
            return img;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
