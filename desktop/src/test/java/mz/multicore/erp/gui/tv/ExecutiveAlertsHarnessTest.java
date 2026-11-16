package mz.multicore.erp.gui.tv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

import static org.assertj.core.api.Assertions.assertThat;

/** The Painel Executivo footer warns about anti-fraud exceptions, and stays quiet when there are none. */
class ExecutiveAlertsHarnessTest {

    @AfterEach
    void reset() {
        ExecutiveAlerts.register(null);
    }

    @Test
    @DisplayName("EA-01: High and medium exceptions appear in the footer centre")
    void footerShowsExceptions() {
        ExecutiveAlerts.setForTest(new ExecutiveAlerts.Counts(2, 3));
        JPanel footer = ExecutiveTvFooterBuilder.build(false, "Actualizado às 10:00", true);
        Component centre = ((BorderLayout) footer.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertThat(centre).isInstanceOf(JLabel.class);
        assertThat(((JLabel) centre).getText()).contains("2 altas").contains("3 médias");
    }

    @Test
    @DisplayName("EA-02: No exceptions, or no source, means no alert")
    void quietWithoutExceptions() {
        ExecutiveAlerts.setForTest(new ExecutiveAlerts.Counts(0, 0));
        JPanel footer = ExecutiveTvFooterBuilder.build(true, "Actualizado às 10:00", true);
        assertThat(((BorderLayout) footer.getLayout()).getLayoutComponent(BorderLayout.CENTER)).isNull();
        assertThat(ExecutiveAlerts.text(null)).isNull();
        assertThat(ExecutiveAlerts.text(new ExecutiveAlerts.Counts(1, 0))).contains("1 alta").contains("0 médias");
    }
}
