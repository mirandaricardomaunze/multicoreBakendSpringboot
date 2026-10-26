package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CollectionRadarServiceTest {

    private InvoiceRepository invoiceRepository;
    private CollectionRadarService radarService;

    @BeforeEach
    void setUp() {
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        radarService = new CollectionRadarService(invoiceRepository);
    }

    private Invoice createMockInvoice(Long id, String number, Client client, BigDecimal total, BigDecimal paid,
                                      LocalDate issueDate, LocalDate dueDate) {
        Invoice inv = Mockito.mock(Invoice.class);
        when(inv.getId()).thenReturn(id);
        when(inv.getInvoiceNumber()).thenReturn(number);
        when(inv.getClient()).thenReturn(client);
        when(inv.getTotalAmount()).thenReturn(total);
        when(inv.getAmountPaid()).thenReturn(paid);
        when(inv.outstandingAmount()).thenReturn(total.subtract(paid));
        when(inv.issueDate()).thenReturn(issueDate);
        when(inv.effectiveDueDate()).thenReturn(dueDate);
        when(inv.getStatus()).thenReturn(InvoiceStatus.APPROVED);
        return inv;
    }

    @Test
    @DisplayName("Garante agregação e consolidação correta do resumo do radar por horizontes temporais")
    void testRadarSummaryClassification() {
        LocalDate ref = LocalDate.of(2026, 10, 20);
        Client c = new Client();
        c.setName("Ferragens do Norte Lda");

        // 1. Vencida há 20 dias -> CRITICAL_OVERDUE
        Invoice inv1 = createMockInvoice(1L, "FT-1", c, new BigDecimal("1000.00"), BigDecimal.ZERO,
                ref.minusDays(50), ref.minusDays(20));

        // 2. Vencida há 5 dias -> RECENT_OVERDUE
        Invoice inv2 = createMockInvoice(2L, "FT-2", c, new BigDecimal("500.00"), BigDecimal.ZERO,
                ref.minusDays(35), ref.minusDays(5));

        // 3. Vence hoje -> DUE_TODAY
        Invoice inv3 = createMockInvoice(3L, "FT-3", c, new BigDecimal("300.00"), BigDecimal.ZERO,
                ref.minusDays(30), ref);

        // 4. Vence em 4 dias -> DUE_IN_7_DAYS
        Invoice inv4 = createMockInvoice(4L, "FT-4", c, new BigDecimal("800.00"), BigDecimal.ZERO,
                ref.minusDays(26), ref.plusDays(4));

        // 5. Vence em 12 dias -> DUE_IN_15_DAYS
        Invoice inv5 = createMockInvoice(5L, "FT-5", c, new BigDecimal("1200.00"), BigDecimal.ZERO,
                ref.minusDays(18), ref.plusDays(12));

        // 6. Vence em 22 dias -> DUE_IN_30_DAYS
        Invoice inv6 = createMockInvoice(6L, "FT-6", c, new BigDecimal("2000.00"), BigDecimal.ZERO,
                ref.minusDays(8), ref.plusDays(22));

        when(invoiceRepository.findOutstandingWithClient(eq(1L), any()))
                .thenReturn(List.of(inv1, inv2, inv3, inv4, inv5, inv6));

        CollectionRadarSummaryDTO summary = radarService.getRadarSummary(1L, ref);

        assertNotNull(summary);
        assertEquals(new BigDecimal("1000.00"), summary.criticalOverdueTotal());
        assertEquals(1, summary.criticalOverdueCount());

        assertEquals(new BigDecimal("500.00"), summary.recentOverdueTotal());
        assertEquals(1, summary.recentOverdueCount());

        assertEquals(new BigDecimal("300.00"), summary.dueTodayTotal());
        assertEquals(1, summary.dueTodayCount());

        assertEquals(new BigDecimal("800.00"), summary.dueIn7DaysTotal());
        assertEquals(1, summary.dueIn7DaysCount());

        assertEquals(new BigDecimal("1200.00"), summary.dueIn15DaysTotal());
        assertEquals(1, summary.dueIn15DaysCount());

        assertEquals(new BigDecimal("2000.00"), summary.dueIn30DaysTotal());
        assertEquals(1, summary.dueIn30DaysCount());

        assertEquals(new BigDecimal("5800.00"), summary.totalReceivable());
        assertEquals(6, summary.totalInvoicesCount());
    }

    @Test
    @DisplayName("Garante filtro e ordenação dos itens do radar por prioridade de urgência")
    void testRadarItemsFilteringAndSorting() {
        LocalDate ref = LocalDate.of(2026, 10, 20);
        Client c = new Client();
        c.setName("Supermercado Central");
        c.setTaxId("400123456");

        Invoice invLate = createMockInvoice(1L, "FT-01", c, new BigDecimal("1000.00"), BigDecimal.ZERO,
                ref.minusDays(30), ref.minusDays(10)); // -10 dias
        Invoice invFuture = createMockInvoice(2L, "FT-02", c, new BigDecimal("2000.00"), BigDecimal.ZERO,
                ref.minusDays(10), ref.plusDays(5)); // +5 dias

        when(invoiceRepository.findOutstandingWithClient(eq(1L), any()))
                .thenReturn(List.of(invFuture, invLate));

        List<CollectionRadarItemDTO> allItems = radarService.getRadarItems(1L, null, ref);
        assertEquals(2, allItems.size());
        // Mais urgente (-10 dias) primeiro
        assertEquals("FT-01", allItems.get(0).invoiceNumber());
        assertEquals("FT-02", allItems.get(1).invoiceNumber());

        List<CollectionRadarItemDTO> overdueOnly = radarService.getRadarItems(1L, CollectionRadarBucket.RECENT_OVERDUE, ref);
        assertEquals(1, overdueOnly.size());
        assertEquals("FT-01", overdueOnly.get(0).invoiceNumber());
    }

    @Test
    @DisplayName("Garante geração de lembretes personalizados nos canais WhatsApp, SMS e Email em pt-MZ")
    void testReminderTemplateGeneration() {
        Client c = new Client();
        c.setName("Construtora Matola Lda");
        c.setEmail("financeiro@matola.co.mz");

        Invoice inv = createMockInvoice(10L, "FT-2026/99", c, new BigDecimal("50000.00"), BigDecimal.ZERO,
                LocalDate.now().minusDays(45), LocalDate.now().minusDays(15));

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(inv));

        CollectionReminderTemplateDTO wa = radarService.generateReminderTemplate(10L, "WHATSAPP");
        assertNotNull(wa);
        assertEquals("WHATSAPP", wa.channel());
        assertTrue(wa.message().contains("Construtora Matola Lda"));
        assertTrue(wa.message().contains("FT-2026/99"));
        assertTrue(wa.message().contains("50,000.00 MT"));
        assertTrue(wa.message().contains("atraso"));

        CollectionReminderTemplateDTO email = radarService.generateReminderTemplate(10L, "EMAIL");
        assertNotNull(email);
        assertEquals("EMAIL", email.channel());
        assertTrue(email.subject().contains("FT-2026/99"));
        assertTrue(email.message().contains("Departamento Financeiro e Cobranças"));

        CollectionReminderTemplateDTO sms = radarService.generateReminderTemplate(10L, "SMS");
        assertNotNull(sms);
        assertEquals("SMS", sms.channel());
        assertTrue(sms.message().startsWith("Multicore ERP:"));
    }
}
