package mz.multicore.erp.modules.printing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockTransferPrintServiceDecompositionHarnessTest {

    private static final Path SERVICE_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/printing/StockTransferPrintService.java");
    private static final Path ROUTE_BUILDER_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/printing/StockTransferPrintRouteBuilder.java");
    private static final Path LINES_TABLE_BUILDER_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/printing/StockTransferPrintLinesTableBuilder.java");
    private static final Path SUMMARY_BUILDER_PATH = Paths.get("src/main/java/mz/multicore/erp/modules/printing/StockTransferPrintSummaryBuilder.java");

    @Test
    @DisplayName("Garante que StockTransferPrintService e seus colaboradores existem e cumprem limites de linhas")
    void testFileSizesAndLimits() throws IOException {
        assertTrue(Files.exists(SERVICE_PATH), "StockTransferPrintService.java deve existir");
        assertTrue(Files.exists(ROUTE_BUILDER_PATH), "StockTransferPrintRouteBuilder.java deve existir");
        assertTrue(Files.exists(LINES_TABLE_BUILDER_PATH), "StockTransferPrintLinesTableBuilder.java deve existir");
        assertTrue(Files.exists(SUMMARY_BUILDER_PATH), "StockTransferPrintSummaryBuilder.java deve existir");

        long serviceLines = Files.lines(SERVICE_PATH).count();
        long routeLines = Files.lines(ROUTE_BUILDER_PATH).count();
        long tableLines = Files.lines(LINES_TABLE_BUILDER_PATH).count();
        long summaryLines = Files.lines(SUMMARY_BUILDER_PATH).count();

        assertTrue(serviceLines <= 160,
                "StockTransferPrintService deve ter <= 160 linhas, mas tem: " + serviceLines);
        assertTrue(routeLines <= 130,
                "StockTransferPrintRouteBuilder deve ter <= 130 linhas, mas tem: " + routeLines);
        assertTrue(tableLines <= 225,
                "StockTransferPrintLinesTableBuilder deve ter <= 225 linhas, mas tem: " + tableLines);
        assertTrue(summaryLines <= 150,
                "StockTransferPrintSummaryBuilder deve ter <= 150 linhas, mas tem: " + summaryLines);
    }

    @Test
    @DisplayName("Garante que StockTransferPrintService delega a composição visual para os colaboradores especializados")
    void testDelegationInOrchestrator() throws IOException {
        String content = Files.readString(SERVICE_PATH);

        assertTrue(content.contains("routeBuilder.buildHorizontalRule()"),
                "Deve delegar a régua horizontal para o routeBuilder");
        assertTrue(content.contains("routeBuilder.buildProfessionalRouteBlock(transfer)"),
                "Deve delegar o bloco profissional de rota para o routeBuilder");
        assertTrue(content.contains("linesTableBuilder.buildProfessionalLinesTable(transfer)"),
                "Deve delegar a tabela profissional de linhas para o linesTableBuilder");
        assertTrue(content.contains("summaryBuilder.buildProfessionalSummary(transfer)"),
                "Deve delegar o bloco de totais/resumo para o summaryBuilder");
        assertTrue(content.contains("summaryBuilder.buildProfessionalSignatureBlock()"),
                "Deve delegar as assinaturas profissionais para o summaryBuilder");
        assertTrue(content.contains("routeBuilder.buildPickingRouteBlock(transfer)"),
                "Deve delegar a rota de picking para o routeBuilder");
        assertTrue(content.contains("linesTableBuilder.buildPickingLinesTable(transfer.getLines())"),
                "Deve delegar as linhas de picking para o linesTableBuilder");
        assertTrue(content.contains("summaryBuilder.buildLoadSummary(transfer.getLines())"),
                "Deve delegar o resumo de carga para o summaryBuilder");
        assertTrue(content.contains("summaryBuilder.buildPickingSignatureBlock()"),
                "Deve delegar as assinaturas de picking para o summaryBuilder");
    }

    @Test
    @DisplayName("Garante anotações Spring @Service e @Component adequadas para injeção de dependência")
    void testSpringAnnotations() throws IOException {
        String serviceContent = Files.readString(SERVICE_PATH);
        String routeContent = Files.readString(ROUTE_BUILDER_PATH);
        String tableContent = Files.readString(LINES_TABLE_BUILDER_PATH);
        String summaryContent = Files.readString(SUMMARY_BUILDER_PATH);

        assertTrue(serviceContent.contains("@Service"), "StockTransferPrintService deve ser @Service");
        assertTrue(routeContent.contains("@Component"), "StockTransferPrintRouteBuilder deve ser @Component");
        assertTrue(tableContent.contains("@Component"), "StockTransferPrintLinesTableBuilder deve ser @Component");
        assertTrue(summaryContent.contains("@Component"), "StockTransferPrintSummaryBuilder deve ser @Component");
    }

    @Test
    @DisplayName("Garante construtores retrocompatíveis em StockTransferPrintService")
    void testBackwardCompatibleConstructors() {
        StockTransferPrintService defaultCtor = new StockTransferPrintService(null);
        assertNotNull(defaultCtor, "Construtor com 1 parâmetro deve instanciar");

        StockTransferPrintService batchCtor = new StockTransferPrintService(null, null);
        assertNotNull(batchCtor, "Construtor com 2 parâmetros deve instanciar");
    }
}
