package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record POSCheckoutRequest(
        @NotBlank(message = "Operador é obrigatório.") String operator,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        /** Opcional. Se nulo, a venda é para cliente não cadastrado e o campo walkInName torna-se obrigatório. */
        Long clientId,
        /** Obrigatório quando {@code clientId} é nulo. Nome do cliente/comprador para o recibo e fatura. */
        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        /** Compat antigo: pagamento único em conta de tesouraria. Use {@link #payments()} para multi-método. */
        Long treasuryAccountId,
        @NotEmpty(message = "A venda deve conter pelo menos uma linha.") @Valid List<POSCheckoutLineRequest> lines,
        /** Multi-método: lista de pagamentos (CASH/CARD/BANK_TRANSFER/CREDIT). Se vazio, usa-se treasuryAccountId. */
        @Valid List<PosPaymentRequest> payments,
        /** Referência única de contingência (ex.: CONT-20260917-192000-A1B2) para idempotência e rastreio. */
        @Size(max = 60, message = "Referência de contingência não pode exceder 60 caracteres.")
        String contingencyReference,
        /** Cotação de origem (opcional). Se preenchida, o checkout marca a cotação como convertida. */
        Long quotationId
) {
    /** Construtor retrocompatível com referência de contingência sem cotação. */
    public POSCheckoutRequest(String operator, Long companyId, Long clientId, String walkInName,
                              Long warehouseId, Long treasuryAccountId,
                              List<POSCheckoutLineRequest> lines, List<PosPaymentRequest> payments,
                              String contingencyReference) {
        this(operator, companyId, clientId, walkInName, warehouseId, treasuryAccountId, lines, payments, contingencyReference, null);
    }

    /** Construtor de conveniência para checkout associando cotação de origem sem contingência. */
    public POSCheckoutRequest(String operator, Long companyId, Long clientId, String walkInName,
                              Long warehouseId, Long treasuryAccountId,
                              List<POSCheckoutLineRequest> lines, List<PosPaymentRequest> payments,
                              Long quotationId) {
        this(operator, companyId, clientId, walkInName, warehouseId, treasuryAccountId, lines, payments, null, quotationId);
    }

    /** Construtor de conveniência para compatibilidade com versões e testes anteriores. */
    public POSCheckoutRequest(String operator, Long companyId, Long clientId, String walkInName,
                              Long warehouseId, Long treasuryAccountId,
                              List<POSCheckoutLineRequest> lines, List<PosPaymentRequest> payments) {
        this(operator, companyId, clientId, walkInName, warehouseId, treasuryAccountId, lines, payments, null, null);
    }
}
