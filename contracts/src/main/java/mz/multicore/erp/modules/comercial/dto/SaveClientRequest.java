package mz.multicore.erp.modules.comercial.dto;

import mz.multicore.erp.architecture.validation.ValidNuit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveClientRequest(
        @NotBlank(message = "O nome do cliente é obrigatório.")
        @Size(max = 200, message = "O nome não pode exceder 200 caracteres.")
        String name,

        @NotBlank(message = "O NUIT é obrigatório.")
        @ValidNuit
        String taxId,

        @NotBlank(message = "O email é obrigatório.")
        @Email(message = "O email deve ter um formato válido.")
        @Size(max = 200, message = "O email não pode exceder 200 caracteres.")
        String email,

        @Size(max = 300, message = "O endereço não pode exceder 300 caracteres.")
        String address,

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
