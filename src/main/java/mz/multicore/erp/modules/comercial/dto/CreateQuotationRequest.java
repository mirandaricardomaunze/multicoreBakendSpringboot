package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido para emitir uma cotação. Como na encomenda, o cliente é opcional — sem {@code clientId} a
 * proposta fica para "Consumidor Final" e {@code walkInName} serve de rótulo, sem criar registo.
 *
 * <p>A validade entra em <b>dias</b> ("válida por 30 dias", que é como se cota); o documento grava
 * a <b>data</b> resultante. Prolongar depois é acto próprio e explícito
 * ({@code ExtendQuotationValidityRequest}), com permissão de gerente.
 */
public record CreateQuotationRequest(
        Long clientId,

        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,

        @NotNull(message = "O ID da empresa é obrigatório.") Long companyId,
        @NotNull(message = "O ID do armazém é obrigatório.") Long warehouseId,

        /** Dias de validade. Ausente = {@code QuotationValidity.DEFAULT_DAYS}. */
        @Positive(message = "A validade deve ser de pelo menos um dia.")
        Integer validityDays,

        @Size(max = 200, message = "Condições de pagamento devem ter no máximo 200 caracteres.")
        String paymentTerms,

        @Size(max = 200, message = "Prazo de entrega deve ter no máximo 200 caracteres.")
        String deliveryTerms,

        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String notes,

        @NotEmpty(message = "A cotação deve conter pelo menos uma linha.") @Valid
        List<CreateQuotationLineRequest> lines
) {}
