package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.model.FleetManifestStop;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import mz.multicore.erp.modules.printing.PdfTheme;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
public class FleetManifestPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    private final FleetManifestRepository manifestRepository;

    public FleetManifestPrintService(FleetManifestRepository manifestRepository) {
        this.manifestRepository = manifestRepository;
    }

    @Transactional(readOnly = true)
    public byte[] renderA4(Long manifestId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        FleetManifest manifest = manifestRepository.findByIdAndCompanyIdWithStops(manifestId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Manifesto de carga não encontrado: " + manifestId));

        return PdfDocumentBuilder.build(com.lowagie.text.PageSize.A4, doc -> renderDocument(doc, manifest));
    }

    private void renderDocument(Document document, FleetManifest m) {
        String statusLabel = switch (m.getStatus()) {
            case "IN_TRANSIT" -> "EM TRÂNSITO";
            case "COMPLETED" -> "CONCLUÍDO";
            case "CANCELLED" -> "CANCELADO";
            default -> m.getStatus();
        };

        document.add(CompanyHeaderRenderer.build(
                m.getCompany(),
                "MANIFESTO DE CARGA & ROTA DE DISTRIBUIÇÃO",
                m.getManifestNumber(),
                statusLabel
        ));

        document.add(buildTransportInfoTable(m));
        document.add(PdfDocumentBuilder.spacer(8f));

        document.add(buildKpiSummaryTable(m));
        document.add(PdfDocumentBuilder.spacer(10f));

        document.add(buildStopsTable(m));
        document.add(PdfDocumentBuilder.spacer(14f));

        document.add(buildSignaturesBlock());
    }

    private PdfPTable buildTransportInfoTable(FleetManifest m) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{50f, 50f});
        } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("VIATURA: " + m.getVehiclePlate() + (m.getVehicleModel() != null ? " (" + m.getVehicleModel() + ")" : ""), PdfTheme.boldFont()));
        left.addElement(new Paragraph("MOTORISTA: " + m.getDriverName() + (m.getDriverPhone() != null && !m.getDriverPhone().isBlank() ? " | Tel: " + m.getDriverPhone() : ""), PdfTheme.bodyFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("ROTA / DESTINO: " + m.getRouteName(), PdfTheme.boldFont()));
        String dateStr = m.getManifestDate() != null ? m.getManifestDate().format(DATE_FMT) : "";
        right.addElement(new Paragraph("DATA / HORA DE SAÍDA: " + dateStr + (m.getDepartureTime() != null && !m.getDepartureTime().isBlank() ? " (" + m.getDepartureTime() + ")" : ""), PdfTheme.bodyFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private PdfPTable buildKpiSummaryTable(FleetManifest m) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{25f, 25f, 25f, 25f});
        } catch (Exception ignored) {}

        addKpiCell(table, "PARADAS / ENTREGAS", String.valueOf(m.getTotalStops()));
        addKpiCell(table, "TOTAL CAIXAS (Cx.)", m.getTotalBoxes() != null ? m.getTotalBoxes().toPlainString() : "0.00");
        addKpiCell(table, "TOTAL EMBALAGENS (Emb.)", m.getTotalPackages() != null ? m.getTotalPackages().toPlainString() : "0.00");
        addKpiCell(table, "TOTAL A COBRAR", CURRENCY_FMT.format(m.getTotalAmountToCollect() != null ? m.getTotalAmountToCollect() : BigDecimal.ZERO));

        return table;
    }

    private void addKpiCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(6f);

        Paragraph pLabel = new Paragraph(label, PdfTheme.smallFont());
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(value, PdfTheme.boldFont());
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private PdfPTable buildStopsTable(FleetManifest m) {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        try {
            // # (4), Cliente (22), Morada (18), Guia (12), Volumes (12), Condição (12), Hora (8), Assinatura (12)
            table.setWidths(new float[]{4f, 22f, 18f, 12f, 12f, 12f, 8f, 12f});
        } catch (Exception ignored) {}

        addHeader(table, "#", Element.ALIGN_CENTER);
        addHeader(table, "Cliente / Destinatário", Element.ALIGN_LEFT);
        addHeader(table, "Morada / Localidade", Element.ALIGN_LEFT);
        addHeader(table, "Nº Guia", Element.ALIGN_CENTER);
        addHeader(table, "Volumes (Cx/Emb)", Element.ALIGN_RIGHT);
        addHeader(table, "Condição / Cobrança", Element.ALIGN_CENTER);
        addHeader(table, "Chegada", Element.ALIGN_CENTER);
        addHeader(table, "Visto / Assinatura", Element.ALIGN_CENTER);

        boolean alt = false;
        for (FleetManifestStop stop : m.getStops()) {
            addCell(table, String.valueOf(stop.getSequence()), Element.ALIGN_CENTER, alt, false);

            String clientText = stop.getClientName() + (stop.getContactPhone() != null && !stop.getContactPhone().isBlank() ? "\nTel: " + stop.getContactPhone() : "");
            addCell(table, clientText, Element.ALIGN_LEFT, alt, false);

            addCell(table, stop.getDeliveryAddress() != null ? stop.getDeliveryAddress() : "-", Element.ALIGN_LEFT, alt, false);
            addCell(table, stop.getDocumentNumber(), Element.ALIGN_CENTER, alt, true);

            String volText = (stop.getBoxCount() != null ? stop.getBoxCount().toPlainString() : "0.00") + " Cx\n" +
                    (stop.getPackageCount() != null ? stop.getPackageCount().toPlainString() : "0.00") + " Emb";
            addCell(table, volText, Element.ALIGN_RIGHT, alt, false);

            String condText = stop.getAmountToCollect() != null && stop.getAmountToCollect().signum() > 0
                    ? "Cobrar: " + CURRENCY_FMT.format(stop.getAmountToCollect())
                    : (stop.getPaymentTerms() != null ? stop.getPaymentTerms() : "Conta Corrente");
            addCell(table, condText, Element.ALIGN_CENTER, alt, false);

            addCell(table, "[__:__]", Element.ALIGN_CENTER, alt, false);
            addCell(table, "_______________", Element.ALIGN_CENTER, alt, false);

            alt = !alt;
        }

        return table;
    }

    private void addHeader(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, int align, boolean alt, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        cell.setBackgroundColor(alt ? PdfTheme.ROW_ALT : Color.WHITE);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{50f, 50f});
        } catch (Exception ignored) {}

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("EXPEDIÇÃO / CONFERENTE DE CARGA:", PdfTheme.smallFont()));
        left.addElement(PdfDocumentBuilder.spacer(18f));
        left.addElement(new Paragraph("________________________________________", PdfTheme.bodyFont()));
        left.addElement(new Paragraph("Data e Hora: ____/____/________  ____:____", PdfTheme.smallFont()));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.addElement(new Paragraph("MOTORISTA / TRANSPORTADOR:", PdfTheme.smallFont()));
        right.addElement(PdfDocumentBuilder.spacer(18f));
        right.addElement(new Paragraph("________________________________________", PdfTheme.bodyFont()));
        right.addElement(new Paragraph("Declaro ter conferido e recebido os volumes listados.", PdfTheme.smallFont()));

        table.addCell(left);
        table.addCell(right);
        return table;
    }
}
