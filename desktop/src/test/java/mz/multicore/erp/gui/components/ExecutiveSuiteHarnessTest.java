package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.MultiCurrencyEngine.Currency;
import mz.multicore.erp.gui.components.ProfitEngine.ProfitMetrics;
import mz.multicore.erp.gui.components.SmartAlertsDialog.SmartAlert;
import mz.multicore.erp.gui.components.SmartAlertsDialog.Urgency;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EX-01 a EX-06: Testes Automatizados do Pacote Executivo e Operacional")
class ExecutiveSuiteHarnessTest {

    @Test
    @DisplayName("EX-01: SmartAlerts categoriza criticidade e gera ações proativas")
    void ex01_smartAlertsClassification() {
        NotificationItem critItem = new NotificationItem("Validades", "Lote vencido: Paracetamol", "Lote 123", "01/01/2026", "stock", 3);
        SmartAlert alert1 = SmartAlert.fromNotification(critItem);
        assertEquals(Urgency.CRITICAL, alert1.urgency());
        assertEquals("stock", alert1.targetModule());
        assertTrue(alert1.actionLabel().contains("Resolver") || alert1.actionLabel().contains("Abrir"));

        NotificationItem warnItem = new NotificationItem("Stock", "Stock baixo: Arroz 5kg", "Qtd 2 un", "Repor", "stock", 2);
        SmartAlert alert2 = SmartAlert.fromNotification(warnItem);
        assertEquals(Urgency.WARNING, alert2.urgency());
        assertEquals("stock", alert2.targetModule());

        NotificationItem infoItem = new NotificationItem("Assinatura", "Licença Pro", "Válida", "OK", "config", 1);
        SmartAlert alert3 = SmartAlert.fromNotification(infoItem);
        assertEquals(Urgency.INFO, alert3.urgency());
        assertEquals("config", alert3.targetModule());
    }

    @Test
    @DisplayName("EX-02: PosDirectPrintEngine controla preferência de impressão direta e valida payload")
    void ex02_posDirectPrintEngine() {
        assertFalse(PosDirectPrintEngine.isDirectPrintEnabled());
        PosDirectPrintEngine.setDirectPrintEnabled(true);
        assertTrue(PosDirectPrintEngine.isDirectPrintEnabled());
        PosDirectPrintEngine.setDirectPrintEnabled(false);
        assertFalse(PosDirectPrintEngine.isDirectPrintEnabled());

        // Teste de resiliência com payload nulo
        assertFalse(PosDirectPrintEngine.printReceiptSilent(null, "job", null));
        assertFalse(PosDirectPrintEngine.printReceiptSilent(new byte[0], "job", null));
    }

    @Test
    @DisplayName("EX-03: ProfitEngine calcula CMVMC, Lucro Bruto, Margem % e Ticket Médio")
    void ex03_profitEngineMetrics() {
        InvoiceLineDTO line1 = new InvoiceLineDTO(
                1L, 1L, "Laptop Dell", "SKU-001", BigDecimal.valueOf(2),
                new BigDecimal("50000.00"), BigDecimal.ZERO, new BigDecimal("100000.00"),
                BigDecimal.ZERO, null, null
        );

        InvoiceDTO inv1 = new InvoiceDTO(
                1L, "FT-001", 1L, "Cliente A", "123456789",
                new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("100000.00"),
                BigDecimal.ZERO, InvoiceStatus.APPROVED, null,
                List.of(line1), LocalDateTime.now(), "admin",
                LocalDate.now().plusDays(30), 0, AgingBucket.CORRENTE
        );

        ProductDTO prod = new ProductDTO(
                1L, "SKU-001", "REF-001", "123456789", "Laptop Dell",
                new BigDecimal("50000.00"), new BigDecimal("35000.00"), BigDecimal.TEN,
                null, null, 1, "UNIT", true, 1L, "Informática",
                null, BigDecimal.ZERO, "Isento", "Laptop", null, null, null
        );

        ProfitMetrics metrics = ProfitEngine.calculateMetrics(List.of(inv1), BigDecimal.ZERO, List.of(prod));

        assertEquals(new BigDecimal("100000.00"), metrics.totalRevenue());
        // Custo = 2 * 35000 = 70000
        assertEquals(new BigDecimal("70000.00"), metrics.totalCogs());
        // Lucro = 100000 - 70000 = 30000
        assertEquals(new BigDecimal("30000.00"), metrics.grossProfit());
        // Margem % = 30000 / 100000 * 100 = 30.0%
        assertEquals(new BigDecimal("30.0"), metrics.profitMarginPercent());
        // Ticket médio = 100000 / 1 = 100000.00
        assertEquals(new BigDecimal("100000.00"), metrics.averageTicket());
        assertEquals(1, metrics.transactionCount());
    }

