package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.modules.comercial.model.OrderKind;

import java.util.List;

/**
 * Pedido para criar uma encomenda. O cliente é opcional — se {@code clientId} for nulo,
 * a encomenda fica para "Consumidor Final" e {@code walkInName} (se preenchido) é usado
 * como rótulo livre no descritor, sem criar registo de cliente.
 *
 * <p>Distinto de {@code CreateInvoiceRequest} para manter o cliente <strong>obrigatório</strong>
 * em facturas (requisito fiscal) sem afectar este fluxo.
 */
public record CreateOrderRequest(
        Long clientId,
        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,
        @NotNull(message = "O ID da empresa é obrigatório.") Long companyId,
        @NotNull(message = "O ID do armazém é obrigatório.") Long warehouseId,
        @NotEmpty(message = "A encomenda deve conter pelo menos uma linha.") @Valid
        List<CreateInvoiceLineRequest> lines,
        /** Via da encomenda. Ausente = {@link OrderKind#FORMAL_ORDER}, o comportamento de sempre. */
        OrderKind kind
) {
    /** Construtor retrocompatível: quem não declara a via fica na encomenda A4 formal. */
    public CreateOrderRequest(Long clientId, String walkInName, Long companyId, Long warehouseId,
                              List<CreateInvoiceLineRequest> lines) {
        this(clientId, walkInName, companyId, warehouseId, lines, OrderKind.FORMAL_ORDER);
    }

    public OrderKind effectiveKind() {
        return OrderKind.orDefault(kind);
    }
}
