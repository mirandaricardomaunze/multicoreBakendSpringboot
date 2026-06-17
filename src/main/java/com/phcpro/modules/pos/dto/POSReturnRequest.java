package com.phcpro.modules.pos.dto;

import com.phcpro.modules.comercial.dto.CreateCreditNoteLineRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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
        @Valid List<CreateCreditNoteLineRequest> lines
) {}