    @Test
    @DisplayName("EX-04: MultiCurrencyEngine converte valores entre MZN, USD, ZAR, EUR e calcula troco")
    void ex04_multiCurrencyConversion() {
        BigDecimal totalMzn = new BigDecimal("6383.00");

        // 6383 MZN em USD (taxa 63.83) = 100.00 USD
        BigDecimal inUsd = MultiCurrencyEngine.convertToForeign(totalMzn, Currency.USD);
        assertEquals(new BigDecimal("100.00"), inUsd);

        // 100 USD em MZN = 6383.00 MZN
        BigDecimal backToMzn = MultiCurrencyEngine.convertToMzn(inUsd, Currency.USD);
        assertEquals(new BigDecimal("6383.00"), backToMzn);

        // Cliente dá 120 USD (7659.60 MT) para pagar 6383.00 MT -> Troco = 1276.60 MT
        BigDecimal change = MultiCurrencyEngine.calculateChangeInMzn(totalMzn, new BigDecimal("120.00"), Currency.USD);
        assertEquals(new BigDecimal("1276.60"), change);

        // Formatação
        assertEquals("100.00 MT", MultiCurrencyEngine.formatCurrency(new BigDecimal("100"), Currency.MZN));
        assertEquals("$ 100.00", MultiCurrencyEngine.formatCurrency(new BigDecimal("100"), Currency.USD));
        assertEquals("R 100.00", MultiCurrencyEngine.formatCurrency(new BigDecimal("100"), Currency.ZAR));
        assertEquals("€ 100.00", MultiCurrencyEngine.formatCurrency(new BigDecimal("100"), Currency.EUR));
    }

    @Test
    @DisplayName("EX-05: LoyaltyEngine atribui pontos por compra e calcula resgate")
    void ex05_loyaltyPointsAndRedemption() {
        // Compra de 550 MT -> 5 pontos (1 ponto por cada 100 MT)
        BigDecimal points = LoyaltyEngine.calculateEarnedPoints(new BigDecimal("550.00"));
        assertEquals(new BigDecimal("5"), points);

        // Valor de 5 pontos em MT = 5.00 MT
        assertEquals(new BigDecimal("5.00"), LoyaltyEngine.pointsToValueMzn(points));

        // Resgate: Cliente tem 50 pontos (50 MT) e compra de 30 MT -> Max resgatável é 30 pontos
        BigDecimal maxRedeem = LoyaltyEngine.calculateMaxRedeemablePoints(new BigDecimal("50"), new BigDecimal("30.00"));
        assertEquals(new BigDecimal("30"), maxRedeem);

        LoyaltyEngine.RedemptionResult result = LoyaltyEngine.applyRedemption(
                new BigDecimal("50"), new BigDecimal("30.00"), new BigDecimal("50"));
        assertEquals(new BigDecimal("30"), result.pointsRedeemed());
        assertEquals(new BigDecimal("30.00"), result.discountMzn());
        assertEquals(new BigDecimal("0.00"), result.finalPayableAmount());
        assertEquals(new BigDecimal("20"), result.remainingPoints());
    }

    @Test
    @DisplayName("EX-06: Instanciação dos diálogos sem erros")
    void ex06_dialogsInstantiation() {
        ProfitAnalyticsWidget widget = new ProfitAnalyticsWidget();
        assertNotNull(widget);

        ProfitMetrics emptyMetrics = new ProfitMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        assertDoesNotThrow(() -> widget.updateMetrics(emptyMetrics));
    }
}
