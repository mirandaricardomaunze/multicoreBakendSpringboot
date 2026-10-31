package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRenewalDialogDecompositionHarnessTest {

    @Test
    void calculatesSubscriptionPricingAndDiscountsAccurately() {
        assertEquals("PRO", SubscriptionRenewalPricingCalculator.planKeyForIndex(0));
        assertEquals("BASIC", SubscriptionRenewalPricingCalculator.planKeyForIndex(1));
        assertEquals("ENTERPRISE", SubscriptionRenewalPricingCalculator.planKeyForIndex(2));

        assertEquals(0, new BigDecimal("3500.00").compareTo(SubscriptionRenewalPricingCalculator.calculateTotal("PRO", 1)));
        assertEquals(0, new BigDecimal("35700.00").compareTo(SubscriptionRenewalPricingCalculator.calculateTotal("PRO", 12)));
        assertEquals("(15% de Desconto Anual Aplicado)", SubscriptionRenewalPricingCalculator.discountLabelForMonths(12));
    }

    @Test
    void enforcesPhysicalLineLimits() throws IOException {
        Path base = resolveDesktopDir().resolve("src/main/java/mz/multicore/erp/gui");
        long facadeLines = Files.readAllLines(base.resolve("SubscriptionRenewalDialog.java")).size();
        long calcLines = Files.readAllLines(base.resolve("SubscriptionRenewalPricingCalculator.java")).size();
        long viewLines = Files.readAllLines(base.resolve("SubscriptionRenewalViewBuilder.java")).size();

        assertTrue(facadeLines <= 190, "SubscriptionRenewalDialog.java deve ter <= 190 linhas, mas tem " + facadeLines);
        assertTrue(calcLines <= 75, "SubscriptionRenewalPricingCalculator.java deve ter <= 75 linhas, mas tem " + calcLines);
        assertTrue(viewLines <= 155, "SubscriptionRenewalViewBuilder.java deve ter <= 155 linhas, mas tem " + viewLines);
    }

    private static Path resolveDesktopDir() {
        Path cwd = Path.of("").toAbsolutePath();
        return (cwd.getFileName() != null && "desktop".equalsIgnoreCase(cwd.getFileName().toString()))
                ? cwd : cwd.resolve("desktop");
    }
}
