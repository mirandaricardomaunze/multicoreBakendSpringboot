package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimplePieChartTest {

    @Test
    void instantiatesAndUpdatesData() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SimplePieChart chart = new SimplePieChart("Distribuição de Vendas");
            assertNotNull(chart);

            String[] labels = {"Balcão POS", "Comercial", "Fatura-Recibo"};
            BigDecimal[] values = {
                    BigDecimal.valueOf(15000),
                    BigDecimal.valueOf(35000),
                    BigDecimal.valueOf(10000)
            };
            Color[] colors = {
                    UIHelper.ACCENT_BLUE,
                    UIHelper.APPROVED_GREEN,
                    UIHelper.PENDING_YELLOW
            };

            chart.setData(labels, values, colors);

            assertEquals(3, chart.getLabels().length);
            assertEquals(3, chart.getValues().length);
            assertEquals("Balcão POS", chart.getLabels()[0]);
            assertEquals(BigDecimal.valueOf(15000), chart.getValues()[0]);
        });
    }

    @Test
    void rendersEmptyStateAndPopulatedStateGracefully() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SimplePieChart chart = new SimplePieChart("Vendas por Canal", true);
            chart.setSize(new Dimension(340, 220));

            BufferedImage img1 = new BufferedImage(340, 220, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1 = img1.createGraphics();
            assertDoesNotThrow(() -> chart.paintComponent(g1));
            g1.dispose();

            chart.setData(
                    new String[]{"POS", "Faturas"},
                    new BigDecimal[]{BigDecimal.valueOf(2500), BigDecimal.valueOf(7500)},
                    new Color[]{UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN}
            );

            BufferedImage img2 = new BufferedImage(340, 220, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img2.createGraphics();
            assertDoesNotThrow(() -> chart.paintComponent(g2));
            g2.dispose();
        });
    }
}
