package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveClientRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 40) String taxId,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 300) String address,

        /** Prazo de pagamento em dias. Nulo = pronto pagamento (0). Tecto de 365 evita enganos de dedo. */
        @Min(value = 0, message = "O prazo de pagamento não pode ser negativo.")
        @Max(value = 365, message = "O prazo de pagamento não pode exceder 365 dias.")
        Integer paymentTermsDays,

        /** Tecto de dívida. Nulo = sem limite; zero = não vende fiado. */
        @DecimalMin(value = "0.00", message = "O limite de crédito não pode ser negativo.")
        BigDecimal creditLimit
) {

    /** Retrocompatível: sem prazo indicado, pronto pagamento; sem limite de crédito. */
    public SaveClientRequest(String name, String taxId, String email, String address) {
        this(name, taxId, email, address, 0, null);
    }

    /** Retrocompatível: prazo indicado, sem limite de crédito. */
    public SaveClientRequest(String name, String taxId, String email, String address, Integer paymentTermsDays) {
        this(name, taxId, email, address, paymentTermsDays, null);
    }

    /** Prazo efectivo, nunca nulo — a mesma leitura que {@code Client.effectivePaymentTermsDays()}. */
    public int effectivePaymentTermsDays() {
        return paymentTermsDays == null || paymentTermsDays < 0 ? 0 : paymentTermsDays;
    }
}
