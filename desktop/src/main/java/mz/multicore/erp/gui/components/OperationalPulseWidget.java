package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.desktop.client.DashboardApiClient;
import mz.multicore.erp.modules.dashboard.dto.OperationalPulseDTO;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Cockpit Operacional & Industrial 360° no Dashboard principal.
 * Consolida as métricas estratégicas dos 4 novos módulos empresariais:
 * 1. Manufatura & Produção Industrial
 * 2. Gestão de Frotas & Viaturas
 * 3. Contratos & Faturação Recorrente
 * 4. Activos Fixos & Património
 */
public class OperationalPulseWidget extends ModernPanel {

    private final DashboardApiClient dashboardApiClient;
    private final Consumer<String> navigationHandler;

    private final JLabel manufacturingValLabel = new JLabel("—");
    private final JLabel manufacturingSubLabel = new JLabel("A carregar produção...");
    private final ModernButton manufacturingNavBtn = UIHelper.createSecondaryButton("Chão de Fábrica");

    private final JLabel fleetValLabel = new JLabel("—");
    private final JLabel fleetSubLabel = new JLabel("A carregar frotas...");
    private final ModernButton fleetNavBtn = UIHelper.createSecondaryButton("Gestão de Frotas");

    private final JLabel recurringValLabel = new JLabel("—");
    private final JLabel recurringSubLabel = new JLabel("A carregar contratos...");
    private final ModernButton recurringNavBtn = UIHelper.createSecondaryButton("Ver Contratos");

    private final JLabel assetsValLabel = new JLabel("—");
    private final JLabel assetsSubLabel = new JLabel("A carregar património...");
    private final ModernButton assetsNavBtn = UIHelper.createSecondaryButton("Activos Fixos");

    private final DecimalFormat currencyFormatter;

    public OperationalPulseWidget(
            DashboardApiClient dashboardApiClient,
            Consumer<String> navigationHandler
    ) {
        super(UIHelper.RADIUS_LG, UIHelper.BG_CARD);
        this.dashboardApiClient = dashboardApiClient;
        this.navigationHandler = navigationHandler;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.currencyFormatter = new DecimalFormat("#,##0.00 MT", symbols);

        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));

        buildHeader();
        buildCards();
        wireActions();
    }

    private void buildHeader() {
        add(StrategicPulseCardFactory.buildHeader("fas-cogs", "Operações e indústria",
                "Produção, frota, contratos recorrentes e imobilizado.", this::loadData), BorderLayout.NORTH);
    }

    private void buildCards() {
        JPanel grid = KpiCard.createHeroGrid(4, 12, 12);

        grid.add(StrategicPulseCardFactory.createPillarCard(
                "MANUFATURA & PRODUÇÃO",
                "fas-industry",
                UIHelper.ACCENT_BLUE,
                manufacturingValLabel,
                manufacturingSubLabel,
                manufacturingNavBtn));

        grid.add(StrategicPulseCardFactory.createPillarCard(
                "FROTAS & COMBUSTÍVEIS",
                "fas-truck",
                UIHelper.ACCENT_ORANGE,
                fleetValLabel,
                fleetSubLabel,
                fleetNavBtn));

        grid.add(StrategicPulseCardFactory.createPillarCard(
                "CONTRATOS RECORRENTES",
                "fas-file-signature",
                UIHelper.APPROVED_GREEN,
                recurringValLabel,
                recurringSubLabel,
                recurringNavBtn));

        grid.add(StrategicPulseCardFactory.createPillarCard(
                "PATRIMÓNIO & ATIVOS",
                "fas-landmark",
                UIHelper.KPI_PURPLE_SOFT,
                assetsValLabel,
                assetsSubLabel,
                assetsNavBtn));

        add(grid, BorderLayout.CENTER);
    }

    private void wireActions() {
        manufacturingNavBtn.addActionListener(e -> navigateSafely("manufacturing"));
        fleetNavBtn.addActionListener(e -> navigateSafely("fleet"));
        recurringNavBtn.addActionListener(e -> navigateSafely("recurring"));
        assetsNavBtn.addActionListener(e -> navigateSafely("assets"));
    }

    private void navigateSafely(String target) {
        if (navigationHandler != null && target != null) {
            navigationHandler.accept(target);
        }
    }

    public void loadData() {
        if (dashboardApiClient == null) {
            return;
        }

        manufacturingSubLabel.setText("A sincronizar...");
        fleetSubLabel.setText("A sincronizar...");
        recurringSubLabel.setText("A sincronizar...");
        assetsSubLabel.setText("A sincronizar...");

        new SwingWorker<OperationalPulseDTO, Void>() {
            @Override
            protected OperationalPulseDTO doInBackground() {
                try {
                    return dashboardApiClient.getOperationalPulse();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    OperationalPulseDTO data = get();
                    if (data != null) {
                        applyData(data);
                    } else {
                        applyOfflineState();
                    }
                } catch (Exception e) {
                    applyOfflineState();
                }
            }
        }.execute();
    }

    private void applyData(OperationalPulseDTO pulse) {
        if (pulse.manufacturing() != null) {
            long activeOrders = pulse.manufacturing().plannedOrders() + pulse.manufacturing().inProgressOrders();
            manufacturingValLabel.setText(activeOrders + " Ordens");
            manufacturingSubLabel.setText(pulse.manufacturing().inProgressOrders() + " em fabrico • CIP: "
                    + currencyFormatter.format(pulse.manufacturing().totalCipValue()));
        }

        if (pulse.fleet() != null) {
            fleetValLabel.setText(pulse.fleet().activeVehicles() + " Viaturas Activas");
            fleetSubLabel.setText("30d: " + String.format(DisplayLocale.NUMBERS, "%,.1f L", pulse.fleet().monthlyFuelLiters())
                    + " (" + currencyFormatter.format(pulse.fleet().monthlyFuelCost()) + ")");
        }

        if (pulse.recurring() != null) {
            recurringValLabel.setText(pulse.recurring().activeContracts() + " Contratos");
            recurringSubLabel.setText("MRR: " + currencyFormatter.format(pulse.recurring().monthlyRecurringRevenue())
                    + " • " + pulse.recurring().contractsDueBilling() + " a faturar");
        }

        if (pulse.assets() != null) {
            assetsValLabel.setText(pulse.assets().totalAssets() + " Activos");
            assetsSubLabel.setText("VLC: " + currencyFormatter.format(pulse.assets().netBookValue())
                    + " • Amort. " + currencyFormatter.format(pulse.assets().accumulatedDepreciation()));
        }
    }

    private void applyOfflineState() {
        manufacturingValLabel.setText("0 Ordens");
        manufacturingSubLabel.setText("Sem ordens ativas");
        fleetValLabel.setText("0 Viaturas");
        fleetSubLabel.setText("Sem viaturas");
        recurringValLabel.setText("0 Contratos");
        recurringSubLabel.setText("MRR: 0,00 MT");
        assetsValLabel.setText("0 Activos");
        assetsSubLabel.setText("VLC: 0,00 MT");
    }
}
