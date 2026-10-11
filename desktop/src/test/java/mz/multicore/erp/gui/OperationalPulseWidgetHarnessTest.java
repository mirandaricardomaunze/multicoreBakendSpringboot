package mz.multicore.erp.gui;

import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalPulseWidgetHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("desktop")) ? Path.of("") : Path.of("..");

    @Test
    void contractsRemainPureAndImmutableRecord() {
        OperationalPulseDTO empty = OperationalPulseDTO.empty();
        assertThat(empty).isNotNull();
        assertThat(empty.manufacturing().totalCipValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.fleet().totalVehicles()).isZero();
        assertThat(empty.recurring().activeContracts()).isZero();
        assertThat(empty.assets().netBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void widgetAdheresToCanonicalDesignSystem() throws Exception {
        String widgetSource = Files.readString(ROOT.resolve("desktop/src/main/java/mz/multicore/erp/gui/components/OperationalPulseWidget.java"));
        assertThat(widgetSource)
                .contains("COCKPIT OPERACIONAL & INDUSTRIAL", "MANUFATURA & PRODUÇÃO", "FROTAS & COMBUSTÍVEIS",
                        "CONTRATOS RECORRENTES", "PATRIMÓNIO & ATIVOS",
                        "KpiCard.createGrid(4, 12, 12)", "KpiCard.STANDARD_CARD_HEIGHT",
                        "Chão de Fábrica", "Gestão de Frotas", "Ver Contratos", "Activos Fixos")
                .doesNotContain("JOptionPane", "printStackTrace");
    }

    @Test
    void dashboardIntegratesOperationalPulseWidget() throws Exception {
        String dashboardSource = Files.readString(ROOT.resolve("desktop/src/main/java/mz/multicore/erp/gui/DashboardPanel.java"));
        assertThat(dashboardSource)
                .contains("private final OperationalPulseWidget operationalPulseWidget;",
                        "centerContainer.add(operationalPulseWidget);",
                        "operationalPulseWidget.loadData();",
                        "getOperationalPulseWidget()");
    }
}
