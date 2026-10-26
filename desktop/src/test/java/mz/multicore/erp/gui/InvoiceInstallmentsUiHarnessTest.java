package mz.multicore.erp.gui;

import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceInstallmentsUiHarnessTest {

    @Test
    @DisplayName("Garante integridade e serialização dos DTOs de Prestações Comerciais")
    void validateInstallmentDtoIntegrity() {
        InvoiceInstallmentDTO installment = new InvoiceInstallmentDTO(
                10L,
                100L,
                1,
                3,
                LocalDate.of(2026, 11, 15),
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("800.00"),
                "PARTIAL",
                "Entrada inicial"
        );

        assertNotNull(installment);
        assertEquals(10L, installment.id());
        assertEquals(100L, installment.invoiceId());
        assertEquals(1, installment.installmentNumber());
        assertEquals(3, installment.totalInstallments());
        assertEquals(LocalDate.of(2026, 11, 15), installment.dueDate());
        assertEquals(new BigDecimal("1000.00"), installment.amount());
        assertEquals(new BigDecimal("200.00"), installment.paidAmount());
        assertEquals(new BigDecimal("800.00"), installment.remainingBalance());
        assertEquals("PARTIAL", installment.status());
        assertEquals("Entrada inicial", installment.notes());
    }

    @Test
    @DisplayName("Valida cálculo do plano de prestações e absorção de arredondamentos na primeira parcela")
    void validateInstallmentPlanCalculationWithRounding() {
        BigDecimal totalDebt = new BigDecimal("100.00");
        int count = 3;
        int intervalDays = 30;
        LocalDate firstDue = LocalDate.of(2026, 11, 1);

        // Lógica de decomposição exata
        BigDecimal base = totalDebt.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal sumBase = base.multiply(BigDecimal.valueOf(count));
        BigDecimal remainder = totalDebt.subtract(sumBase);

        List<BigDecimal> amounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal parcela = (i == 0) ? base.add(remainder) : base;
            amounts.add(parcela);
        }

        // 100 / 3 = 33.33 * 3 = 99.99 -> Resto = 0.01 na 1ª parcela -> 33.34, 33.33, 33.33
        assertEquals(3, amounts.size());
        assertEquals(new BigDecimal("33.34"), amounts.get(0));
        assertEquals(new BigDecimal("33.33"), amounts.get(1));
        assertEquals(new BigDecimal("33.33"), amounts.get(2));

        BigDecimal totalSum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(totalDebt, totalSum);

        CreateInstallmentPlanRequest request = new CreateInstallmentPlanRequest(
                100L,
                count,
                intervalDays,
                firstDue,
                amounts
        );

        assertNotNull(request);
        assertEquals(100L, request.invoiceId());
        assertEquals(3, request.numberOfInstallments());
        assertEquals(30, request.intervalDays());
        assertEquals(firstDue, request.firstDueDate());
        assertEquals(3, request.customAmounts().size());
        assertEquals(new BigDecimal("33.34"), request.customAmounts().get(0));
    }

    @Test
    @DisplayName("Valida liquidação progressiva de prestações fracionadas")
    void validateInstallmentAmortizationSimulation() {
        InvoiceInstallmentDTO p1 = new InvoiceInstallmentDTO(1L, 10L, 1, 2, LocalDate.now().plusDays(15),
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("500.00"), "PENDING", null);
        InvoiceInstallmentDTO p2 = new InvoiceInstallmentDTO(2L, 10L, 2, 2, LocalDate.now().plusDays(45),
                new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("500.00"), "PENDING", null);

        List<InvoiceInstallmentDTO> list = List.of(p1, p2);

        BigDecimal payment = new BigDecimal("700.00");
        BigDecimal remaining = payment;

        List<InvoiceInstallmentDTO> updated = new ArrayList<>();
        for (InvoiceInstallmentDTO inst : list) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                updated.add(inst);
                continue;
            }
            BigDecimal canPay = remaining.min(inst.remainingBalance());
            BigDecimal newPaid = inst.paidAmount().add(canPay);
            BigDecimal newBal = inst.amount().subtract(newPaid);
            remaining = remaining.subtract(canPay);
            String st = newBal.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PARTIAL";
            updated.add(new InvoiceInstallmentDTO(inst.id(), inst.invoiceId(), inst.installmentNumber(),
                    inst.totalInstallments(), inst.dueDate(), inst.amount(), newPaid, newBal, st, inst.notes()));
        }

        assertEquals(2, updated.size());
        assertEquals("PAID", updated.get(0).status());
        assertEquals(0, updated.get(0).remainingBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, updated.get(0).paidAmount().compareTo(new BigDecimal("500.00")));

        assertEquals("PARTIAL", updated.get(1).status());
        assertEquals(0, updated.get(1).remainingBalance().compareTo(new BigDecimal("300.00")));
        assertEquals(0, updated.get(1).paidAmount().compareTo(new BigDecimal("200.00")));
        assertEquals(0, remaining.compareTo(BigDecimal.ZERO));
    }
}
