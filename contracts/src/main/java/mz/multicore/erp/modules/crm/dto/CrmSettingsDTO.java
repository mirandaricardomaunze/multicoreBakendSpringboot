package mz.multicore.erp.modules.crm.dto;

import java.math.BigDecimal;

/**
 * Parâmetros da assistência técnica. Hoje só a tarifa horária.
 *
 * <p>A tarifa <b>não</b> é uma definição à parte: é o preço do produto de mão de obra no catálogo
 * ({@code labourSku}), que é o mesmo preço que a factura cobra. Guardá-la em dois sítios era
 * garantir que um dia divergiam — o que o cliente assina na folha e o que a factura diz.
 */
public record CrmSettingsDTO(
    BigDecimal hourlyRate,
    String labourSku,
    String labourProductName
) {}
