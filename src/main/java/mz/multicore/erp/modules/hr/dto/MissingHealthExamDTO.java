package mz.multicore.erp.modules.hr.dto;

import java.time.LocalDate;

/**
 * Trabalhador no activo <b>sem exame de aptidão nenhum</b>.
 *
 * <p>É a falha de conformidade que não se vê: a lista de exames a caducar só conhece quem já fez
 * exame, pelo que quem nunca fez é exactamente quem nunca aparece em aviso nenhum. Perante a
 * inspecção do trabalho, é também o caso mais grave dos dois.
 */
public record MissingHealthExamDTO(
        Long employeeId, String employeeName, String role, String department,
        LocalDate hireDate, Long daysSinceHire
) {}
