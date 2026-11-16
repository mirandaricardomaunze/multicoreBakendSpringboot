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
        @Valid List<ReceiveLine> lines,
        @Size(max = 60, message = "A chave de idempotência não pode exceder 60 caracteres.")
        String idempotencyKey
) {
    public ReceiveStockTransferRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
        lines = lines == null ? List.of() : List.copyOf(lines);
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    /** Construtor retrocompatível: recepção integral, apenas com observações. */
    public ReceiveStockTransferRequest(String notes) {
        this(notes, null, null);
    }

    /** Construtor retrocompatível anterior à protecção de replay. */
    public ReceiveStockTransferRequest(String notes, List<ReceiveLine> lines) {
        this(notes, lines, null);
    }

    /**
     * Quantidade conferida no destino para uma linha da guia.
     *
     * @param lineId             linha da guia de transferência
     * @param receivedQuantity   quantidade que chegou e entra no destino (0 até ao enviado)
     * @param discrepancyReason  motivo obrigatório quando recebido &lt; enviado
     * @param lostInTransit      a diferença perdeu-se no transporte (partida, roubada): não volta à origem e é
     *                           abatida como quebra. Nulo ou falso: a diferença volta à origem
     */
    public record ReceiveLine(
            @NotNull(message = "Linha da guia em falta.") Long lineId,
            @NotNull(message = "Indique a quantidade recebida.")
            @PositiveOrZero(message = "A quantidade recebida não pode ser negativa.")
            BigDecimal receivedQuantity,
            @Size(max = 200, message = "O motivo da divergência não pode exceder 200 caracteres.")
            String discrepancyReason,
            Boolean lostInTransit
    ) {
        public ReceiveLine {
            discrepancyReason = mz.multicore.erp.architecture.validation.InputSanitizer
                    .sanitizeNotes(discrepancyReason, 200);
        }

        /** Construtor retrocompatível: a diferença volta à origem. */
        public ReceiveLine(Long lineId, BigDecimal receivedQuantity, String discrepancyReason) {
            this(lineId, receivedQuantity, discrepancyReason, null);
        }
    }
}
