package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.architecture.quantity.PackageQuantity;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Resumo de carga partilhado pelos documentos que <b>movimentam mercadoria</b>: guia de remessa
 * ao cliente e guia de transferência entre armazéns.
 *
 * <p>Existe para a regra viver num sítio só. O cálculo já estava dentro do
 * {@code DeliveryGuidePrintService}; copiá-lo para a transferência era pôr a mesma conta em duas
 * portas — o padrão de erro que este projecto já fechou várias vezes (IVA, saldo em dívida,
 * "isto conta como venda").
 *
 * <p>Usa o <b>peso bruto</b>: o que conta para transportar é o que se levanta, embalagem
 * incluída.
 */
public final class LoadSummaryRenderer {

    private LoadSummaryRenderer() {}

    /**
     * Uma linha do documento, reduzida ao que a carga precisa de saber.
     *
     * @param grossUnitWeightKg peso bruto por unidade; {@code null} quando o produto não o tem
     */
    public record Item(String productName, BigDecimal quantity, BigDecimal grossUnitWeightKg, int unitsPerBox) {}

    /**
     * Parágrafo com a carga total e a repartição por artigo, ou <b>{@code null}</b> quando não há
     * peso nenhum registado.
     *
     * <p>Devolver {@code null} em vez de "Carga total: 0,000 kg" é deliberado: numa empresa que
     * ainda não preencheu pesos no cadastro, um zero grande no documento diz que a carga não pesa
     * nada — o que é pior do que não dizer nada.
     */
    public static Paragraph build(List<Item> items) {
        if (items == null || items.isEmpty()) return null;

        List<LogisticsLoadCalculator.Share> shares = LogisticsLoadCalculator.calculate(items.stream()
                .map(item -> new LogisticsLoadCalculator.Input(item.quantity(), item.grossUnitWeightKg()))
                .toList());

        BigDecimal totalWeight = shares.stream()
                .map(LogisticsLoadCalculator.Share::lineWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() <= 0) return null;

        String detail = IntStream.range(0, items.size()).mapToObj(index -> {
            Item item = items.get(index);
            LogisticsLoadCalculator.Share share = shares.get(index);
            int factor = Math.max(1, item.unitsPerBox());
            return item.productName() + ": " + PackageQuantity.label(item.quantity(), factor)
                    + ", " + share.lineWeightKg() + " kg"
                    + " (" + share.quantityPercentage() + "% qtd; " + share.weightPercentage() + "% peso)";
        }).collect(Collectors.joining(" | "));

        Paragraph paragraph = new Paragraph(
                "Carga total: " + totalWeight + " kg. Volumes: " + detail, PdfTheme.smallFont());
        paragraph.setSpacingBefore(5f);
        return paragraph;
    }
}
