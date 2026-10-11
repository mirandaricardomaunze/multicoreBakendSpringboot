package mz.multicore.erp.gui.components;

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
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        this.currencyFormatter = new DecimalFormat("#,##0.00 MT", symbols);

        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        buildHeader();
        buildCards();
        wireActions();
    }

    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon("fas-cogs", 18, UIHelper.ACCENT_BLUE));
        JLabel title = new JLabel("COCKPIT OPERACIONAL & INDUSTRIAL (GESTÃO 360°)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        title.setForeground(UIHelper.TEXT_LIGHT);
        titleRow.add(icon);
        titleRow.add(title);
        titleBox.add(titleRow);

        JLabel sub = new JLabel("Produção fabril, logística de viaturas, carteira de contratos e imobilizado corporativo.");
        sub.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        sub.setForeground(UIHelper.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(3, 26, 0, 0));
        titleBox.add(sub);

        header.add(titleBox, BorderLayout.WEST);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadData);
        refreshBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        refreshBtn.setPreferredSize(new Dimension(135, 28));
        header.add(refreshBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void buildCards() {
        JPanel grid = KpiCard.createGrid(4, 12, 12);

        grid.add(createPillarCard(
                "MANUFATURA & PRODUÇÃO",
                "fas-industry",
                UIHelper.ACCENT_BLUE,
                manufacturingValLabel,
                manufacturingSubLabel,
                manufacturingNavBtn,
                "fas-tools"
        ));

        grid.add(createPillarCard(
                "FROTAS & COMBUSTÍVEIS",
                "fas-truck",
                UIHelper.ACCENT_ORANGE,
                fleetValLabel,
                fleetSubLabel,
                fleetNavBtn,
                "fas-gas-pump"
        ));

        grid.add(createPillarCard(
                "CONTRATOS RECORRENTES",
                "fas-file-signature",
                UIHelper.APPROVED_GREEN,
                recurringValLabel,
                recurringSubLabel,
                recurringNavBtn,
                "fas-calendar-check"
        ));

        grid.add(createPillarCard(
                "PATRIMÓNIO & ATIVOS",
                "fas-landmark",
                UIHelper.KPI_PURPLE_SOFT,
                assetsValLabel,
                assetsSubLabel,
                assetsNavBtn,
                "fas-boxes"
        ));

        add(grid, BorderLayout.CENTER);
    }

    private ModernPanel createPillarCard(
            String title,
            String iconCode,
            Color accentColor,
            JLabel valueLabel,
            JLabel subLabel,
            ModernButton actionBtn,
            String actionIcon
    ) {
        KpiCard.KpiPalette pal = KpiCard.resolvePalette(accentColor);

        ModernPanel card = new ModernPanel(UIHelper.RADIUS_MD, pal.bg);
        card.putClientProperty("card.border", pal.border);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(card.getPreferredSize().width, KpiCard.STANDARD_CARD_HEIGHT));
        card.setMinimumSize(new Dimension(KpiCard.STANDARD_CARD_MIN_WIDTH, KpiCard.STANDARD_CARD_HEIGHT));

        JPanel cardHeader = new JPanel(new BorderLayout(6, 0));
        cardHeader.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(pal.title);

        JLabel iconLbl = new JLabel(UIHelper.icon(iconCode, 14, pal.title));
        cardHeader.add(iconLbl, BorderLayout.WEST);
        cardHeader.add(titleLbl, BorderLayout.CENTER);
        card.add(cardHeader, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        valueLabel.setForeground(pal.value);
        valueLabel.setAlignmentX(0.0f);

        subLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        subLabel.setForeground(pal.subtitle);
        subLabel.setAlignmentX(0.0f);

        content.add(valueLabel);
        content.add(Box.createVerticalStrut(3));
        content.add(subLabel);
        card.add(content, BorderLayout.CENTER);

        actionBtn.setIcon(UIHelper.icon(actionIcon, 11));
        actionBtn.setPreferredSize(new Dimension(0, 26));
        actionBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        card.add(actionBtn, BorderLayout.SOUTH);

        return card;
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
            fleetSubLabel.setText("30d: " + String.format(Locale.forLanguageTag("pt-MZ"), "%,.1f L", pulse.fleet().monthlyFuelLiters())
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
        recurringSubLabel.setText("MRR: 0.00 MT");
        assetsValLabel.setText("0 Activos");
        assetsSubLabel.setText("VLC: 0.00 MT");
    }
}
