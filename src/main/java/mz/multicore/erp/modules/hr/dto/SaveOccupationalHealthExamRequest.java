package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SaveOccupationalHealthExamRequest(
        @NotNull(message = "Indique o trabalhador.") Long employeeId,
        String cardNumber,
        @NotNull(message = "Indique a data do exame.") LocalDate examDate,
        @NotNull(message = "Indique a validade do exame.") LocalDate expiryDate,
        @NotBlank(message = "Indique o resultado de aptidão.") String fitnessResult,
        String clinic,
        String doctorName,
        String restrictions,
        String notes,
        String attachmentName,
        @Size(max = 5_000_000, message = "O comprovativo não pode exceder 5 MB.") byte[] attachmentData
) {}
