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

        return PdfDocumentBuilder.buildA4(doc -> {
            doc.add(CompanyHeaderRenderer.build(
                    quotation.getCompany(),
                    "Cotação",
                    quotation.getQuotationNumber()
            ));
            doc.add(ClientBlockRenderer.build(quotation.getClient(), quotation.getWalkInName(),
                    quotation.getQuotationDate(), null));
            doc.add(buildValidityBlock(quotation));
            doc.add(LineItemsTableRenderer.build(toRows(quotation.getLines()),
                    documentConfigService.getColumns(quotation.getCompany().getId(), DocumentType.COMMERCIAL)));
            doc.add(TotalsBlockRenderer.build(
                    quotation.getTotalBeforeTax(),
                    quotation.getTaxAmount(),
                    quotation.getTotalAmount()
            ));
            doc.add(PdfDocumentBuilder.spacer(10f));
            // Bloco partilhado com a encomenda A4 — são o mesmo acordo, não podem divergir.
            PdfPTable terms = CommercialTermsRenderer.build(
                    quotation.getPaymentTerms(), quotation.getDeliveryTerms(), null, quotation.getNotes());
            if (terms != null) {
                doc.add(terms);
            }
            doc.add(PdfDocumentBuilder.spacer(18f));
            doc.add(SignatureBlockRenderer.build(
                    "Pela empresa", "Aceite pelo cliente (nome, assinatura e data)"));
            doc.add(PdfDocumentBuilder.spacer(8f));
            doc.add(disclaimer());
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
