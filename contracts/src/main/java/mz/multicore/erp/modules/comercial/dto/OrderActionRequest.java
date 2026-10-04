package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.Size;

public record OrderActionRequest(
        @Size(max = 500, message = "O motivo não pode exceder 500 caracteres.")
        String reason,

        @Size(max = 100, message = "O nome do terminal não pode exceder 100 caracteres.")
        String terminalName
) {}
