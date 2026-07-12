package com.phcpro.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Folha de <b>etiquetas de produto</b> (código de barras + nome + preço) em A4, grelha de 3 colunas.
 * O código de barras é gerado como <b>imagem AWT</b> (Code128, universal) e embebido — não precisa do
 * {@code PdfWriter}. Uma responsabilidade: produtos → folha de etiquetas. Ver
 * {@code docs/ETIQUETAS_CODIGO_BARRAS_SPEC.md}.
 */
@Service
public class ProductLabelPrintService {

    private static final int COLUMNS = 3;
    private static final int MAX_COPIES = 200;

    private final ComercialService comercialService;
    private final CompanyService companyService;

    public ProductLabelPrintService(ComercialService comercialService, CompanyService companyService) {
        this.comercialService = comercialService;
        this.companyService = companyService;
    }

    /**
     * @param productIds artigos a etiquetar (da empresa activa)
     * @param copies     nº de etiquetas por artigo (limitado a 1..200)
     */
    @Transactional(readOnly = true)
    public byte[] render(Long companyId, List<Long> productIds, int copies) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessRuleException("Selecione pelo menos um produto.");
        }
        int copiesClamped = Math.max(1, Math.min(copies, MAX_COPIES));
        Set<Long> idSet = new HashSet<>(productIds);
        List<ProductDTO> products = comercialService.getAllProducts().stream()
                .filter(p -> idSet.contains(p.id()))
                .toList();
        if (products.isEmpty()) {
            throw new BusinessRuleException("Nenhum dos produtos indicados foi encontrado.");
        }

        String stamp = "ETQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(company, "Etiquetas de Produtos", stamp));
            PdfPTable grid = new PdfPTable(COLUMNS);
            grid.setWidthPercentage(100);
            grid.setSpacingBefore(8f);

            int cells = 0;
            for (ProductDTO p : products) {
                for (int c = 0; c < copiesClamped; c++) {
                    grid.addCell(labelCell(p));
                    cells++;
                }
            }
            while (cells % COLUMNS != 0) { // completa a última linha
                grid.addCell(emptyCell());
                cells++;
            }
            doc.add(grid);
        });
    }

    private PdfPCell labelCell(ProductDTO product) {
        String code = codeOf(product);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(8f);
        cell.setMinimumHeight(80f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph name = new Paragraph(truncate(product.name(), 30), PdfTheme.tableHeaderFont());
        name.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(name);

        Image barcode = barcodeImage(code);
        if (barcode != null) {
            barcode.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(barcode);
        }

        Paragraph codeText = new Paragraph(code, PdfTheme.smallFont());
        codeText.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(codeText);

        Paragraph price = new Paragraph(
                product.unitPrice() == null ? "" : String.format("%,.2f MT", product.unitPrice()),
                PdfTheme.subtitleFont());
        price.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(price);
        return cell;
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setMinimumHeight(80f);
        return cell;
    }

    /** Código a imprimir: barcode → referência → SKU (sempre existe SKU). */
    private static String codeOf(ProductDTO p) {
        if (p.barcode() != null && !p.barcode().isBlank()) return p.barcode().trim();
        if (p.reference() != null && !p.reference().isBlank()) return p.reference().trim();
        return p.sku();
    }

    /** Code128 como imagem embebível; null se o código for inválido para código de barras. */
    private static Image barcodeImage(String code) {
        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(code);
            barcode.setCodeType(Barcode128.CODE128);
            barcode.setBarHeight(30f);
            barcode.setX(0.8f);
            barcode.setFont(null); // sem o texto do OpenPDF — mostramos o código em separado
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
