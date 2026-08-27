package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderTerms;

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
        OrderKind kind,

        // ─── Condições acordadas (opcionais; ver docs/ENCOMENDA_PROFISSIONAL_SPEC.md) ───
        @Size(max = 200, message = "Condições de pagamento devem ter no máximo 200 caracteres.")
        String paymentTerms,

        @Size(max = 200, message = "Prazo de entrega deve ter no máximo 200 caracteres.")
        String deliveryTerms,

        /** Dias de entrega a contar de hoje. Ausente = encomenda sem data de entrega prometida. */
        @Positive(message = "O prazo de entrega deve ser de pelo menos um dia.")
        Integer deliveryDays,

        /**
         * Armazém que recebe, na reposição interna — a loja que pediu. Obrigatório nessa via e
         * ignorado nas outras. Ver {@code docs/REPOSICAO_INTERNA_SPEC.md}.
         */
        Long destinationWarehouseId
) {
    /** Construtor retrocompatível: quem não declara a via fica na encomenda A4 formal. */
    public CreateOrderRequest(Long clientId, String walkInName, Long companyId, Long warehouseId,
                              List<CreateInvoiceLineRequest> lines) {
        this(clientId, walkInName, companyId, warehouseId, lines, OrderKind.FORMAL_ORDER);
    }

    /** Construtor retrocompatível de quem declara a via mas não as condições. */
    public CreateOrderRequest(Long clientId, String walkInName, Long companyId, Long warehouseId,
                              List<CreateInvoiceLineRequest> lines, OrderKind kind) {
        this(clientId, walkInName, companyId, warehouseId, lines, kind, null, null, null, null);
    }

    /** Reposição interna: origem, destino e artigos; sem condições comerciais nem cliente. */
    public static CreateOrderRequest replenishment(Long companyId, Long originWarehouseId,
                                                   Long destinationWarehouseId,
                                                   List<CreateInvoiceLineRequest> lines) {
        return new CreateOrderRequest(null, null, companyId, originWarehouseId, lines,
                OrderKind.INTERNAL_REPLENISHMENT, null, null, null, destinationWarehouseId);
    }

    public OrderKind effectiveKind() {
        return OrderKind.orDefault(kind);
    }

    /**
     * Condições declaradas neste pedido. Sem origem — uma encomenda criada à mão não vem de cotação
     * nenhuma; quem tem origem é a conversão, que usa {@code Quotation.agreedTerms()}.
     */
    public OrderTerms agreedTerms() {
        return new OrderTerms(null, null, paymentTerms, deliveryTerms, deliveryDays);
    }
}
