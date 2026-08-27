package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
    @NotNull(message = "O ID do cliente é obrigatório.")
    Long clientId,

    @NotNull(message = "O ID da empresa é obrigatório.")
    Long companyId,

    @NotNull(message = "O ID do armazém é obrigatório.")
    Long warehouseId,

    @NotEmpty(message = "A fatura deve conter pelo menos uma linha.")
    @Valid
    List<CreateInvoiceLineRequest> lines,

    /** Vencimento escolhido pelo operador. Vazio = prazo de pagamento do cliente. */
    LocalDate dueDate
) {

    /** Retrocompatível: sem vencimento explícito, manda o prazo acordado com o cliente. */
    public CreateInvoiceRequest(Long clientId, Long companyId, Long warehouseId,
                                List<CreateInvoiceLineRequest> lines) {
        this(clientId, companyId, warehouseId, lines, null);
    }
}
