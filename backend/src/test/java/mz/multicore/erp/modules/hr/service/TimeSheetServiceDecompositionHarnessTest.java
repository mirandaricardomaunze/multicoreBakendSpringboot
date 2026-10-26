package mz.multicore.erp.modules.hr.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular - TimeSheetService (Fase 99)")
class TimeSheetServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/hr/service/TimeSheetService.java");
    private static final Path REGISTRATION_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/hr/service/TimeEntryRegistrationService.java");
    private static final Path CALCULATOR_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/hr/service/TimeSheetCalculator.java");
    private static final Path COORDINATOR_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/hr/service/TimeSheetPeriodCoordinator.java");

    @Test
    @DisplayName("Garante limites físicos estritos de linhas de código em todos os componentes")
    void shouldAdhereToStrictPhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_PATH).count();
        long registrationLines = Files.lines(REGISTRATION_PATH).count();
        long calculatorLines = Files.lines(CALCULATOR_PATH).count();
        long coordinatorLines = Files.lines(COORDINATOR_PATH).count();

        assertTrue(serviceLines <= 180,
                "TimeSheetService.java deve ter <= 180 linhas, mas tem: " + serviceLines);
        assertTrue(registrationLines <= 160,
                "TimeEntryRegistrationService.java deve ter <= 160 linhas, mas tem: " + registrationLines);
        assertTrue(calculatorLines <= 195,
                "TimeSheetCalculator.java deve ter <= 195 linhas, mas tem: " + calculatorLines);
        assertTrue(coordinatorLines <= 195,
                "TimeSheetPeriodCoordinator.java deve ter <= 195 linhas, mas tem: " + coordinatorLines);
    }

    @Test
    @DisplayName("Garante separação de responsabilidades e delegação no orquestrador")
    void shouldMaintainDelegationPattern() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("registrationService.recordEntry"),
                "TimeSheetService deve delegar recordEntry para TimeEntryRegistrationService");
        assertTrue(content.contains("calculator.getMonthlySheet"),
                "TimeSheetService deve delegar getMonthlySheet para TimeSheetCalculator");
        assertTrue(content.contains("periodCoordinator.closePeriod"),
                "TimeSheetService deve delegar closePeriod para TimeSheetPeriodCoordinator");
        assertTrue(content.contains("PENDING_JUSTIFICATION = \"PENDING_JUSTIFICATION\""),
                "Constante canónica PENDING_JUSTIFICATION deve ser mantida");
    }

    @Test
    @DisplayName("Verifica ausência de chamadas proibidas e UI no backend")
    void shouldNotContainForbiddenCalls() throws IOException {
        List<Path> paths = List.of(SERVICE_PATH, REGISTRATION_PATH, CALCULATOR_PATH, COORDINATOR_PATH);
        for (Path path : paths) {
            String content = Files.readString(path);
            assertFalse(content.contains("JOptionPane"),
                    path.getFileName() + " não pode conter JOptionPane");
            assertFalse(content.contains("javax.swing"),
                    path.getFileName() + " não pode conter Swing");
            assertFalse(content.contains("java.awt"),
                    path.getFileName() + " não pode conter AWT");
        }
    }

    @Test
    @DisplayName("Verifica que os colaboradores são componentes Spring anotados com @Service")
    void shouldBeProperSpringServices() throws IOException {
        assertTrue(Files.readString(REGISTRATION_PATH).contains("@Service"),
                "TimeEntryRegistrationService deve ser @Service");
        assertTrue(Files.readString(CALCULATOR_PATH).contains("@Service"),
                "TimeSheetCalculator deve ser @Service");
        assertTrue(Files.readString(COORDINATOR_PATH).contains("@Service"),
                "TimeSheetPeriodCoordinator deve ser @Service");
    }
}
