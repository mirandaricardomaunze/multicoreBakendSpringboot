package mz.multicore.erp.modules.performance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular - SalesGoalService (Fase 102)")
class SalesGoalServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/performance/service/SalesGoalService.java");
    private static final Path LIFECYCLE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/performance/service/SalesGoalLifecycleService.java");
    private static final Path PROGRESS_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/performance/service/SalesGoalProgressCoordinator.java");
    private static final Path BONUS_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/performance/service/SalesGoalBonusService.java");
    private static final Path QUERY_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/performance/service/SalesGoalQueryService.java");

    @Test
    @DisplayName("Garante limites físicos estritos de linhas de código em todos os componentes")
    void shouldAdhereToStrictPhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_PATH).count();
        long lifecycleLines = Files.lines(LIFECYCLE_PATH).count();
        long progressLines = Files.lines(PROGRESS_PATH).count();
        long bonusLines = Files.lines(BONUS_PATH).count();
        long queryLines = Files.lines(QUERY_PATH).count();

        assertTrue(serviceLines <= 160,
                "SalesGoalService.java deve ter <= 160 linhas, mas tem: " + serviceLines);
        assertTrue(lifecycleLines <= 215,
                "SalesGoalLifecycleService.java deve ter <= 215 linhas, mas tem: " + lifecycleLines);
        assertTrue(progressLines <= 130,
                "SalesGoalProgressCoordinator.java deve ter <= 130 linhas, mas tem: " + progressLines);
        assertTrue(bonusLines <= 240,
                "SalesGoalBonusService.java deve ter <= 240 linhas, mas tem: " + bonusLines);
        assertTrue(queryLines <= 95,
                "SalesGoalQueryService.java deve ter <= 95 linhas, mas tem: " + queryLines);
    }

    @Test
    @DisplayName("Garante delegação no orquestrador")
    void shouldMaintainDelegationPattern() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("lifecycleService.createGoal"),
                "SalesGoalService deve delegar createGoal para SalesGoalLifecycleService");
        assertTrue(content.contains("lifecycleService.updateGoal"),
                "SalesGoalService deve delegar updateGoal para SalesGoalLifecycleService");
        assertTrue(content.contains("progressCoordinator.getGoalProgress"),
                "SalesGoalService deve delegar getGoalProgress para SalesGoalProgressCoordinator");
        assertTrue(content.contains("bonusService.approveBonus"),
                "SalesGoalService deve delegar approveBonus para SalesGoalBonusService");
        assertTrue(content.contains("queryService.listGoals"),
                "SalesGoalService deve delegar listGoals para SalesGoalQueryService");
    }

    @Test
    @DisplayName("Verifica ausência de chamadas proibidas e UI no backend")
    void shouldNotContainForbiddenCalls() throws IOException {
        List<Path> paths = List.of(SERVICE_PATH, LIFECYCLE_PATH, PROGRESS_PATH, BONUS_PATH, QUERY_PATH);
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
        assertTrue(Files.readString(LIFECYCLE_PATH).contains("@Service"),
                "SalesGoalLifecycleService deve ser @Service");
        assertTrue(Files.readString(PROGRESS_PATH).contains("@Service"),
                "SalesGoalProgressCoordinator deve ser @Service");
        assertTrue(Files.readString(BONUS_PATH).contains("@Service"),
                "SalesGoalBonusService deve ser @Service");
        assertTrue(Files.readString(QUERY_PATH).contains("@Service"),
                "SalesGoalQueryService deve ser @Service");
    }
}
