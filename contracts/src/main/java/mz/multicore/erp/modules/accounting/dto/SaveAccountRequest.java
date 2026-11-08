package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.AccountNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAccountRequest(
        @NotBlank(message = "O código da conta é obrigatório.")
        @Size(max = 20) String code,

        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 200) String name,

        /** Blank uses the usual nature of the code's account class. */
        AccountNature nature,

        boolean postable
) {}
