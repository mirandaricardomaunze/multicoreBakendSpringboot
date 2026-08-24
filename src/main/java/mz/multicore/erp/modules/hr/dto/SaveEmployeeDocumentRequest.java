package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * @param expiryDate nulo é uma resposta legítima — significa <b>não caduca</b> (NUIT, BI vitalício),
 *                   e não "ainda não preenchi". É por isso que não é obrigatório.
 */
public record SaveEmployeeDocumentRequest(
        @NotNull(message = "Indique o colaborador.") Long employeeId,
        @NotBlank(message = "Indique o tipo de documento (BI, DIRE, PASSAPORTE, NUIT, …).")
        String documentType,
        String documentNumber,
        LocalDate issueDate,
        LocalDate expiryDate,
        String notes
) {}
