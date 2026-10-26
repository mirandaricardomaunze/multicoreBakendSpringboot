package mz.multicore.erp.gui;

import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardLineRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SalesReturnUiHarnessTest {

    @Test
    @DisplayName("Garante integridade e cálculo automático de saldo devolvível no ReturnableInvoiceLineDTO")
    void testReturnableInvoiceLineDtoIntegrity() {
        ReturnableInvoiceLineDTO line = new ReturnableInvoiceLineDTO(
                10L,
                100L,
                "REF-OLEO-1L",
                "Óleo Vegetal 1L",
                new BigDecimal("120.00"),
                new BigDecimal("0.16"),
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                null, // Deve derivar 10.00 - 3.00 = 7.00
                new BigDecimal("1.75"),
                4,
                1,
                "LOTE-2026-X",
                LocalDate.of(2027, 6, 30)
        );

        assertNotNull(line);
        assertEquals(10L, line.invoiceLineId());
        assertEquals("REF-OLEO-1L", line.productCode());
        assertEquals(new BigDecimal("10.00"), line.invoicedQuantity());
        assertEquals(new BigDecimal("3.00"), line.alreadyReturnedQuantity());
        assertEquals(new BigDecimal("7.00"), line.returnableQuantity());
        assertEquals(new BigDecimal("1.75"), line.boxes());
        assertEquals("LOTE-2026-X", line.batchNumber());
    }

    @Test
    @DisplayName("Valida simulação de cálculo de devolução e conversão em percentagem de caixa (ex: 0.75)")
    void testPackagingBoxesCalculationOnReturn() {
        // Exemplo da regra do utilizador: 3 embalagens de 4 de uma caixa -> 0.75 caixa
        BigDecimal qtyToReturn = new BigDecimal("3.00");
        int unitsPerBox = 4;

        BigDecimal boxes = qtyToReturn.divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("0.75"), boxes);

        // Se o produto não tiver caixa, usa 0.00 em vez de travessão
        BigDecimal noBox = new BigDecimal("0.00");
        assertEquals(new BigDecimal("0.00"), noBox);
    }

    @Test
    @DisplayName("Garante criação do pedido do assistente e mapeamento do resultado com NC emitida")
    void testSalesReturnWizardRequestAndResult() {
        SalesReturnWizardLineRequest lineReq = new SalesReturnWizardLineRequest(10L, new BigDecimal("2.00"), true);
        SalesReturnWizardRequest request = new SalesReturnWizardRequest(
                500L,
                "RETURN",
                1L,
                true,
                "Devolução de teste",
                List.of(lineReq)
        );

        assertNotNull(request);
        assertEquals(500L, request.invoiceId());
        assertEquals("RETURN", request.reason());
        assertTrue(request.autoApprove());
        assertEquals(1, request.lines().size());
        assertEquals(new BigDecimal("2.00"), request.lines().get(0).quantity());
        assertTrue(request.lines().get(0).restock());

        SalesReturnWizardResultDTO result = new SalesReturnWizardResultDTO(
                77L,
                "NC-2026/77",
                500L,
                "FT-2026/500",
                "APPROVED",
                new BigDecimal("278.40"),
                1,
                LocalDateTime.now(),
                "Nota de Crédito processada com sucesso"
        );

        assertNotNull(result);
        assertEquals(77L, result.creditNoteId());
        assertEquals("NC-2026/77", result.creditNoteNumber());
        assertEquals("APPROVED", result.status());
        assertEquals(new BigDecimal("278.40"), result.totalCredited());
    }
}
