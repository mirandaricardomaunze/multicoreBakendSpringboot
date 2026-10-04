package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnResultDTO;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes automatizados para conformidade visual e de ergonomia do fluxo de Vales de Compras no POS.
 */
public class PosVoucherUiHarnessTest {

    @Test
    @DisplayName("UI-VOUCH-01: PosPaymentDialog possui métodos show e showPayments retornando DTOs válidos")
    void testPosPaymentDialogMethodSignatures() {
        assertDoesNotThrow(() -> {
            Method showM1 = PosPaymentDialog.class.getDeclaredMethod("show", BigDecimal.class, Long.class);
            assertNotNull(showM1);
            Method showM2 = PosPaymentDialog.class.getDeclaredMethod("show", BigDecimal.class, Long.class, POSApiClient.class);
            assertNotNull(showM2);
            Method showPayments = PosPaymentDialog.class.getDeclaredMethod("showPayments", BigDecimal.class, Long.class, POSApiClient.class);
            assertNotNull(showPayments);
            assertEquals(List.class, showPayments.getReturnType());
        });
    }

    @Test
    @DisplayName("UI-VOUCH-02: POSApiClient expõe métodos completos de consulta, emissão e talão de vales")
    void testPosApiClientVoucherMethods() throws Exception {
        Method returnSale = POSApiClient.class.getDeclaredMethod("returnSale", POSReturnRequest.class);
        assertEquals(POSReturnResultDTO.class, returnSale.getReturnType(), "returnSale deve retornar POSReturnResultDTO");

        Method getVoucher = POSApiClient.class.getDeclaredMethod("getVoucher", String.class, Long.class);
        assertEquals(Optional.class, getVoucher.getReturnType(), "getVoucher deve retornar Optional<StoreVoucherDTO>");

        Method renderVoucher = POSApiClient.class.getDeclaredMethod("renderVoucher", Long.class);
        assertEquals(byte[].class, renderVoucher.getReturnType(), "renderVoucher deve retornar PDF em bytes");

        Method renderByCode = POSApiClient.class.getDeclaredMethod("renderVoucherByCode", String.class);
        assertEquals(byte[].class, renderByCode.getReturnType(), "renderVoucherByCode deve retornar PDF em bytes");
    }

    @Test
    @DisplayName("UI-VOUCH-03: Ficheiros modificados cumprem rigorosamente a restrição de < 1000 linhas")
    void testFileLengthConstraints() throws Exception {
        Path basePath = Path.of("src/main/java/mz/multicore/erp/gui");
        List<String> filesToCheck = List.of(
                "PosReturnDialog.java",
                "PosPaymentDialog.java",
                "POSPanel.java"
        );

        for (String fileName : filesToCheck) {
            Path file = basePath.resolve(fileName);
            assertTrue(Files.exists(file), "Ficheiro deve existir: " + fileName);
            long lineCount = Files.lines(file).count();
            assertTrue(lineCount < 1000,
                    String.format("Ficheiro %s excede 1000 linhas (linhas actuais: %d)", fileName, lineCount));
        }
    }

    @Test
    @DisplayName("UI-VOUCH-04: Ausência de emojis crus Unicode nos ficheiros de interface")
    void testNoRawUnicodeEmojis() throws Exception {
        Path basePath = Path.of("src/main/java/mz/multicore/erp/gui");
        List<String> filesToCheck = List.of(
                "PosReturnDialog.java",
                "PosPaymentDialog.java"
        );

        String[] forbiddenEmojis = {"⭐", "✅", "❌", "🟢", "🔴", "🔑", "🎫", "💰", "🧾"};

        for (String fileName : filesToCheck) {
            String content = Files.readString(basePath.resolve(fileName));
            for (String emoji : forbiddenEmojis) {
                assertFalse(content.contains(emoji),
                        String.format("Ficheiro %s não pode conter emojis Unicode (%s)", fileName, emoji));
            }
        }
    }

    @Test
    @DisplayName("UI-VOUCH-05: StoreVoucherDTO possui métodos utilitários isActive e isExpired")
    void testStoreVoucherDtoHelpers() {
        StoreVoucherDTO active = new StoreVoucherDTO(
                1L, "VALE-1234", new BigDecimal("100"), new BigDecimal("100"), 1L, null, "Cliente",
                null, null, null, java.time.LocalDate.now().plusDays(10), "ACTIVE", "admin"
        );
        assertTrue(active.isActive());
        assertFalse(active.isExpired());

        StoreVoucherDTO expired = new StoreVoucherDTO(
                2L, "VALE-5678", new BigDecimal("100"), new BigDecimal("100"), 1L, null, "Cliente",
                null, null, null, java.time.LocalDate.now().minusDays(1), "ACTIVE", "admin"
        );
        assertTrue(expired.isExpired());

        StoreVoucherDTO fullyRedeemed = new StoreVoucherDTO(
                3L, "VALE-0000", new BigDecimal("100"), BigDecimal.ZERO, 1L, null, "Cliente",
                null, null, null, java.time.LocalDate.now().plusDays(10), "FULLY_REDEEMED", "admin"
        );
        assertFalse(fullyRedeemed.isActive());
    }
}
