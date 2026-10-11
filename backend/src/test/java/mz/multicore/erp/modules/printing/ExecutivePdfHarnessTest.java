package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.modules.company.model.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de teste canónico dos PDFs executivos do Multicore ERP.
 * Valida a renderização corporativa A4, talão térmico 80mm, discriminação de impostos (IVA)
 * e coerência visual dos blocos de totais.
 */
class ExecutivePdfHarnessTest {

    @Test
    @DisplayName("EP-01: Discriminação automática de IVA por taxa a partir de linhas de documento")
    void discriminacaoAutomaticaDeIvaPorTaxa() {
        List<LineItemsTableRenderer.Row> rows = List.of(
                new LineItemsTableRenderer.Row(
                        "56010001", "REF-01", "Arroz Agulha 5kg", LocalDate.now().plusMonths(6),
                        BigDecimal.valueOf(10), 1, 1, BigDecimal.valueOf(500), BigDecimal.valueOf(0.16),
                        BigDecimal.ZERO, BigDecimal.valueOf(5000)
                ),
                new LineItemsTableRenderer.Row(
                        "56010002", "REF-02", "Farinha de Milho 1kg", LocalDate.now().plusMonths(3),
                        BigDecimal.valueOf(20), 1, 1, BigDecimal.valueOf(50), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.valueOf(1000)
                )
        );

        List<TotalsBlockRenderer.TaxBreakdown> breakdowns = TotalsBlockRenderer.calculateBreakdowns(rows);

        assertThat(breakdowns).hasSize(2);

        // Taxa 16%: Incidência 5.000,00 MT, Imposto 800,00 MT
        TotalsBlockRenderer.TaxBreakdown generalTax = breakdowns.stream()
                .filter(b -> b.ratePercentage().compareTo(BigDecimal.valueOf(16)) == 0)
                .findFirst()
                .orElseThrow();
        assertThat(generalTax.taxableAmount()).isEqualByComparingTo("5000.00");
        assertThat(generalTax.taxAmount()).isEqualByComparingTo("800.00");

        // Taxa Isenta 0%: Incidência 1.000,00 MT, Imposto 0,00 MT
        TotalsBlockRenderer.TaxBreakdown exemptTax = breakdowns.stream()
                .filter(b -> b.ratePercentage().compareTo(BigDecimal.ZERO) == 0)
                .findFirst()
                .orElseThrow();
        assertThat(exemptTax.taxableAmount()).isEqualByComparingTo("1000.00");
        assertThat(exemptTax.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(exemptTax.taxRateLabel()).contains("Isento");
    }

    @Test
    @DisplayName("EP-02: Renderização de A4 inclui quadro discriminativo de taxas e totalizador")
    void renderizacaoA4ComQuadroDiscriminativo() throws Exception {
        List<TotalsBlockRenderer.TaxBreakdown> breakdowns = List.of(
                new TotalsBlockRenderer.TaxBreakdown("IVA 16.0%", BigDecimal.valueOf(16), BigDecimal.valueOf(10000), BigDecimal.valueOf(1600)),
                new TotalsBlockRenderer.TaxBreakdown("Isento (Art. 9)", BigDecimal.ZERO, BigDecimal.valueOf(2500), BigDecimal.ZERO)
        );

        byte[] pdf = PdfDocumentBuilder.buildA4(doc -> {
            doc.add(TotalsBlockRenderer.build(
                    BigDecimal.valueOf(12500),
                    BigDecimal.valueOf(1600),
                    BigDecimal.valueOf(14100),
                    breakdowns
            ));
        });

        assertThat(pdf).isNotEmpty();

        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(text)
                .contains("Taxa / Motivo", "Incidência", "Valor IVA")
                .contains("IVA 16.0%", "1.600,00")
                .contains("Isento (Art. 9)")
                .contains("TOTAL A PAGAR", "14.100,00 MT");
    }

    @Test
    @DisplayName("EP-03: Renderização de talão térmico 80mm com cabeçalho corporativo e separadores pontilhados")
    void renderizacaoTermica80mmComCabecalhoESeparadores() throws Exception {
        Company company = new Company();
        company.setName("Distribuidora Central de Maputo");
        company.setTaxId("400998877");
        company.setAddress("Avenida de Moçambique, Km 5");
        company.setPhone("+258 84 123 4567");

        byte[] receiptPdf = PdfDocumentBuilder.buildReceipt(doc -> {
            ThermalReceiptRenderer.companyHeader(doc, company);
            ThermalReceiptRenderer.centered(doc, "VENDA A DINHEIRO (POS)", PdfTheme.boldFont());
            ThermalReceiptRenderer.right(doc, "TOTAL: 750,00 MT", PdfTheme.boldFont());
        });

        assertThat(receiptPdf).isNotEmpty();

        PdfReader reader = new PdfReader(new ByteArrayInputStream(receiptPdf));
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(text)
                .contains("Distribuidora Central", "Maputo")
                .contains("NUIT: 400998877")
                .contains("VENDA A DINHEIRO (POS)")
                .contains("TOTAL: 750,00 MT");
    }
}
