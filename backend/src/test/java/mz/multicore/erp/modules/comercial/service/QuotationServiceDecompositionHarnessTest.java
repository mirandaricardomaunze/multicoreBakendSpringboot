package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Decomposição Modular - QuotationService (Fase 100)")
class QuotationServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/comercial/service/QuotationService.java");
    private static final Path LIFECYCLE_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/comercial/service/QuotationLifecycleService.java");
    private static final Path CONVERSION_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/comercial/service/QuotationConversionService.java");
    private static final Path QUERY_PATH = Paths.get(
            "src/main/java/mz/multicore/erp/modules/comercial/service/QuotationQueryService.java");

    @Test
    @DisplayName("Garante limites físicos estritos de linhas de código em todos os componentes")
    void shouldAdhereToStrictPhysicalLineLimits() throws IOException {
        long serviceLines = Files.lines(SERVICE_PATH).count();
        long lifecycleLines = Files.lines(LIFECYCLE_PATH).count();
        long conversionLines = Files.lines(CONVERSION_PATH).count();
        long queryLines = Files.lines(QUERY_PATH).count();

        assertTrue(serviceLines <= 160,
                "QuotationService.java deve ter <= 160 linhas, mas tem: " + serviceLines);
        assertTrue(lifecycleLines <= 295,
                "QuotationLifecycleService.java deve ter <= 295 linhas, mas tem: " + lifecycleLines);
        assertTrue(conversionLines <= 150,
                "QuotationConversionService.java deve ter <= 150 linhas, mas tem: " + conversionLines);
        assertTrue(queryLines <= 135,
                "QuotationQueryService.java deve ter <= 135 linhas, mas tem: " + queryLines);
    }

    @Test
    @DisplayName("Garante separação de responsabilidades e delegação no orquestrador")
    void shouldMaintainDelegationPattern() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("lifecycleService.create"),
                "QuotationService deve delegar create para QuotationLifecycleService");
        assertTrue(content.contains("lifecycleService.update"),
                "QuotationService deve delegar update para QuotationLifecycleService");
        assertTrue(content.contains("conversionService.convert"),
                "QuotationService deve delegar convert para QuotationConversionService");
        assertTrue(content.contains("conversionService.convertToInvoice"),
                "QuotationService deve delegar convertToInvoice para QuotationConversionService");
        assertTrue(content.contains("queryService.findByCompany"),
                "QuotationService deve delegar findByCompany para QuotationQueryService");
    }

    @Test
    @DisplayName("Verifica ausência de chamadas proibidas e UI no backend")
    void shouldNotContainForbiddenCalls() throws IOException {
        List<Path> paths = List.of(SERVICE_PATH, LIFECYCLE_PATH, CONVERSION_PATH, QUERY_PATH);
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
                "QuotationLifecycleService deve ser @Service");
        assertTrue(Files.readString(CONVERSION_PATH).contains("@Service"),
                "QuotationConversionService deve ser @Service");
        assertTrue(Files.readString(QUERY_PATH).contains("@Service"),
                "QuotationQueryService deve ser @Service");
    }
}
