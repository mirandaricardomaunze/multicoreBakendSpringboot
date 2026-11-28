package mz.multicore.erp.modules.printing;

import com.lowagie.text.Document;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elementos fiscais que nenhum modelo de documento pode esconder (DT-03, SPEC §3.2): identificação do
 * adquirente (ou "Consumidor Final") e o IVA discriminado por taxa, com o motivo de isenção quando é zero.
 * Não depende das colunas visíveis: as linhas para o IVA são construídas só com os valores fiscais.
 */
public final class FiscalElements {

    public static final String FINAL_CONSUMER = "Consumidor Final";

    private FiscalElements() {}

    /** Nome do adquirente: o nome escrito no documento, senão o do cliente, senão "Consumidor Final". */
    public static String buyerName(String customerName, Client client) {
        if (useful(customerName)) return customerName.trim();
        if (client != null && useful(client.getName())) return client.getName().trim();
        return FINAL_CONSUMER;
    }

    /** NUIT do adquirente, ou "Consumidor Final" quando não existe. */
    public static String buyerTaxId(Client client) {
        return client != null && useful(client.getTaxId()) ? client.getTaxId().trim() : FINAL_CONSUMER;
    }

    /** Linha só com os valores fiscais, para o discriminativo de IVA. */
    public static LineItemsTableRenderer.Row taxRow(BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate,
                                                    BigDecimal discountPercentage, BigDecimal lineTotal,
                                                    String exemptionReason) {
        return new LineItemsTableRenderer.Row(null, null, null, null, quantity, null, null, unitPrice, taxRate,
                discountPercentage, lineTotal, exemptionReason);
    }

    /** IVA por taxa das linhas de uma factura (inclui o motivo de isenção das linhas a 0%). */
    public static List<TotalsBlockRenderer.TaxBreakdown> invoiceBreakdowns(List<InvoiceLine> lines) {
        return TotalsBlockRenderer.calculateBreakdowns(lines.stream().map(line -> taxRow(line.getQuantity(),
                line.getUnitPrice(), line.getTaxRate(), line.getDiscountPercentage(), line.getLineTotal(),
                line.effectiveExemptionReason())).toList());
    }

    /** Discriminativo de IVA por taxa num talão térmico, uma linha por taxa. */
    public static void thermalVatByRate(Document doc, List<TotalsBlockRenderer.TaxBreakdown> breakdowns) {
        for (TotalsBlockRenderer.TaxBreakdown b : breakdowns) {
            ThermalReceiptRenderer.right(doc, b.taxRateLabel() + " s/ " + MoneyFormat.format(b.taxableAmount())
                    + ": " + MoneyFormat.format(b.taxAmount()), PdfTheme.smallFont());
        }
    }

    private static boolean useful(String value) {
        return value != null && !value.isBlank() && !"-".equals(value.trim()) && !"—".equals(value.trim());
    }
}
