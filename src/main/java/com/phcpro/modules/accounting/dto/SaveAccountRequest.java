package com.phcpro.modules.accounting.dto;

import com.phcpro.modules.accounting.model.AccountNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAccountRequest(
        @NotBlank(message = "O código da conta é obrigatório.")
        @Size(max = 20) String code,

        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 200) String name,

        /** Vazio = natureza habitual da classe do código. */
        AccountNature nature,

        boolean postable
) {}
