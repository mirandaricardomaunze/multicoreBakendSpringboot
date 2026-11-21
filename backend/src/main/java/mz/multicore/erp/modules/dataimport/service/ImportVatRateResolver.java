package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolve a coluna {@code iva} de um artigo importado (IMPORTACAO_DADOS_SPEC §4.3): o código de uma taxa
 * de IVA da empresa (sem distinguir maiúsculas) ou a sua percentagem ({@code 16}, {@code 16,00}, {@code 16%}).
 * Taxa inexistente, ou percentagem partilhada por duas taxas, é erro da linha — nunca se cria nem se adivinha
 * uma taxa. Classe pura: recebe as taxas activas da empresa já lidas pela porta do módulo fiscal.
 */
final class ImportVatRateResolver {

    private static final Set<TaxType> VAT_TYPES =
            EnumSet.of(TaxType.IVA_STANDARD, TaxType.IVA_REDUCED, TaxType.IVA_ZERO, TaxType.IVA_EXEMPT);
    private static final Pattern PERCENT_SUFFIX = Pattern.compile("\\s*%$");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final List<TaxRate> vatRates;
    private final ImportValueParser parser = new ImportValueParser();

    ImportVatRateResolver(List<TaxRate> companyRates) {
        this.vatRates = companyRates.stream().filter(rate -> VAT_TYPES.contains(rate.getType())).toList();
    }

    /** Taxa de IVA da empresa indicada na célula; {@link ImportValueException} se não houver uma só. */
    TaxRate resolve(String raw, int lineNumber, String column) {
        String value = parser.parseText(raw);
        if (value == null) {
            throw new ImportValueException(lineNumber, column, "a taxa de IVA é obrigatória.");
        }
        Optional<TaxRate> byCode = vatRates.stream()
                .filter(rate -> rate.getCode() != null && rate.getCode().equalsIgnoreCase(value))
                .findFirst();
        if (byCode.isPresent()) {
            return byCode.get();
        }
        List<TaxRate> byPercentage = percentage(value, lineNumber, column)
                .map(this::withPercentage)
                .orElse(List.of());
        if (byPercentage.size() == 1) {
            return byPercentage.get(0);
        }
        if (byPercentage.isEmpty()) {
            throw new ImportValueException(lineNumber, column, "a taxa de IVA «" + value
                    + "» não existe na empresa (use a percentagem, ex.: 16, ou o código de uma taxa existente).");
        }
        throw new ImportValueException(lineNumber, column, "a taxa de IVA «" + value + "» é ambígua ("
                + codes(byPercentage) + " têm essa percentagem); indique o código da taxa.");
    }

    private Optional<BigDecimal> percentage(String value, int lineNumber, String column) {
        try {
            return Optional.ofNullable(parser.parseDecimal(PERCENT_SUFFIX.matcher(value).replaceFirst(""), lineNumber, column));
        } catch (ImportValueException notAPercentage) {
            return Optional.empty(); // nem código nem percentagem: taxa inexistente
        }
    }

    private List<TaxRate> withPercentage(BigDecimal percentage) {
        return vatRates.stream()
                .filter(rate -> rate.getRate() != null && rate.getRate().multiply(HUNDRED).compareTo(percentage) == 0)
                .toList();
    }

    private static String codes(List<TaxRate> rates) {
        return rates.stream().map(TaxRate::getCode).collect(Collectors.joining(", "));
    }
}
