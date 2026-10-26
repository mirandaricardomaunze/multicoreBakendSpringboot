package mz.multicore.erp.modules.pos.dto;

import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record POSReturnRequest(
        @NotBlank(message = "Operador é obrigatório.") String operator,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Fatura é obrigatória.") Long invoiceId,
        @NotNull(message = "Armazém é obrigatório para devolução de stock.") Long warehouseId,
        @NotBlank(message = "Motivo da devolução é obrigatório.") String reason,
        @NotBlank(message = "Método de reembolso é obrigatório.") String refundMethod,
        Long treasuryAccountId,
        @NotEmpty(message = "A devolução deve conter pelo menos uma linha.")
        @Valid List<CreateCreditNoteLineRequest> lines,
        @Size(max = 60, message = "A chave de idempotência não pode exceder 60 caracteres.")
        String idempotencyKey
) {
    public POSReturnRequest {
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    /** Construtor retrocompatível anterior à protecção de devoluções contra replay. */
    public POSReturnRequest(String operator, Long companyId, Long invoiceId, Long warehouseId,
                            String reason, String refundMethod, Long treasuryAccountId,
                            List<CreateCreditNoteLineRequest> lines) {
        this(operator, companyId, invoiceId, warehouseId, reason, refundMethod,
                treasuryAccountId, lines, null);
    }
}
