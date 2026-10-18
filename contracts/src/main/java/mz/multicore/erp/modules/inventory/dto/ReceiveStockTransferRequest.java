package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Confirmação de recepção de uma guia em trânsito pelo armazém de destino.
 *
 * <p>{@code lines} é opcional: nulo ou vazio = recepção integral (comportamento anterior). Cada
 * linha indicada declara a quantidade fisicamente recebida; a diferença volta à origem.
 * Ver {@code docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md}.
 */
public record ReceiveStockTransferRequest(
        @Size(max = 500, message = "As observações da recepção não podem exceder 500 caracteres.")
        String notes,
        @Valid List<ReceiveLine> lines
) {
    public ReceiveStockTransferRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** Construtor retrocompatível: recepção integral, apenas com observações. */
    public ReceiveStockTransferRequest(String notes) {
        this(notes, null);
    }

    /**
     * Quantidade conferida no destino para uma linha da guia.
     *
     * @param lineId             linha da guia de transferência
     * @param receivedQuantity   quantidade que chegou e entra no destino (0 até ao enviado)
     * @param discrepancyReason  motivo obrigatório quando recebido &lt; enviado
     */
    public record ReceiveLine(
            @NotNull(message = "Linha da guia em falta.") Long lineId,
            @NotNull(message = "Indique a quantidade recebida.")
            @PositiveOrZero(message = "A quantidade recebida não pode ser negativa.")
            BigDecimal receivedQuantity,
            @Size(max = 200, message = "O motivo da divergência não pode exceder 200 caracteres.")
            String discrepancyReason
    ) {
        public ReceiveLine {
            discrepancyReason = mz.multicore.erp.architecture.validation.InputSanitizer
                    .sanitizeNotes(discrepancyReason, 200);
        }
    }
}
