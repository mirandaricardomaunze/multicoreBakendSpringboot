package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceInstallment;
import mz.multicore.erp.modules.comercial.repository.InvoiceInstallmentRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceInstallmentServiceTest {

    private InvoiceInstallmentRepository installmentRepository;
    private InvoiceRepository invoiceRepository;
    private InvoiceInstallmentService service;

    private Company company;
    private Invoice invoice;

    @BeforeEach
    void setup() {
        installmentRepository = mock(InvoiceInstallmentRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        service = new InvoiceInstallmentService(installmentRepository, invoiceRepository);

        company = new Company();
        company.setId(1L);
        company.setName("Multicore Teste Lda");

        invoice = new Invoice();
        invoice.setId(10L);
        invoice.setCompany(company);
        invoice.setInvoiceNumber("FT-2026/1");
        invoice.setTotalAmount(new BigDecimal("1000.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);

        CurrentUserContext.setCurrentCompanyId(1L);
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(installmentRepository.save(any(InvoiceInstallment.class))).thenAnswer(inv -> {
            InvoiceInstallment inst = inv.getArgument(0);
            if (inst.getId() == null) inst.setId((long) (Math.random() * 10000) + 1);
            return inst;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("Cria plano de 3 parcelas iguais com absorção exacta de centavos na última parcela")
    void createInstallmentPlan_equalPartsWithRounding() {
        LocalDate start = LocalDate.now().plusDays(1);
        CreateInstallmentPlanRequest request = new CreateInstallmentPlanRequest(
                10L, 3, 30, start, List.of()
        );

        List<InvoiceInstallmentDTO> result = service.createInstallmentPlan(1L, request);

        assertNotNull(result);
        assertEquals(3, result.size());

        // 1000 / 3 = 333.33 + 333.33 + 333.34 = 1000.00 exactos
        assertEquals(new BigDecimal("333.33"), result.get(0).amount());
        assertEquals(start, result.get(0).dueDate());
        assertEquals("PENDING", result.get(0).status());

        assertEquals(new BigDecimal("333.33"), result.get(1).amount());
        assertEquals(start.plusDays(30), result.get(1).dueDate());
        assertEquals("PENDING", result.get(1).status());

        assertEquals(new BigDecimal("333.34"), result.get(2).amount());
        assertEquals(start.plusDays(60), result.get(2).dueDate());
        assertEquals("PENDING", result.get(2).status());

        BigDecimal sum = result.stream().map(InvoiceInstallmentDTO::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1000.00"), sum);
    }

    @Test
    @DisplayName("Valida soma de parcelas customizadas contra total da fatura")
    void createInstallmentPlan_customAmounts() {
        LocalDate start = LocalDate.of(2026, 11, 1);

        // Soma errada: 500 + 400 = 900 != 1000
        CreateInstallmentPlanRequest invalidRequest = new CreateInstallmentPlanRequest(
                10L, 2, 30, start, List.of(new BigDecimal("500.00"), new BigDecimal("400.00"))
        );
        assertThrows(BusinessRuleException.class, () -> service.createInstallmentPlan(1L, invalidRequest));

        // Soma correta: 600 + 400 = 1000
        CreateInstallmentPlanRequest validRequest = new CreateInstallmentPlanRequest(
                10L, 2, 30, start, List.of(new BigDecimal("600.00"), new BigDecimal("400.00"))
        );
        List<InvoiceInstallmentDTO> result = service.createInstallmentPlan(1L, validRequest);
        assertEquals(2, result.size());
        assertEquals(new BigDecimal("600.00"), result.get(0).amount());
        assertEquals(new BigDecimal("400.00"), result.get(1).amount());
    }

    @Test
    @DisplayName("Aplica amortização sequencial amortizando primeiramente a prestação mais antiga")
    void applyPaymentToInvoiceInstallments_sequentialAmortization() {
        InvoiceInstallment inst1 = new InvoiceInstallment();
        inst1.setId(101L);
        inst1.setInstallmentNumber(1);
        inst1.setAmount(new BigDecimal("333.33"));
        inst1.setRemainingBalance(new BigDecimal("333.33"));
        inst1.setPaidAmount(BigDecimal.ZERO);

        InvoiceInstallment inst2 = new InvoiceInstallment();
        inst2.setId(102L);
        inst2.setInstallmentNumber(2);
        inst2.setAmount(new BigDecimal("333.33"));
        inst2.setRemainingBalance(new BigDecimal("333.33"));
        inst2.setPaidAmount(BigDecimal.ZERO);

        InvoiceInstallment inst3 = new InvoiceInstallment();
        inst3.setId(103L);
        inst3.setInstallmentNumber(3);
        inst3.setAmount(new BigDecimal("333.34"));
        inst3.setRemainingBalance(new BigDecimal("333.34"));
        inst3.setPaidAmount(BigDecimal.ZERO);

        List<InvoiceInstallment> list = new ArrayList<>(List.of(inst1, inst2, inst3));
        when(installmentRepository.findByInvoiceIdAndCompanyIdOrderByInstallmentNumberAsc(10L, 1L))
                .thenReturn(list);

        // Pagamento de 500 MT: 333.33 liquida inst1, restante 166.67 amortiza inst2
        List<InvoiceInstallmentDTO> updated = service.applyPaymentToInvoiceInstallments(1L, 10L, new BigDecimal("500.00"));

        assertEquals("PAID", updated.get(0).status());
        assertEquals(BigDecimal.ZERO, updated.get(0).remainingBalance());
        assertEquals(new BigDecimal("333.33"), updated.get(0).paidAmount());

        assertEquals("PARTIALLY_PAID", updated.get(1).status());
        assertEquals(new BigDecimal("166.66"), updated.get(1).remainingBalance());
        assertEquals(new BigDecimal("166.67"), updated.get(1).paidAmount());

        assertEquals("PENDING", updated.get(2).status());
        assertEquals(new BigDecimal("333.34"), updated.get(2).remainingBalance());
        assertEquals(BigDecimal.ZERO, updated.get(2).paidAmount());
    }
}
