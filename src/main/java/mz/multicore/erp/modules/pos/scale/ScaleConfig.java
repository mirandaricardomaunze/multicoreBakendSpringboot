package mz.multicore.erp.modules.pos.scale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Constrói o {@link ScaleBarcodeParser} a partir de {@code retail.scale.*} (ver
 * {@code application.properties}). Mantém a configuração fora do código e o parser puro/testável.
 */
@Configuration
public class ScaleConfig {

    @Bean
    public ScaleBarcodeParser scaleBarcodeParser(
            @Value("${retail.scale.enabled:true}") boolean enabled,
            @Value("${retail.scale.prefix:2}") String prefix,
            @Value("${retail.scale.item-digits:5}") int itemDigits,
            @Value("${retail.scale.measure-digits:6}") int measureDigits,
            @Value("${retail.scale.embedded:WEIGHT}") EmbeddedMeasure embedded,
            @Value("${retail.scale.weight-divisor:1000}") int weightDivisor,
            @Value("${retail.scale.price-divisor:100}") int priceDivisor) {
        return new ScaleBarcodeParser(new ScaleBarcodeFormat(
                enabled, prefix, itemDigits, measureDigits, embedded, weightDivisor, priceDivisor));
    }
}
