package mz.multicore.erp.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de StockTransferTransitService (Fase 120).
 */
class StockTransferTransitServiceDecompositionHarnessTest {

    private static final String BASE = "src/main/java/mz/multicore/erp/modules/inventory/service/";

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
    @DisplayName("STTD-01: Limites físicos estritos de linhas de código")
    void sttd01_limitesFisicosLinhasCodigo() {
        assertThat(countLines("StockTransferTransitService.java")).as("Fachada <= 110").isLessThanOrEqualTo(110);
        assertThat(countLines("StockTransferReceiptService.java")).as("Recepção <= 155").isLessThanOrEqualTo(155);
        assertThat(countLines("StockTransferReceiptValidator.java")).as("Validador <= 105").isLessThanOrEqualTo(105);
        assertThat(countLines("StockTransferDispatchService.java")).as("Despacho <= 85").isLessThanOrEqualTo(85);
        assertThat(countLines("StockTransitLedger.java")).as("Ledger <= 80").isLessThanOrEqualTo(80);
    }

    @Test
    @DisplayName("STTD-02: Backend headless sem Swing nem JOptionPane")
    void sttd02_backendHeadless() {
        for (String file : List.of("StockTransferTransitService.java", "StockTransferReceiptService.java",
                "StockTransferReceiptValidator.java", "StockTransferDispatchService.java", "StockTransitLedger.java")) {
            assertThat(readSource(file)).as(file).doesNotContain("JOptionPane", "javax.swing");
        }
    }

    @Test
    @DisplayName("STTD-03: Delegação canónica e lock pessimista no ledger")
    void sttd03_delegacaoCanonica() {
        assertThat(readSource("StockTransferTransitService.java"))
                .contains("dispatchService.dispatch(")
                .contains("receiptService.receive(")
                .doesNotContain("stockRepository.");
        assertThat(readSource("StockTransitLedger.java"))
                .contains("Stock stock = stockRepository.findForReservation");
        assertThat(readSource("StockTransferReceiptService.java"))
                .contains("PermissionGuard.requireManagerOrAdmin(")
                .contains("lockByIdAndCompanyId(");
    }

    @Test
    @DisplayName("STTD-04: Contratos públicos e construtor retrocompatível preservados")
    void sttd04_contratosPreservados() {
        assertThat(readSource("StockTransferTransitService.java"))
                .contains("public void dispatch(StockTransfer transfer, String dispatcher)")
                .contains("public StockTransferDTO receive(Long id, String notes)")
                .contains("public List<StockTransferDTO> findInTransit(Long companyId, Long destinationWarehouseId)")
                .contains("ProductBatchService productBatchService,")
                .contains("@Autowired");
        assertThat(readSource("StockTransferReceiptValidator.java")).doesNotContain("BigDecimal.ZERO.equals");
        assertThat(readSource("StockTransferReceiptService.java")).doesNotContain("BigDecimal.ZERO.equals");
    }
}
