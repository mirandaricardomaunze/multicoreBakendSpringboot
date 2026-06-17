package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveClientRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 40) String taxId,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 300) String address
) {}
