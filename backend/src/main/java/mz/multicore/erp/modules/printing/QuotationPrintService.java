package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF da Cotação. Reutiliza o cabeçalho da empresa, o bloco do cliente, a tabela de linhas e o
 * bloco de totais partilhados, pelo que sai com o mesmo desenho da fatura e da encomenda A4 <b>por
 * construção</b> — não por coincidência de manutenção (foi assim que o bloco do cliente acabou
 * escrito duas vezes, ver {@link ClientBlockRenderer}).
 *
 * <p>Acrescenta o que uma proposta tem e uma fatura não: validade em destaque, condições
 * comerciais, espaço para a aceitação do cliente e a nota de que não é documento fiscal.
 *
 * <p><b>Não imprime o armazém.</b> A cotação grava-o porque a conversão em encomenda precisa dele,
 * mas é informação interna e este documento sai para o cliente — daí o {@code null} passado ao
 * {@link ClientBlockRenderer}.
 */
@Service
public class QuotationPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuotationRepository quotationRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public QuotationPrintService(QuotationRepository quotationRepository, LineRowMapper lineRowMapper,
                                 DocumentConfigService documentConfigService) {
        this.quotationRepository = quotationRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long quotationId) {
        Quotation quotation = quotationRepository
                .findByIdWithLinesAndCompanyId(quotationId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cotação não encontrada."));

        CurrentUserContext.requireCompany(quotation.getCompany().getId());
        List<LineItemsTableRenderer.Row> rows = toRows(quotation.getLines());

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(buildHeaderTable(quotation.getCompany(), quotation));
            doc.add(buildClientAndMetaTable(quotation));

            PdfPTable validityBlock = buildValidityBlock(quotation);
            if (validityBlock != null) {
                doc.add(validityBlock);
            }

            // Tabela corporativa canónica com coluna de Código de Barras
            doc.add(LineItemsTableRenderer.buildModelTable(rows));

            // Bloco de Totais
            java.math.BigDecimal totalDiscount = calculateTotalDiscount(quotation.getLines());
            doc.add(TotalsBlockRenderer.buildModelTotals(
                    quotation.getTotalBeforeTax(),
                    totalDiscount,
                    quotation.getTaxAmount(),
                    quotation.getTotalAmount(),
                    TotalsBlockRenderer.calculateBreakdowns(rows)
            ));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // Bloco partilhado de condições comerciais
            PdfPTable terms = CommercialTermsRenderer.build(
                    quotation.getPaymentTerms(), quotation.getDeliveryTerms(), null, quotation.getNotes());
            if (terms != null) {
                doc.add(terms);
            }

            // Rodapé configurado da empresa
            var columns = documentConfigService.getColumns(quotation.getCompany().getId(), DocumentType.COMMERCIAL);
            if (columns != null && columns.footer() != null && !columns.footer().isBlank()) {
                Paragraph foot = new Paragraph(columns.footer().trim(), PdfTheme.smallFont());
                foot.setAlignment(Element.ALIGN_CENTER);
                foot.setSpacingBefore(6f);
                doc.add(foot);
            }

            doc.add(PdfDocumentBuilder.spacer(14f));
            doc.add(SignatureBlockRenderer.build(
                    "Pela empresa", "Aceite pelo cliente (nome, assinatura e data)"));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(disclaimer());
        });
    }

    private static PdfPTable buildHeaderTable(mz.multicore.erp.modules.company.model.Company company, Quotation quotation) {
        PdfPTable table = new PdfPTable(new float[]{55f, 45f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);

        // Coluna da Empresa (Esquerda)
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(PdfPCell.NO_BORDER);

        if (company != null && company.getLogo() != null && company.getLogo().length > 0) {
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(company.getLogo());
                logo.scaleToFit(140f, 45f);
                logo.setSpacingAfter(4f);
                companyCell.addElement(logo);
            } catch (Exception ignored) {}
        }

        String compName = (company != null && company.getName() != null) ? company.getName().toUpperCase() : "EMPRESA";
        Paragraph nameP = new Paragraph(compName, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 13f, PdfTheme.BRAND));
        nameP.setSpacingAfter(2f);
        companyCell.addElement(nameP);

        if (company != null) {
            if (company.getAddress() != null && !company.getAddress().isBlank()) {
                companyCell.addElement(new Paragraph(company.getAddress().trim(), PdfTheme.bodyFont()));
            }
            String taxAndPhone = "NUIT: " + (company.getTaxId() != null ? company.getTaxId() : "—");
            if (company.getPhone() != null && !company.getPhone().isBlank()) {
                taxAndPhone += " · Tel.: " + company.getPhone().trim();
            }
            companyCell.addElement(new Paragraph(taxAndPhone, PdfTheme.bodyFont()));

            if (company.getEmail() != null && !company.getEmail().isBlank()) {
                companyCell.addElement(new Paragraph("E-mail: " + company.getEmail().trim(), PdfTheme.bodyFont()));
            }
        }
        table.addCell(companyCell);

        // Coluna do Documento (Direita)
        PdfPCell docCell = new PdfPCell();
        docCell.setBorder(PdfPCell.NO_BORDER);
        docCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph docTitle = new Paragraph("COTAÇÃO", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 22f, PdfTheme.BRAND));
        docTitle.setAlignment(Element.ALIGN_RIGHT);
        docCell.addElement(docTitle);

        String num = "N.º " + (quotation.getQuotationNumber() != null ? quotation.getQuotationNumber() : "—");
        Paragraph numP = new Paragraph(num, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11f, PdfTheme.BRAND));
        numP.setAlignment(Element.ALIGN_RIGHT);
        numP.setSpacingAfter(2f);
        docCell.addElement(numP);

        String statusStr = "Estado: " + (quotation.getStatus() != null ? quotation.getStatus().getLabel() : "—");
        Paragraph statusP = new Paragraph(statusStr, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8.5f, PdfTheme.MUTED));
        statusP.setAlignment(Element.ALIGN_RIGHT);
        docCell.addElement(statusP);

        table.addCell(docCell);
        return table;
    }

    private static PdfPTable buildClientAndMetaTable(Quotation quotation) {
        PdfPTable table = new PdfPTable(new float[]{55f, 45f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);

        // CLIENTE / DESTINATÁRIO (Esquerda)
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(PdfPCell.NO_BORDER);

        Paragraph clienteLabel = new Paragraph("PROPOSTA PARA", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED));
        clienteLabel.setSpacingAfter(2f);
        clientCell.addElement(clienteLabel);

        mz.multicore.erp.modules.comercial.model.Client client = quotation.getClient();
        String buyerName = quotation.getWalkInName() != null && !quotation.getWalkInName().isBlank()
                ? quotation.getWalkInName().trim()
                : (client != null ? client.getName() : "Consumidor Final");
        Paragraph buyerP = new Paragraph(buyerName, PdfTheme.boldFont());
        buyerP.setSpacingAfter(2f);
        clientCell.addElement(buyerP);

        if (client != null && client.getAddress() != null && !client.getAddress().isBlank()
                && !"-".equals(client.getAddress()) && !"—".equals(client.getAddress()) && !"n/a".equalsIgnoreCase(client.getAddress())) {
            clientCell.addElement(new Paragraph(client.getAddress().trim(), PdfTheme.bodyFont()));
        }
        String clientTaxId = (client != null && client.getTaxId() != null && !client.getTaxId().isBlank())
                ? client.getTaxId().trim()
                : "Consumidor Final";
        clientCell.addElement(new Paragraph("NUIT: " + clientTaxId, PdfTheme.bodyFont()));

        table.addCell(clientCell);

        // Metadados (Direita - Grelha 2x2)
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(PdfPCell.NO_BORDER);

        PdfPTable grid = new PdfPTable(new float[]{50f, 50f});
        grid.setWidthPercentage(100);

        String issueDateStr = quotation.getQuotationDate() != null ? quotation.getQuotationDate().format(DATE_FMT) : "—";
        String validStr = quotation.getValidUntil() != null ? quotation.getValidUntil().format(DATE_FMT) : "—";

        String condPag = quotation.getPaymentTerms() != null && !quotation.getPaymentTerms().isBlank()
                ? quotation.getPaymentTerms().trim()
                : (client != null && client.effectivePaymentTermsDays() > 0 ? client.effectivePaymentTermsDays() + " dias" : "Pronto Pagamento");
        String refStr = quotation.getQuotationNumber() != null ? quotation.getQuotationNumber() : "—";

        addMetaBox(grid, "DATA DA PROPOSTA", issueDateStr);
        addMetaBox(grid, "VALIDADE", validStr);
        addMetaBox(grid, "COND. PAGAMENTO", condPag);
        addMetaBox(grid, "REFERÊNCIA", refStr);

        metaCell.addElement(grid);
        table.addCell(metaCell);
        return table;
    }

    private static void addMetaBox(PdfPTable grid, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(6f);
        cell.addElement(new Paragraph(label, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 7.5f, PdfTheme.MUTED)));
        cell.addElement(new Paragraph(value, PdfTheme.bodyFont()));
        grid.addCell(cell);
    }

    private static java.math.BigDecimal calculateTotalDiscount(List<QuotationLine> lines) {
        java.math.BigDecimal totalDiscount = java.math.BigDecimal.ZERO;
        if (lines != null) {
            for (QuotationLine line : lines) {
                if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0
                        && line.getUnitPrice() != null && line.getQuantity() != null) {
                    java.math.BigDecimal lineGross = line.getUnitPrice().multiply(line.getQuantity());
                    java.math.BigDecimal disc = lineGross.multiply(line.getDiscountPercentage().divide(java.math.BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
                    totalDiscount = totalDiscount.add(disc);
                }
            }
        }
        return totalDiscount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public byte[] renderThermal(Long quotationId) {
        Quotation quotation = quotationRepository
                .findByIdWithLinesAndCompanyId(quotationId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cotação não encontrada."));

        return PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, quotation.getCompany());

            ThermalReceiptRenderer.centered(doc, "COTAÇÃO COMERCIAL (80mm)", PdfTheme.boldFont());
            ThermalReceiptRenderer.centered(doc, quotation.getQuotationNumber(), PdfTheme.bodyFont());
            if (quotation.getQuotationDate() != null) {
                ThermalReceiptRenderer.centered(doc, "Data: " + quotation.getQuotationDate().format(DATE_FMT), PdfTheme.smallFont());
            }
            if (quotation.getValidUntil() != null) {
                boolean expired = quotation.getValidUntil().isBefore(LocalDate.now());
                String validityText = "Válida até: " + quotation.getValidUntil().format(DATE_FMT)
                        + (expired ? " (EXPIRADA)" : "");
                ThermalReceiptRenderer.centered(doc, validityText, expired ? PdfTheme.boldFont() : PdfTheme.smallFont());
            }

            String clientName = quotation.getClient() != null ? quotation.getClient().getName() : quotation.getWalkInName();
            if (clientName != null && !clientName.isBlank()) {
                ThermalReceiptRenderer.centered(doc, "Cliente: " + clientName, PdfTheme.smallFont());
            }

            ThermalReceiptRenderer.dottedLine(doc);

            PdfPTable table = new PdfPTable(new float[]{65f, 35f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(2f);

            ThermalReceiptRenderer.cell(table, "Artigo", PdfTheme.tableHeaderFont(), Element.ALIGN_LEFT, true);
            ThermalReceiptRenderer.cell(table, "Total", PdfTheme.tableHeaderFont(), Element.ALIGN_RIGHT, true);

            for (QuotationLine line : quotation.getLines()) {
                com.lowagie.text.Phrase phrase = new com.lowagie.text.Phrase();
                phrase.add(new com.lowagie.text.Chunk(line.getProduct().getName(), PdfTheme.bodyFont()));
                String qtyPrice = line.getQuantity() + " un. × " + MoneyFormat.formatPlain(line.getUnitPrice());
                if (line.getDiscountPercentage() != null && line.getDiscountPercentage().signum() > 0) {
                    qtyPrice += " (-" + line.getDiscountPercentage().stripTrailingZeros().toPlainString() + "%)";
                }
                phrase.add(new com.lowagie.text.Chunk("\n" + qtyPrice, PdfTheme.smallFont()));
                PdfPCell descCell = new PdfPCell(phrase);
                descCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                descCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                descCell.setCellEvent(ThermalReceiptRenderer.DOTTED_BOTTOM);
                descCell.setPadding(3f);
                table.addCell(descCell);

                ThermalReceiptRenderer.cell(table, MoneyFormat.formatPlain(line.getLineTotal()), PdfTheme.boldFont(), Element.ALIGN_RIGHT, false);
            }
            doc.add(table);

            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.right(doc, "Subtotal: " + MoneyFormat.format(quotation.getTotalBeforeTax()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "IVA:      " + MoneyFormat.format(quotation.getTaxAmount()), PdfTheme.bodyFont());
            ThermalReceiptRenderer.right(doc, "TOTAL:    " + MoneyFormat.format(quotation.getTotalAmount()), PdfTheme.boldFont());

            ThermalReceiptRenderer.dottedLine(doc);
            ThermalReceiptRenderer.centered(doc, "PROPOSTA COMERCIAL INFORMATIVA", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "NÃO CONSTITUI DOCUMENTO FISCAL", PdfTheme.smallFont());
            ThermalReceiptRenderer.centered(doc, "Processado por Computador / Multicore ERP", PdfTheme.smallFont());
        });
    }

    /**
     * A validade é o que distingue uma proposta de uma fatura, por isso vai em destaque e não
     * perdida entre as condições. Quando já passou, o documento di-lo — reimprimir uma cotação
     * caducada sem aviso era a forma silenciosa de honrar um preço que já não está garantido.
     */
    private PdfPTable buildValidityBlock(Quotation quotation) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBackgroundColor(PdfTheme.TOTAL_ROW_BG);
        cell.setPadding(8f);

        Paragraph validity = new Paragraph(
                "Proposta válida até " + quotation.getValidUntil().format(DATE_FMT), PdfTheme.subtitleFont());
        cell.addElement(validity);

        if (quotation.isExpired(LocalDate.now())) {
            cell.addElement(new Paragraph(
                    "ATENÇÃO: esta cotação já caducou. Os preços apresentados carecem de nova confirmação.",
                    PdfTheme.boldFont()));
        } else {
            cell.addElement(new Paragraph(
                    "Os preços e condições abaixo são garantidos até à data indicada.", PdfTheme.bodyFont()));
        }
        table.addCell(cell);
        return table;
    }

    private Paragraph disclaimer() {
        Paragraph p = new Paragraph(
                "Este documento é uma proposta comercial. Não constitui documento fiscal nem substitui a "
                        + "factura, que será emitida após confirmação da encomenda.", PdfTheme.smallFont());
        p.setAlignment(Element.ALIGN_LEFT);
        return p;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private List<LineItemsTableRenderer.Row> toRows(List<QuotationLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(),
                null, // uma proposta não promete lote — o FEFO decide quando o stock se mover
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}
