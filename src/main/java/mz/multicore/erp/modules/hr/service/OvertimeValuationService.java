package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.OvertimeValuationDTO;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.model.OvertimeRateConfig;
import mz.multicore.erp.modules.hr.model.OvertimeTier;
import mz.multicore.erp.modules.hr.repository.OvertimeRateConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Transforma horas extra apuradas em valor a pagar. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>Serviço próprio porque são duas responsabilidades distintas: o {@code TimeSheetService} conta
 * horas, este atribui-lhes preço. Contar não depende de legislação nenhuma; valorizar depende toda.
 *
 * <p><b>Sem configuração de acréscimos, recusa-se a valorizar</b> — e diz porquê. A alternativa era
 * multiplicar por um número inventado, que parece certo e paga mal.
 */
@Service
public class OvertimeValuationService {

    private final TimeSheetService timeSheetService;
    private final OvertimeRateConfigRepository configRepository;

    public OvertimeValuationService(TimeSheetService timeSheetService,
                                    OvertimeRateConfigRepository configRepository) {
        this.timeSheetService = timeSheetService;
        this.configRepository = configRepository;
    }

    /**
     * Valor das horas extra de um colaborador num mês, a partir da folha de ponto.
     * Devolve vazio quando não há folha para o colaborador nesse mês.
     */
    @Transactional(readOnly = true)
    public Optional<OvertimeValuationDTO> valueFor(Long employeeId, int year, int month, BigDecimal baseSalary) {
        Optional<TimeSheetDTO.TimeSheetLineDTO> line =
                timeSheetService.findEmployeeLine(employeeId, year, month);
        if (line.isEmpty()) {
            return Optional.empty();
        }
        TimeSheetDTO.TimeSheetLineDTO sheet = line.get();
        BigDecimal totalExtra = sheet.overtimeDayHours()
                .add(sheet.overtimeNightHours())
                .add(sheet.restDayHours());
        if (totalExtra.signum() == 0) {
            return Optional.of(new OvertimeValuationDTO(
                    sheet.overtimeDayHours(), sheet.overtimeNightHours(), sheet.restDayHours(),
                    BigDecimal.ZERO, null, null));
        }

        OvertimeRateConfig config = activeConfig(LocalDate.of(year, month, 1));
        BigDecimal hourlyRate = hourlyRate(baseSalary, sheet.expectedHours());

        BigDecimal amount = tierAmount(hourlyRate, sheet.overtimeDayHours(), config, OvertimeTier.DIURNA)
                .add(tierAmount(hourlyRate, sheet.overtimeNightHours(), config, OvertimeTier.NOCTURNA))
                .add(tierAmount(hourlyRate, sheet.restDayHours(), config, OvertimeTier.DESCANSO));

        return Optional.of(new OvertimeValuationDTO(
                sheet.overtimeDayHours(), sheet.overtimeNightHours(), sheet.restDayHours(),
                amount.setScale(2, RoundingMode.HALF_UP), config.getName(), config.getLegalBasis()));
    }

    /**
     * O valor/hora normal. Sai do salário acordado a dividir pelas horas <b>previstas nesse mês</b>,
     * e não por um divisor fixo: um mês de 22 dias úteis e outro de 20 não valem a hora ao mesmo
     * preço, e é o horário que já diz quantas horas o mês tinha.
     */
    private BigDecimal hourlyRate(BigDecimal baseSalary, BigDecimal expectedHours) {
        if (baseSalary == null || baseSalary.signum() <= 0) {
            throw new BusinessRuleException(
                    "Não é possível valorizar horas extra sem salário base definido no contrato.");
        }
        if (expectedHours == null || expectedHours.signum() <= 0) {
            throw new BusinessRuleException(
                    "O horário de trabalho não prevê horas neste mês — não há valor/hora que apurar.");
        }
        return baseSalary.divide(expectedHours, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal tierAmount(BigDecimal hourlyRate, BigDecimal hours,
                                  OvertimeRateConfig config, OvertimeTier tier) {
        if (hours == null || hours.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return hourlyRate.multiply(hours).multiply(config.multiplierFor(tier));
    }

    /**
     * A configuração que vigora na data. Não existir é um erro que se diz em voz alta: os
     * multiplicadores legais têm de ser confirmados com o contabilista, e o sistema não os adivinha.
     */
    private OvertimeRateConfig activeConfig(LocalDate date) {
        return configRepository
                .findByCompanyIdOrderByEffectiveFromDesc(CurrentUserContext.requireCurrentCompanyId())
                .stream()
                .filter(config -> config.coversDate(date))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Há horas extra apuradas mas não existe configuração de acréscimos em vigor. "
                                + "Configure os multiplicadores em RH → Acréscimos de Hora Extra "
                                + "(os valores devem ser confirmados com o contabilista da empresa)."));
    }
}
