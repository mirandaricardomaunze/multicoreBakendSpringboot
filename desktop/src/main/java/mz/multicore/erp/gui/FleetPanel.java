package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel de Gestão de Frotas & Viaturas.
 * Odómetros, controlo de combustível (Km/L) e monitorização operacional.
 */
public final class FleetPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat KM_FMT = new DecimalFormat("#,##0 Km");
    private static final DecimalFormat CONSUMPTION_FMT = new DecimalFormat("#0.0 Km/L");

    private final FleetApiClient fleetApiClient;
    private final FuelOperationsCoordinator fuelCoordinator;

    final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JLabel kpiActiveCount = new JLabel("0 Viaturas");
    private final JLabel kpiTotalKm = new JLabel("0 Km");
    private final JLabel kpiAvgConsumption = new JLabel("0.0 Km/L");
    private final JLabel kpiMaintenanceCount = new JLabel("0 Viaturas");

    private final DefaultTableModel tableModel;
    final JTable table;
    private List<VehicleDTO> currentVehicles = new ArrayList<>();

    private final JTextField searchField = TableFilter.searchField("Pesquisar por matrícula, marca, modelo ou condutor...");
    private final JComboBox<String> fuelFilterCombo = TableFilter.combo("Todos os Combustíveis", "Gasóleo (Diesel)", "Gasolina", "Híbrido", "Elétrico");
    private final JComboBox<String> statusFilterCombo = TableFilter.combo("Todos os Estados", "Operacional", "Manutenção", "Inspecção", "Fora de serviço", "Vendido");

    public FleetPanel(FleetApiClient fleetApiClient) {
        this.fleetApiClient = fleetApiClient;
        this.fuelCoordinator = new FuelOperationsCoordinator(fleetApiClient);

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UIHelper.createHeading("Gestão de Frotas & Viaturas"), BorderLayout.WEST);

        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(feedback);

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createCard("Viaturas em Operação", kpiActiveCount, "Frota operacional activa",
                "fas-car", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Quilometragem Total", kpiTotalKm, "Distância acumulada da frota",
                "fas-tachometer-alt", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Média de Consumo", kpiAvgConsumption, "Rendimento médio da frota",
                "fas-gas-pump", UIHelper.MODULE_STOCK));
        kpiGrid.add(KpiCard.createCard("Em Manutenção", kpiMaintenanceCount, "Viaturas na oficina",
                "fas-wrench", UIHelper.PENDING_YELLOW));

        JPanel northStack = new JPanel(new BorderLayout(0, 12));
        northStack.setOpaque(false);
        northStack.add(headerPanel, BorderLayout.NORTH);
        northStack.add(kpiGrid, BorderLayout.CENTER);
        add(northStack, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Matrícula", "Marca & Modelo", "Ano", "Combustível", "Odómetro Actual",
                "Condutor Atribuído", "Consumo Médio", "Seguro Válido Até", "Inspecção Válida Até", "Estado"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        int[] widths = {100, 170, 70, 100, 110, 160, 100, 120, 120, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(table);

        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        table.putClientProperty("customDetailAction", (Runnable) this::showFuelHistory);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        TableFilter.install(table, searchField,
                new TableFilter.ColumnFilter(fuelFilterCombo, 3),
                new TableFilter.ColumnFilter(statusFilterCombo, 9));

        JPanel filterBar = TableFilter.bar(
                searchField,
                TableFilter.label("Combustível:"), fuelFilterCombo,
                TableFilter.label("Estado:"), statusFilterCombo
        );

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> loadVehicles());

        ModernButton manifestBtn = UIHelper.createSecondaryButton("Manifestos de Carga");
        manifestBtn.setIcon(UIHelper.icon("fas-route", 14));
        manifestBtn.setToolTipText("Consultar e emitir manifestos de carga e rotas de distribuição");
        manifestBtn.addActionListener(e -> FleetManifestsViewDialog.show(SwingUtilities.getWindowAncestor(this), fleetApiClient));

        ModernButton fuelBtn = UIHelper.createPrimaryButton("Registar Abastecimento");
        fuelBtn.setIcon(UIHelper.icon("fas-gas-pump", 14));
        fuelBtn.setToolTipText("Registar entrada de combustível e actualizar odómetro");
        fuelBtn.addActionListener(e -> fuelCoordinator.openFuelDialog(SwingUtilities.getWindowAncestor(this), currentVehicles, feedback, this::loadVehicles));

        ModernButton newVehicleBtn = UIHelper.createSuccessButton("Nova Viatura");
        newVehicleBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newVehicleBtn.setToolTipText("Registar nova viatura na frota da empresa");
        newVehicleBtn.addActionListener(e -> VehicleFormDialog.show(SwingUtilities.getWindowAncestor(this), fleetApiClient, feedback, this::loadVehicles));

        card.add(UIHelper.tableCardTop("Inventário e Monitorização Operacional de Viaturas", filterBar,
                refreshBtn, manifestBtn, fuelBtn, newVehicleBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        loadVehicles();
    }

    private void loadVehicles() {
        feedback.clear();
        if (fleetApiClient == null) return;
        SwingWorker<List<VehicleDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<VehicleDTO> doInBackground() {
                return fleetApiClient.listVehicles();
            }

            @Override
            protected void done() {
                try {
                    currentVehicles = get();
                    updateTableAndKpis(currentVehicles);
                } catch (Exception ex) {
                    feedback.show(FeedbackType.ERROR, "Falha ao carregar frota: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateTableAndKpis(List<VehicleDTO> vehicles) {
        tableModel.setRowCount(0);
        int active = 0;
        int maintenance = 0;
        BigDecimal totalKm = BigDecimal.ZERO;
        BigDecimal totalConsumption = BigDecimal.ZERO;
        int withConsumptionCount = 0;

        for (VehicleDTO v : vehicles) {
            if (v.status() == VehicleStatus.OPERATIONAL) active++;
            if (v.status() == VehicleStatus.MAINTENANCE) maintenance++;
            totalKm = totalKm.add(v.currentOdometerKm() != null ? v.currentOdometerKm() : BigDecimal.ZERO);

            if (v.averageFuelConsumptionKmPerLiter() != null && v.averageFuelConsumptionKmPerLiter().compareTo(BigDecimal.ZERO) > 0) {
                totalConsumption = totalConsumption.add(v.averageFuelConsumptionKmPerLiter());
                withConsumptionCount++;
            }

            String insDate = v.insuranceExpiryDate() != null ? v.insuranceExpiryDate().format(DATE_FMT) : "—";
            String inspDate = v.inspectionExpiryDate() != null ? v.inspectionExpiryDate().format(DATE_FMT) : "—";
            String odoStr = v.currentOdometerKm() != null ? KM_FMT.format(v.currentOdometerKm()) : "0 Km";
            String consStr = v.averageFuelConsumptionKmPerLiter() != null && v.averageFuelConsumptionKmPerLiter().compareTo(BigDecimal.ZERO) > 0
                    ? CONSUMPTION_FMT.format(v.averageFuelConsumptionKmPerLiter())
                    : "—";

            tableModel.addRow(new Object[]{
                    v.licensePlate(),
                    v.make() + " " + v.model(),
                    v.manufacturingYear(),
                    v.fuelType() != null ? v.fuelType().label() : "—",
                    odoStr,
                    v.assignedDriverName() != null ? v.assignedDriverName() : "—",
                    consStr,
                    insDate,
                    inspDate,
                    v.status().name()
            });
        }

        kpiActiveCount.setText(active + " Viaturas");
        kpiTotalKm.setText(KM_FMT.format(totalKm));
        kpiMaintenanceCount.setText(maintenance + " Viaturas");

        if (withConsumptionCount > 0) {
            BigDecimal avg = totalConsumption.divide(BigDecimal.valueOf(withConsumptionCount), 1, RoundingMode.HALF_UP);
            kpiAvgConsumption.setText(CONSUMPTION_FMT.format(avg));
        } else {
            kpiAvgConsumption.setText("0.0 Km/L");
        }
    }

    void showFuelHistory() {
        int row = table.getSelectedRow();
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma viatura",
                    "Seleccione uma viatura na tabela para consultar o histórico de abastecimentos e rendimento.", null, null);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow >= currentVehicles.size()) return;
        VehicleDTO vehicle = currentVehicles.get(modelRow);
        fuelCoordinator.showFuelHistoryDialog(SwingUtilities.getWindowAncestor(this), vehicle, feedback);
    }
}
