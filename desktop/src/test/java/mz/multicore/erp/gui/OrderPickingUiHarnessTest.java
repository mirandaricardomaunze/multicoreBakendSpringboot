package mz.multicore.erp.gui;

import mz.multicore.erp.modules.comercial.dto.OrderPickingDetailsDTO;
import mz.multicore.erp.modules.comercial.dto.OrderPickingLineDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderPickingUiHarnessTest {

    @Test
    @DisplayName("Valida constantes de colunas do diálogo OrderPickingDialog")
    void columnConstants_areCorrect() {
        assertEquals(0, OrderPickingDialog.COL_STATUS);
        assertEquals(1, OrderPickingDialog.COL_BARCODE);
        assertEquals(2, OrderPickingDialog.COL_REF);
        assertEquals(3, OrderPickingDialog.COL_NAME);
        assertEquals(4, OrderPickingDialog.COL_REQUESTED);
        assertEquals(5, OrderPickingDialog.COL_PACKAGES);
        assertEquals(6, OrderPickingDialog.COL_BOXES);
        assertEquals(7, OrderPickingDialog.COL_CONFIRMED);
        assertEquals(8, OrderPickingDialog.COL_BATCH);
        assertEquals(9, OrderPickingDialog.COL_LINE_ID);
    }

    @Test
    @DisplayName("DTO de Picking Details preserva contagem de caixas e linhas")
    void orderPickingDetailsDTO_holdsConsistentData() {
        OrderPickingLineDTO line = new OrderPickingLineDTO(
                1L, 10L, "REF-10", "1234567890123", "Farinha Trigo 1kg",
                new BigDecimal("20"), BigDecimal.ZERO, 10, 1,
                new BigDecimal("2.00"), "LOTE-1", null, "Armazém A"
        );

        OrderPickingDetailsDTO details = new OrderPickingDetailsDTO(
                100L, "ENC-100", "Padaria Central", "Armazém A",
                "IN_SEPARATION", List.of(line), 1, new BigDecimal("2.00")
        );

        assertEquals(100L, details.orderId());
        assertEquals("ENC-100", details.orderNumber());
        assertEquals("Padaria Central", details.clientName());
        assertEquals(1, details.totalItems());
        assertEquals(new BigDecimal("2.00"), details.totalBoxes());
        assertEquals(1, details.lines().size());
        assertEquals("REF-10", details.lines().get(0).productReference());
    }
}
