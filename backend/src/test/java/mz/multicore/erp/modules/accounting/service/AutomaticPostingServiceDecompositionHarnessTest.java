package mz.multicore.erp.modules.accounting.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular - AutomaticPostingService (Fase 101)")
class AutomaticPostingServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/accounting/service/AutomaticPostingService.java");
    private static final Path COMMERCIAL_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/accounting/service/CommercialPostingHandler.java");
    private static final Path PROCUREMENT_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/accounting/service/ProcurementPostingHandler.java");
    private static final Path OPERATIONS_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/accounting/service/OperationsPostingHandler.java");
    private static final Path HELPER_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/accounting/service/PostingHelper.java");

    @Test
    @DisplayName("Garante limites físicos estritos de linhas de código em todos os componentes")
    void shouldAdhereToStrictPhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_PATH).count();
        long commercialLines = Files.lines(COMMERCIAL_PATH).count();
        long procurementLines = Files.lines(PROCUREMENT_PATH).count();
        long operationsLines = Files.lines(OPERATIONS_PATH).count();
        long helperLines = Files.lines(HELPER_PATH).count();

        assertTrue(serviceLines <= 160,
                "AutomaticPostingService.java deve ter <= 160 linhas, mas tem: " + serviceLines);
        assertTrue(commercialLines <= 195,
                "CommercialPostingHandler.java deve ter <= 195 linhas, mas tem: " + commercialLines);
        assertTrue(procurementLines <= 140,
                "ProcurementPostingHandler.java deve ter <= 140 linhas, mas tem: " + procurementLines);
        assertTrue(operationsLines <= 195,
                "OperationsPostingHandler.java deve ter <= 195 linhas, mas tem: " + operationsLines);
        assertTrue(helperLines <= 95,
                "PostingHelper.java deve ter <= 95 linhas, mas tem: " + helperLines);
    }

    @Test
    @DisplayName("Garante anotações de evento e delegação no orquestrador")
    void shouldMaintainDelegationAndEventListeners() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("@EventListener"),
                "AutomaticPostingService deve conter anotações @EventListener");
        assertTrue(content.contains("commercialPostingHandler.postSale"),
                "AutomaticPostingService deve delegar postSale para CommercialPostingHandler");
        assertTrue(content.contains("procurementPostingHandler.postPurchase"),
                "AutomaticPostingService deve delegar postPurchase para ProcurementPostingHandler");
        assertTrue(content.contains("operationsPostingHandler.postPayslip"),
                "AutomaticPostingService deve delegar postPayslip para OperationsPostingHandler");
    }

    @Test
    @DisplayName("Garante isolamento arquitectural de RH na Contabilidade")
    void shouldMaintainArchitecturalDecouplingFromHR() throws IOException {
        List<Path> paths = List.of(SERVICE_PATH, COMMERCIAL_PATH, PROCUREMENT_PATH, OPERATIONS_PATH, HELPER_PATH);
        for (Path path : paths) {
            String content = Files.readString(path);
            assertFalse(content.contains("mz.multicore.erp.modules.hr.model"),
                    path.getFileName() + " não pode importar modelo de RH");
            assertFalse(content.contains("mz.multicore.erp.modules.hr.repository"),
                    path.getFileName() + " não pode importar repositório de RH");
            assertFalse(content.contains("mz.multicore.erp.modules.hr.service"),
                    path.getFileName() + " não pode importar serviço de RH");
            assertFalse(content.contains("JOptionPane"),
                    path.getFileName() + " não pode conter JOptionPane");
        }
    }

    @Test
    @DisplayName("Verifica que os colaboradores são componentes Spring anotados")
    void shouldBeProperSpringComponents() throws IOException {
        assertTrue(Files.readString(COMMERCIAL_PATH).contains("@Service"),
                "CommercialPostingHandler deve ser @Service");
        assertTrue(Files.readString(PROCUREMENT_PATH).contains("@Service"),
                "ProcurementPostingHandler deve ser @Service");
        assertTrue(Files.readString(OPERATIONS_PATH).contains("@Service"),
                "OperationsPostingHandler deve ser @Service");
        assertTrue(Files.readString(HELPER_PATH).contains("@Component"),
                "PostingHelper deve ser @Component");
    }
}
