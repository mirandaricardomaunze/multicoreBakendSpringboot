package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
    /** Opcional. Se nulo, a fatura é emitida para Consumidor Final usando walk-in client. */
    Long clientId,

    @NotNull(message = "O ID da empresa é obrigatório.")
    Long companyId,

    @NotNull(message = "O ID do armazém é obrigatório.")
    Long warehouseId,

    @NotEmpty(message = "A fatura deve conter pelo menos uma linha.")
    @Valid
    List<CreateInvoiceLineRequest> lines,

    /** Vencimento escolhido pelo operador. Vazio = prazo de pagamento do cliente. */
    LocalDate dueDate,

    /** Linha de negócio exclusiva (opcional). Se nula, aceita produtos mistos. */
    Long businessUnitId,

    /** Nome do comprador a mostrar no documento quando emitido para Consumidor Final ou cliente avulso. */
    String customerName
) {

    public CreateInvoiceRequest(Long clientId, Long companyId, Long warehouseId,
                                List<CreateInvoiceLineRequest> lines, LocalDate dueDate, Long businessUnitId) {
        this(clientId, companyId, warehouseId, lines, dueDate, businessUnitId, null);
    }

    public CreateInvoiceRequest(Long clientId, Long companyId, Long warehouseId,
                                List<CreateInvoiceLineRequest> lines, LocalDate dueDate) {
        this(clientId, companyId, warehouseId, lines, dueDate, null, null);
    }

    /** Retrocompatível: sem vencimento explícito, manda o prazo acordado com o cliente. */
    public CreateInvoiceRequest(Long clientId, Long companyId, Long warehouseId,
                                List<CreateInvoiceLineRequest> lines) {
        this(clientId, companyId, warehouseId, lines, null, null, null);
    }
}
