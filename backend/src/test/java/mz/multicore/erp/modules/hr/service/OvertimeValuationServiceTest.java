package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.model.OvertimeRateConfig;
import mz.multicore.erp.modules.hr.repository.OvertimeRateConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A valorização das horas extra. O caso que carrega esta classe é o <b>primeiro</b>: sem
 * configuração de acréscimos, o sistema recusa-se a valorizar e diz porquê. A alternativa —
 * multiplicar por um número inventado — parece certa e paga mal.
 */
class OvertimeValuationServiceTest {

    private static final Long COMPANY = 7L;
    private static final int YEAR = 2026;
    private static final int MONTH = 6;
    /** 176 horas previstas no mês → com 35.200 de salário, a hora normal vale 200,00. */
    private static final BigDecimal SALARY = new BigDecimal("35200.00");

    private TimeSheetService timeSheetService;
    private OvertimeRateConfigRepository configRepository;
    private OvertimeValuationService service;

    @BeforeEach
    void setUp() {
        timeSheetService = mock(TimeSheetService.class);
        configRepository = mock(OvertimeRateConfigRepository.class);
        service = new OvertimeValuationService(timeSheetService, configRepository);
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void withoutRateConfig_refusesAndSaysWhatToDo() {
        // O ponto do bloco: nada de multiplicadores por omissão. A spec (§6) diz que os acréscimos
        // legais têm de ser confirmados com o contabilista — o sistema não os adivinha.
        withLine(new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(configRepository.findByCompanyIdOrderByEffectiveFromDesc(COMPANY)).thenReturn(List.of());

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.valueFor(5L, YEAR, MONTH, SALARY));
        assertTrue(ex.getMessage().contains("não existe configuração de acréscimos"));
        assertTrue(ex.getMessage().contains("contabilista"), "a recusa diz o que fazer a seguir");
    }

    @Test
    void withoutOvertimeHours_needsNoConfigAtAll() {
        // Sem horas extra não há nada a valorizar: exigir configuração aqui seria obrigar uma loja
        // que nunca faz horas extra a configurar acréscimos que não usa.
        withLine(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        var valuation = service.valueFor(5L, YEAR, MONTH, SALARY).orElseThrow();

        assertEquals(0, BigDecimal.ZERO.compareTo(valuation.amount()));
    }

    @Test
    void valuesEachTierWithItsOwnMultiplier() {
        // 4h diurnas ×1,5 + 2h nocturnas ×2 + 8h de descanso ×2 sobre uma hora de 200,00:
        // 1200 + 800 + 3200 = 5200,00. Somar as horas num total só perderia esta distinção.
        withLine(new BigDecimal("4.00"), new BigDecimal("2.00"), new BigDecimal("8.00"));
        withConfig(new BigDecimal("1.5"), new BigDecimal("2.0"), new BigDecimal("2.0"));

        var valuation = service.valueFor(5L, YEAR, MONTH, SALARY).orElseThrow();

        assertEquals(0, new BigDecimal("5200.00").compareTo(valuation.amount()));
        assertEquals("Acréscimos 2026", valuation.configName());
        assertEquals("Confirmado com o contabilista", valuation.legalBasis(),
                "o recibo tem de saber contra o quê se confere");
    }

    @Test
    void configOutsideItsValidityWindow_isNotUsed() {
        withLine(new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        OvertimeRateConfig expired = config(new BigDecimal("1.5"), new BigDecimal("2.0"), new BigDecimal("2.0"));
        expired.setEffectiveTo(LocalDate.of(2025, 12, 31));
        when(configRepository.findByCompanyIdOrderByEffectiveFromDesc(COMPANY)).thenReturn(List.of(expired));

        assertThrows(BusinessRuleException.class, () -> service.valueFor(5L, YEAR, MONTH, SALARY));
    }

    @Test
    void inactiveConfig_isNotUsed() {
        withLine(new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        OvertimeRateConfig inactive = config(new BigDecimal("1.5"), new BigDecimal("2.0"), new BigDecimal("2.0"));
        inactive.setActive(false);
        when(configRepository.findByCompanyIdOrderByEffectiveFromDesc(COMPANY)).thenReturn(List.of(inactive));

        assertThrows(BusinessRuleException.class, () -> service.valueFor(5L, YEAR, MONTH, SALARY));
    }

    @Test
    void withoutBaseSalary_refuses() {
        withLine(new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        withConfig(new BigDecimal("1.5"), new BigDecimal("2.0"), new BigDecimal("2.0"));

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.valueFor(5L, YEAR, MONTH, BigDecimal.ZERO));
        assertTrue(ex.getMessage().contains("salário base"));
    }

    @Test
    void withoutTimeSheetLine_returnsEmpty() {
        when(timeSheetService.findEmployeeLine(5L, YEAR, MONTH)).thenReturn(Optional.empty());

        assertTrue(service.valueFor(5L, YEAR, MONTH, SALARY).isEmpty());
    }

    private void withLine(BigDecimal day, BigDecimal night, BigDecimal rest) {
        when(timeSheetService.findEmployeeLine(5L, YEAR, MONTH)).thenReturn(Optional.of(
                new TimeSheetDTO.TimeSheetLineDTO(5L, "Maria", 22, 22, 0,
                        new BigDecimal("176.00"), new BigDecimal("176.00"), new BigDecimal("176.00"),
                        day, night, rest, 0)));
    }

    private void withConfig(BigDecimal day, BigDecimal night, BigDecimal rest) {
        when(configRepository.findByCompanyIdOrderByEffectiveFromDesc(COMPANY))
                .thenReturn(List.of(config(day, night, rest)));
    }

    private OvertimeRateConfig config(BigDecimal day, BigDecimal night, BigDecimal rest) {
        OvertimeRateConfig config = new OvertimeRateConfig();
        config.setName("Acréscimos 2026");
        config.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        config.setDayMultiplier(day);
        config.setNightMultiplier(night);
        config.setRestDayMultiplier(rest);
        config.setLegalBasis("Confirmado com o contabilista");
        config.setActive(true);
        return config;
    }
}
