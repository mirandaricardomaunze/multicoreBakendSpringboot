package mz.multicore.erp.modules.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creates ({@code id=null}) or updates a client's project. */
public record SaveCustomerProjectRequest(
        Long id,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Cliente é obrigatório.") Long clientId,
        @NotBlank(message = "O nome da obra é obrigatório.") @Size(max = 150) String name,
        @Size(max = 300) String address,
        @Size(max = 150) String contact,
        boolean active
) {}
