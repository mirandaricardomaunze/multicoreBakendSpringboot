package mz.multicore.erp.modules.purchases.dto;

import mz.multicore.erp.architecture.validation.ValidNuit;
import mz.multicore.erp.architecture.validation.ValidPhoneMZ;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "Nome do fornecedor é obrigatório.")
        @Size(max = 200, message = "O nome do fornecedor não pode exceder 200 caracteres.")
        String name,

        @NotBlank(message = "NUIT/NIF é obrigatório.")
        @ValidNuit
        String taxId,

        @Email(message = "Email inválido.")
        @Size(max = 200, message = "O email não pode exceder 200 caracteres.")
        String email,

        @Size(max = 300, message = "O endereço não pode exceder 300 caracteres.")
        String address,

        @ValidPhoneMZ
        String phone,

        @Size(max = 100, message = "A pessoa de contacto não pode exceder 100 caracteres.")
        String contactPerson,

        @NotNull(message = "Empresa é obrigatória.")
        Long companyId
) {}
