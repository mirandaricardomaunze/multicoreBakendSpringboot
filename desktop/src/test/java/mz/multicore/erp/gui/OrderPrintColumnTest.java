package mz.multicore.erp.gui;

import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Orders list "Impressões" column: picking sheets and official A4 copies are counted together. */
class OrderPrintColumnTest {

    private static DocumentPrintStatusDTO official(int count, LocalDateTime at) {
        return new DocumentPrintStatusDTO(SensitiveDocumentType.ORDER, 1L, count, at, "ana", null, null);
    }

    @Test
    void neverPrintedSaysSo() {
        assertThat(OrderPrintColumn.label(0, null, null)).isEqualTo("Não impressa");
        assertThat(OrderPrintColumn.label(0, null, official(0, null))).isEqualTo("Não impressa");
    }

    @Test
    void officialA4CopiesAreCountedEvenWithoutPickingSheet() {
        assertThat(OrderPrintColumn.label(0, null, official(2, LocalDateTime.of(2026, 11, 8, 10, 0)))).isEqualTo("2 × 08/11/2026");
    }

    @Test
    void bothKindsAddUpAndTheLatestDateWins() {
        String label = OrderPrintColumn.label(1, LocalDateTime.of(2026, 10, 14, 9, 0), official(1, LocalDateTime.of(2026, 11, 2, 9, 0)));
        assertThat(label).isEqualTo("2 × 02/11/2026");
    }
}
