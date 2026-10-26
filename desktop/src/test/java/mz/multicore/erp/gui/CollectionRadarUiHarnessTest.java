package mz.multicore.erp.gui;

import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CollectionRadarUiHarnessTest {

    @Test
    @DisplayName("Garante classificação precisa de todos os horizontes temporais do Radar de Cobranças")
    void testCollectionRadarBucketBoundaries() {
        // Fronteira crítica: > 15 dias de atraso (daysUntilDue < -15)
        assertEquals(CollectionRadarBucket.CRITICAL_OVERDUE, CollectionRadarBucket.of(-30));
        assertEquals(CollectionRadarBucket.CRITICAL_OVERDUE, CollectionRadarBucket.of(-16));

        // Fronteira recente: 1 a 15 dias de atraso (-15 a -1)
        assertEquals(CollectionRadarBucket.RECENT_OVERDUE, CollectionRadarBucket.of(-15));
        assertEquals(CollectionRadarBucket.RECENT_OVERDUE, CollectionRadarBucket.of(-1));

        // Vence hoje: 0
        assertEquals(CollectionRadarBucket.DUE_TODAY, CollectionRadarBucket.of(0));

        // Preventivo a 7 dias: 1 a 7 dias
        assertEquals(CollectionRadarBucket.DUE_IN_7_DAYS, CollectionRadarBucket.of(1));
        assertEquals(CollectionRadarBucket.DUE_IN_7_DAYS, CollectionRadarBucket.of(7));

        // Preventivo a 15 dias: 8 a 15 dias
        assertEquals(CollectionRadarBucket.DUE_IN_15_DAYS, CollectionRadarBucket.of(8));
        assertEquals(CollectionRadarBucket.DUE_IN_15_DAYS, CollectionRadarBucket.of(15));

        // Preventivo a 30 dias: 16 a 30 dias
        assertEquals(CollectionRadarBucket.DUE_IN_30_DAYS, CollectionRadarBucket.of(16));
        assertEquals(CollectionRadarBucket.DUE_IN_30_DAYS, CollectionRadarBucket.of(30));

        // Futuro distante: > 30 dias
        assertEquals(CollectionRadarBucket.FUTURE, CollectionRadarBucket.of(31));
        assertEquals(CollectionRadarBucket.FUTURE, CollectionRadarBucket.of(60));
    }

    @Test
    @DisplayName("Garante integridade e serialização dos DTOs de resumo e itens do Radar")
    void testRadarDtoIntegrity() {
        CollectionRadarSummaryDTO summary = new CollectionRadarSummaryDTO(
                1L,
                LocalDate.of(2026, 10, 20),
                new BigDecimal("15000.00"), 3,
                new BigDecimal("8000.00"), 2,
                new BigDecimal("4500.00"), 1,
                new BigDecimal("12000.00"), 4,
                new BigDecimal("6000.00"), 2,
                new BigDecimal("25000.00"), 5,
                new BigDecimal("70500.00"), 17
        );

        assertNotNull(summary);
        assertEquals(1L, summary.companyId());
        assertEquals(17, summary.totalInvoicesCount());
        assertEquals(new BigDecimal("70500.00"), summary.totalReceivable());
        assertEquals(3, summary.criticalOverdueCount());

        CollectionRadarItemDTO item = new CollectionRadarItemDTO(
                101L,
                "FT-2026/101",
                55L,
                "Distribuidora Maputo Sul",
                "400998877",
                "841122334",
                "financeiro@maputosul.mz",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1),
                new BigDecimal("20000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("15000.00"),
                -19,
                CollectionRadarBucket.CRITICAL_OVERDUE
        );

        assertNotNull(item);
        assertEquals("FT-2026/101", item.invoiceNumber());
        assertEquals("Distribuidora Maputo Sul", item.clientName());
        assertEquals(new BigDecimal("15000.00"), item.remainingBalance());
        assertEquals(-19, item.daysUntilDue());
        assertTrue(item.bucket().isOverdue());
    }

    @Test
    @DisplayName("Garante formatação de mensagem e metadados de lembrete preventivo")
    void testReminderTemplateDto() {
        CollectionReminderTemplateDTO reminder = new CollectionReminderTemplateDTO(
                200L,
                "FT-2026/200",
                "Transportes do Zambeze Lda",
                "WHATSAPP",
                "849988776",
                "Aviso de Regularização",
                "Olá, Transportes do Zambeze Lda! Lembramos a fatura FT-2026/200."
        );

        assertNotNull(reminder);
        assertEquals(200L, reminder.invoiceId());
        assertEquals("WHATSAPP", reminder.channel());
        assertEquals("849988776", reminder.recipientContact());
        assertTrue(reminder.message().contains("FT-2026/200"));
    }
}
