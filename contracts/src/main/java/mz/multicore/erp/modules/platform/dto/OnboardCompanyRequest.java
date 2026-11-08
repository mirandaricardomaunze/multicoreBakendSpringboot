package mz.multicore.erp.modules.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;

/**
 * Complete onboarding of a company in one transaction: the company, its first administrator,
 * the subscription and, optionally, the bank account printed on its invoices. Either everything
 * is created or nothing is, so a company never stays without an administrator.
 *
 * <p>A {@code null} subscription starts a {@value #DEFAULT_TRIAL_DAYS}-day trial.
 */
public record OnboardCompanyRequest(
        @NotNull(message = "Indique os dados da empresa.")
        @Valid
        CreateCompanyRequest company,

        @NotNull(message = "Indique o administrador da empresa.")
        @Valid
        Administrator administrator,

        @Valid
        SaveSubscriptionRequest subscription,

        @Valid
        BankAccount bankAccount
) {

    public static final int DEFAULT_TRIAL_DAYS = 30;

    /** First user of the company, created with the ADMIN role. */
    public record Administrator(
            @NotBlank(message = "O nome de utilizador do administrador é obrigatório.")
            @Size(min = 3, max = 50, message = "O nome de utilizador deve ter entre 3 e 50 caracteres.")
            String username,

            @NotBlank(message = "O nome do administrador é obrigatório.")
            @Size(max = 120, message = "O nome do administrador não pode exceder 120 caracteres.")
            String name,

            @NotBlank(message = "A senha do administrador é obrigatória.")
            String password
    ) {}

    /** Bank account shown on invoices for payment by transfer. */
    public record BankAccount(
            @NotBlank(message = "Indique o banco.")
            @Size(max = 100, message = "O nome do banco não pode exceder 100 caracteres.")
            String bankName,

            @NotBlank(message = "Indique o número da conta ou NIB.")
            @Size(max = 60, message = "O número da conta não pode exceder 60 caracteres.")
            String accountNumber
    ) {}
}
