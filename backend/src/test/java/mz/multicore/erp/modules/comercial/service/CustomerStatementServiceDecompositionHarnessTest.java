package mz.multicore.erp.modules.comercial.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de CustomerStatementService (Fase 123).
 */
class CustomerStatementServiceDecompositionHarnessTest {

    private static final String BASE = "src/main/java/mz/multicore/erp/modules/comercial/service/";
    private static final List<String> FILES = List.of(
            "CustomerStatementService.java",
            "CustomerStatementCollector.java",
            "CustomerStatementCalculator.java",
            "CustomerStatementMovement.java"
    );

    private static String readSource(String file) {
        try {
            return Files.readString(Path.of(BASE + file));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + file, e);
        }
    }

    private static long countLines(String file) {
        try (var lines = Files.lines(Path.of(BASE + file))) {
            return lines.count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + file, e);
        }
    }

    @Test
    @DisplayName("CSSD-01: Limites físicos estritos de linhas de código")
    void cssd01_limitesFisicosLinhasCodigo() {
        assertThat(countLines("CustomerStatementService.java")).as("Fachada <= 85").isLessThanOrEqualTo(85);
        assertThat(countLines("CustomerStatementCollector.java")).as("Coletor <= 195").isLessThanOrEqualTo(195);
        assertThat(countLines("CustomerStatementCalculator.java")).as("Calculador <= 95").isLessThanOrEqualTo(95);
        assertThat(countLines("CustomerStatementMovement.java")).as("Record <= 30").isLessThanOrEqualTo(30);
    }

    @Test
    @DisplayName("CSSD-02: Backend headless sem Swing nem JOptionPane")
    void cssd02_backendHeadless() {
        for (String file : FILES) {
            assertThat(readSource(file)).as(file).doesNotContain("JOptionPane", "javax.swing");
        }
    }

    @Test
    @DisplayName("CSSD-03: Delegação canónica para coletor e calculador")
    void cssd03_delegacaoCanonica() {
        assertThat(readSource("CustomerStatementService.java"))
                .contains("collector.collect(")
                .contains("calculator.calculate(")
                .doesNotContain("invoiceRepository.", "receiptRepository.", "creditNoteRepository.", "debitNoteRepository.");
        assertThat(readSource("CustomerStatementCollector.java"))
                .contains("invoiceRepository.findByCompanyId(")
                .contains("receiptRepository.findByCompanyId(")
                .contains("debitNoteRepository.findByCompanyId(")
                .contains("creditNoteRepository.findByCompanyId(");
        assertThat(readSource("CustomerStatementCalculator.java"))
                .contains("openingBalance.add(mov.debit()).subtract(mov.credit())")
                .contains("closingBalance");
    }

    @Test
    @DisplayName("CSSD-04: Contratos públicos e construtor retrocompatível preservados")
    void cssd04_contratosPreservados() {
        assertThat(readSource("CustomerStatementService.java"))
                .contains("@Autowired")
                .contains("DebitNoteRepository debitNoteRepository");
        assertThat(readSource("CustomerStatementCollector.java")).doesNotContain("BigDecimal.ZERO.equals");
        assertThat(readSource("CustomerStatementCalculator.java")).doesNotContain("BigDecimal.ZERO.equals");
    }
}
