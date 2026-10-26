package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.documents.service.DocumentConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Generates the PDF for a sales order (Encomenda). */
@Service
public class OrderPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrderRepository orderRepository;
    private final LineRowMapper lineRowMapper;
    private final DocumentConfigService documentConfigService;

    public OrderPrintService(OrderRepository orderRepository, LineRowMapper lineRowMapper,
                             DocumentConfigService documentConfigService) {
        this.orderRepository = orderRepository;
        this.lineRowMapper = lineRowMapper;
        this.documentConfigService = documentConfigService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));

        CurrentUserContext.requireCompany(order.getCompany().getId());
        List<LineItemsTableRenderer.Row> rows = toRows(order.getLines());

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(buildHeaderTable(order.getCompany(), order));
            doc.add(buildClientAndMetaTable(order));

            // Origem: quem abre a encomenda tem de conseguir chegar à proposta que a justificou.
            if (order.getQuotationNumber() != null && !order.getQuotationNumber().isBlank()) {
                Paragraph origin = new Paragraph(
                        "Origem: Cotação " + order.getQuotationNumber(), PdfTheme.boldFont());
                origin.setSpacingAfter(6f);
                doc.add(origin);
            }

            // Tabela corporativa canónica com coluna de Código de Barras
            doc.add(LineItemsTableRenderer.buildModelTable(rows));

            // Peso bruto total se aplicável
            java.math.BigDecimal grossWeight = order.getLines().stream()
                    .map(line -> line.getProduct().getGrossUnitWeightKg() == null ? java.math.BigDecimal.ZERO
                            : line.getQuantity().multiply(line.getProduct().getGrossUnitWeightKg()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (grossWeight.compareTo(java.math.BigDecimal.ZERO) > 0) {
                Paragraph weight = new Paragraph("Peso bruto total: "
                        + grossWeight.setScale(3, java.math.RoundingMode.HALF_UP) + " kg", PdfTheme.boldFont());
                weight.setAlignment(Element.ALIGN_RIGHT);
                weight.setSpacingAfter(4f);
                doc.add(weight);
            }

            // Bloco de Totais
            java.math.BigDecimal totalDiscount = calculateTotalDiscount(order.getLines());
            doc.add(TotalsBlockRenderer.buildModelTotals(
                    order.getTotalBeforeTax(),
                    totalDiscount,
                    order.getTaxAmount(),
                    order.getTotalAmount(),
                    TotalsBlockRenderer.calculateBreakdowns(rows)
            ));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // Condições acordadas + data prometida
            PdfPTable terms = CommercialTermsRenderer.build(order.getPaymentTerms(),
                    order.getDeliveryTerms(), order.getExpectedDeliveryDate(), null);
            if (terms != null) {
                doc.add(terms);
            }

            // Bloco de Notas / Observações e Rodapé Corporativo
            var columns = documentConfigService.getColumns(order.getCompany().getId(),
                    mz.multicore.erp.modules.documents.model.DocumentType.COMMERCIAL);
            PdfPTable footerBlock = buildFooterBlock(order.getNotes(), columns != null ? columns.footer() : null);
            if (footerBlock != null) {
                doc.add(footerBlock);
            }

            doc.add(PdfDocumentBuilder.spacer(14f));
            doc.add(SignatureBlockRenderer.build("Pela empresa", "Confirmação do cliente"));
        });
    }

    private static PdfPTable buildHeaderTable(mz.multicore.erp.modules.company.model.Company company, Order order) {
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

        Paragraph docTitle = new Paragraph("ENCOMENDA", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 22f, PdfTheme.BRAND));
        docTitle.setAlignment(Element.ALIGN_RIGHT);
        docCell.addElement(docTitle);

        String num = "N.º " + (order.getOrderNumber() != null ? order.getOrderNumber() : "—");
        Paragraph numP = new Paragraph(num, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11f, PdfTheme.BRAND));
        numP.setAlignment(Element.ALIGN_RIGHT);
        numP.setSpacingAfter(2f);
        docCell.addElement(numP);

        String statusStr = "Estado: " + OrderStatusLabel.of(order.getStatus());
        Paragraph statusP = new Paragraph(statusStr, com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8.5f, PdfTheme.MUTED));
        statusP.setAlignment(Element.ALIGN_RIGHT);
        docCell.addElement(statusP);

        table.addCell(docCell);
        return table;
    }

    private static PdfPTable buildClientAndMetaTable(Order order) {
        PdfPTable table = new PdfPTable(new float[]{55f, 45f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);

        // CLIENTE (Esquerda)
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(PdfPCell.NO_BORDER);

        Paragraph clienteLabel = new Paragraph("ENCOMENDADO POR", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8f, PdfTheme.MUTED));
        clienteLabel.setSpacingAfter(2f);
        clientCell.addElement(clienteLabel);

        Client client = order.getClient();
        String buyerName = order.getWalkInName() != null && !order.getWalkInName().isBlank()
                ? order.getWalkInName().trim()
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

        String issueDateStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "—";
        String deliveryStr = order.getExpectedDeliveryDate() != null ? order.getExpectedDeliveryDate().format(DATE_FMT) : "—";

        String condPag = order.getPaymentTerms() != null && !order.getPaymentTerms().isBlank()
                ? order.getPaymentTerms().trim()
                : (client != null && client.effectivePaymentTermsDays() > 0 ? client.effectivePaymentTermsDays() + " dias" : "Pronto Pagamento");
        String refStr = order.getOrderNumber() != null ? order.getOrderNumber() : "—";

        addMetaBox(grid, "DATA DE EMISSÃO", issueDateStr);
        addMetaBox(grid, "PREVISÃO ENTREGA", deliveryStr);
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

    private static java.math.BigDecimal calculateTotalDiscount(List<OrderLine> lines) {
        java.math.BigDecimal totalDiscount = java.math.BigDecimal.ZERO;
        if (lines != null) {
            for (OrderLine line : lines) {
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

    private static PdfPTable buildFooterBlock(String orderNotes, String companyFooter) {
        boolean hasNotes = orderNotes != null && !orderNotes.isBlank();
        boolean hasFooter = companyFooter != null && !companyFooter.isBlank();
        if (!hasNotes && !hasFooter) {
            return null;
        }

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(4f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setPadding(6f);

        if (hasNotes) {
            Paragraph title = new Paragraph("Observações / Instruções:", PdfTheme.boldFont());
            title.setSpacingAfter(2f);
            cell.addElement(title);
            Paragraph note = new Paragraph(orderNotes.trim(), PdfTheme.bodyFont());
            note.setSpacingAfter(hasFooter ? 4f : 0f);
            cell.addElement(note);
        }

        if (hasFooter) {
            Paragraph foot = new Paragraph(companyFooter.trim(), PdfTheme.smallFont());
            foot.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(foot);
        }

        table.addCell(cell);
        return table;
    }

    private List<LineItemsTableRenderer.Row> toRows(List<OrderLine> lines) {
        return lines.stream().map(l -> lineRowMapper.map(
                l.getProduct(),
                l.getBatchNumber(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getTaxRate(),
                l.getDiscountPercentage(),
                l.getLineTotal()
        )).toList();
    }
}
