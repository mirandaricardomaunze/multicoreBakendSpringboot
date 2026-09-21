package mz.multicore.erp.modules.printing;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.CashFlowBucketDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowItemDTO;
import mz.multicore.erp.modules.financeira.service.CashFlowForecastService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class CashFlowForecastPrintService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CashFlowForecastService forecastService;
    private final CompanyService companyService;

    public CashFlowForecastPrintService(
            CashFlowForecastService forecastService,
            CompanyService companyService
    ) {
        this.forecastService = forecastService;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        CashFlowForecastDTO forecast = forecastService.generateForecastForCompany(companyId, null);

        return PdfDocumentBuilder.buildA4(doc -> {
            // 1. Cabeçalho Institucional da Empresa
            String docNumber = "MAP-TES/" + forecast.generatedDate().getYear();
            doc.add(CompanyHeaderRenderer.build(company, "Mapa Previsional de Fluxo de Caixa", docNumber));

            // 2. Metadados
            doc.add(buildMetaBlock(forecast));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // 3. Cartões Métricos Superiores
            doc.add(buildSummaryCards(forecast));
            doc.add(PdfDocumentBuilder.spacer(8f));

            // 4. Banner de Alerta de Tesouraria
            doc.add(buildAlertBanner(forecast));
            doc.add(PdfDocumentBuilder.spacer(10f));

            // 5. Matriz Temporal de Liquidez Previsional
            doc.add(buildBucketMatrixTable(forecast));
            doc.add(PdfDocumentBuilder.spacer(12f));

            // 6. Principais Movimentos (Recebimentos e Pagamentos Previstos)
            doc.add(buildTopMovementsTable(forecast));
            doc.add(PdfDocumentBuilder.spacer(16f));

            // 7. Parecer Financeiro e Assinaturas
            doc.add(buildSignaturesBlock());
        });
    }

    private PdfPTable buildMetaBlock(CashFlowForecastDTO forecast) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);

        PdfPCell c1 = new PdfPCell(new Paragraph("Data de Emissão: " + forecast.generatedDate().format(DATE_FMT), PdfTheme.bodyFont()));
        c1.setBorder(0);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Paragraph("Moeda de Referência: " + PdfTheme.CURRENCY_CODE + " (Meticais)", PdfTheme.bodyFont()));
        c2.setBorder(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);

        return table;
    }

    private PdfPTable buildSummaryCards(CashFlowForecastDTO forecast) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        try { table.setWidths(new float[]{25f, 25f, 25f, 25f}); } catch (Exception ignored) {}

        table.addCell(metricCard("Disponível Atual (Caixa+Banco)", forecast.totalAvailableLiquidity(), new Color(240, 246, 255)));
        table.addCell(metricCard("Total a Receber (Clientes)", forecast.totalReceivables(), new Color(240, 253, 244)));
        table.addCell(metricCard("Total a Pagar (Fornecedores)", forecast.totalPayables(), new Color(254, 242, 242)));
        table.addCell(metricCard("Posição Projetada Líquida", forecast.netProjectedPosition(), new Color(245, 245, 247)));

        return table;
    }

    private PdfPCell metricCard(String title, BigDecimal amount, Color bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);

        Paragraph pTitle = new Paragraph(title, PdfTheme.smallFont());
        pTitle.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pTitle);

        Paragraph pVal = new Paragraph(MoneyFormat.format(amount), PdfTheme.boldFont());
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        return cell;
    }

    private PdfPTable buildAlertBanner(CashFlowForecastDTO forecast) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);

        Color bannerBg;
        String titlePrefix;
        if ("CRITICAL".equals(forecast.alert().status())) {
            bannerBg = new Color(254, 226, 226);
            titlePrefix = "ALERTA CRÍTICO DE TESOURARIA: ";
        } else if ("WARNING".equals(forecast.alert().status())) {
            bannerBg = new Color(254, 243, 199);
            titlePrefix = "AVISO DE CAIXA: ";
        } else {
            bannerBg = new Color(220, 252, 231);
            titlePrefix = "SITUAÇÃO DE TESOURARIA ESTÁVEL: ";
        }

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bannerBg);
        cell.setPadding(8f);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setBorderWidth(0.5f);

        Paragraph p = new Paragraph(titlePrefix + forecast.alert().alertMessage(), PdfTheme.boldFont());
        cell.addElement(p);

        if (forecast.alert().recommendation() != null) {
            Paragraph rec = new Paragraph("Recomendação: " + forecast.alert().recommendation(), PdfTheme.bodyFont());
            cell.addElement(rec);
        }

        table.addCell(cell);
        return table;
    }

    private PdfPTable buildBucketMatrixTable(CashFlowForecastDTO forecast) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        try { table.setWidths(new float[]{22f, 19.5f, 19.5f, 19.5f, 19.5f}); } catch (Exception ignored) {}

        // Cabeçalhos
        addHeaderCell(table, "Período Temporal", Element.ALIGN_LEFT);
        addHeaderCell(table, "Entradas Previstas", Element.ALIGN_RIGHT);
        addHeaderCell(table, "Saídas Previstas", Element.ALIGN_RIGHT);
        addHeaderCell(table, "Movimento Líquido", Element.ALIGN_RIGHT);
        addHeaderCell(table, "Saldo Cumulativo", Element.ALIGN_RIGHT);

        boolean alternate = false;
        for (CashFlowBucketDTO b : forecast.buckets()) {
            Color rowBg = alternate ? PdfTheme.TOTAL_ROW_BG : Color.WHITE;
            alternate = !alternate;

            addCell(table, b.bucketLabel(), rowBg, Element.ALIGN_LEFT, false);
            addCell(table, MoneyFormat.format(b.inflows()), rowBg, Element.ALIGN_RIGHT, false);
            addCell(table, MoneyFormat.format(b.outflows()), rowBg, Element.ALIGN_RIGHT, false);
            addCell(table, MoneyFormat.format(b.netMovement()), rowBg, Element.ALIGN_RIGHT, false);

            Color saldoBg = b.projectedCumulativeBalance().compareTo(BigDecimal.ZERO) < 0
                    ? new Color(254, 226, 226)
                    : rowBg;
            addCell(table, MoneyFormat.format(b.projectedCumulativeBalance()), saldoBg, Element.ALIGN_RIGHT, true);
        }

        return table;
    }

    private void addHeaderCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, PdfTheme.tableHeaderFont()));
        cell.setBackgroundColor(PdfTheme.TABLE_HEADER_BG);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Color bg, int align, boolean bold) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, bold ? PdfTheme.boldFont() : PdfTheme.bodyFont()));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(PdfTheme.BORDER);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private PdfPTable buildTopMovementsTable(CashFlowForecastDTO forecast) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);

        PdfPCell leftCol = new PdfPCell();
        leftCol.setBorder(0);
        leftCol.addElement(new Paragraph("Maiores Contas a Receber (Clientes):", PdfTheme.boldFont()));
        for (CashFlowItemDTO r : forecast.topReceivables()) {
            Paragraph p = new Paragraph(String.format("• %s (%s): %s [Venc: %s]",
                    r.documentNumber(), r.entityName(), MoneyFormat.format(r.amount()), r.dueDate().format(DATE_FMT)), PdfTheme.smallFont());
            leftCol.addElement(p);
        }
        table.addCell(leftCol);

        PdfPCell rightCol = new PdfPCell();
        rightCol.setBorder(0);
        rightCol.addElement(new Paragraph("Maiores Contas a Pagar (Fornecedores):", PdfTheme.boldFont()));
        for (CashFlowItemDTO pItem : forecast.topPayables()) {
            Paragraph p = new Paragraph(String.format("• %s (%s): %s [Venc: %s]",
                    pItem.documentNumber(), pItem.entityName(), MoneyFormat.format(pItem.amount()), pItem.dueDate().format(DATE_FMT)), PdfTheme.smallFont());
            rightCol.addElement(p);
        }
        table.addCell(rightCol);

        return table;
    }

    private PdfPTable buildSignaturesBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(0);
        c1.setPaddingTop(25f);
        c1.addElement(new Paragraph("____________________________________________", PdfTheme.smallFont()));
        c1.addElement(new Paragraph("Responsável de Tesouraria / Caixa", PdfTheme.bodyFont()));
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setBorder(0);
        c2.setPaddingTop(25f);
        c2.addElement(new Paragraph("____________________________________________", PdfTheme.smallFont()));
        c2.addElement(new Paragraph("Direção Financeira / Gerência", PdfTheme.bodyFont()));
        table.addCell(c2);

        return table;
    }
}
