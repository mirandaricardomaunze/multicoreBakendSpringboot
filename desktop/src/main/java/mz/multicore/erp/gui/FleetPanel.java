package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;
import mz.multicore.erp.modules.fleet.model.FuelType;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JLabel kpiActiveCount = new JLabel("0 Viaturas");
    private final JLabel kpiTotalKm = new JLabel("0 Km");
    private final JLabel kpiAvgConsumption = new JLabel("0.0 Km/L");
    private final JLabel kpiMaintenanceCount = new JLabel("0 Viaturas");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<VehicleDTO> currentVehicles = new ArrayList<>();

    private final JTextField searchField = TableFilter.searchField("Pesquisar por matrícula, marca, modelo ou condutor...");
    private final JComboBox<String> fuelFilterCombo = TableFilter.combo("Todos os Combustíveis", "Gasóleo (Diesel)", "Gasolina", "Híbrido", "Elétrico");
    private final JComboBox<String> statusFilterCombo = TableFilter.combo("Todos os Estados", "Operacional", "Manutenção", "Inspecção", "Fora de serviço", "Vendido");

    public FleetPanel(FleetApiClient fleetApiClient) {
        this.fleetApiClient = fleetApiClient;

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

        ModernButton fuelBtn = UIHelper.createPrimaryButton("Registar Abastecimento");
        fuelBtn.setIcon(UIHelper.icon("fas-gas-pump", 14));
        fuelBtn.setToolTipText("Registar entrada de combustível e actualizar odómetro");
        fuelBtn.addActionListener(e -> openFuelDialog());

        ModernButton newVehicleBtn = UIHelper.createSuccessButton("Nova Viatura");
        newVehicleBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newVehicleBtn.setToolTipText("Registar nova viatura na frota da empresa");
        newVehicleBtn.addActionListener(e -> openNewVehicleDialog());

        card.add(UIHelper.tableCardTop("Inventário e Monitorização Operacional de Viaturas", filterBar,
                refreshBtn, fuelBtn, newVehicleBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        loadVehicles();
    }

    private void loadVehicles() {
        feedback.clear();
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
            BigDecimal avg = totalConsumption.divide(BigDecimal.valueOf(withConsumptionCount), 1, java.math.RoundingMode.HALF_UP);
            kpiAvgConsumption.setText(CONSUMPTION_FMT.format(avg));
        } else {
            kpiAvgConsumption.setText("0.0 Km/L");
        }
    }

    private void openNewVehicleDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);

        JTextField plateField = new JTextField(10);
        JTextField makeField = new JTextField(15);
        JTextField modelField = new JTextField(15);
        JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JComboBox<FuelType> fuelCombo = new JComboBox<>(FuelType.values());
        UIHelper.styleComboBox(fuelCombo);
        fuelCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FuelType f) {
                    setText(f.label());
                }
                return this;
            }
        });
        JTextField odoField = new JTextField("0", 10);
        JTextField driverField = new JTextField(20);
        DateField insuranceField = new DateField();
        DateField inspectionField = new DateField();

        JPanel form = UIHelper.createDialogForm(
                "Matrícula *:", plateField,
                "Marca *:", makeField,
                "Modelo *:", modelField,
                "Ano de Fabrico *:", yearField,
                "Tipo de Combustível *:", fuelCombo,
                "Odómetro Inicial (Km) *:", odoField,
                "Condutor Habitual:", driverField,
                "Validade do Seguro:", insuranceField,
                "Validade da Inspecção:", inspectionField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Registar Nova Viatura", "fas-car",
                "Adicionar veículo ao controlo da frota empresarial", form);

        dialog.setOnSave(() -> {
            String plate = plateField.getText().trim();
            if (plate.isEmpty()) {
                plateField.requestFocus();
                throw new IllegalArgumentException("A matrícula é obrigatória.");
            }
            String make = makeField.getText().trim();
            if (make.isEmpty()) {
                makeField.requestFocus();
                throw new IllegalArgumentException("A marca é obrigatória.");
            }
            String model = modelField.getText().trim();
            if (model.isEmpty()) {
                modelField.requestFocus();
                throw new IllegalArgumentException("O modelo é obrigatório.");
            }

            int year;
            try {
                year = Integer.parseInt(yearField.getText().trim());
                if (year < 1950 || year > 2100) throw new Exception();
            } catch (Exception ex) {
                yearField.requestFocus();
                throw new IllegalArgumentException("Ano de fabrico inválido.");
            }

            BigDecimal odo;
            try {
                odo = new BigDecimal(odoField.getText().trim().replace(",", "."));
                if (odo.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
            } catch (Exception ex) {
                odoField.requestFocus();
                throw new IllegalArgumentException("Odómetro não pode ser negativo.");
            }

            LocalDate insDate = null;
            if (!insuranceField.getText().trim().isEmpty()) {
                try {
                    insDate = LocalDate.parse(insuranceField.getText().trim());
                } catch (Exception ex) {
                    insuranceField.requestFocus();
                    throw new IllegalArgumentException("Data de seguro inválida. Use AAAA-MM-DD.");
                }
            }

            LocalDate inspDate = null;
            if (!inspectionField.getText().trim().isEmpty()) {
                try {
                    inspDate = LocalDate.parse(inspectionField.getText().trim());
                } catch (Exception ex) {
                    inspectionField.requestFocus();
                    throw new IllegalArgumentException("Data de inspecção inválida. Use AAAA-MM-DD.");
                }
            }

            CreateVehicleRequest req = new CreateVehicleRequest(
                    plate,
                    make,
                    model,
                    year,
                    (FuelType) fuelCombo.getSelectedItem(),
                    odo,
                    driverField.getText().trim().isEmpty() ? null : driverField.getText().trim(),
                    insDate,
                    inspDate
            );

            fleetApiClient.createVehicle(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Viatura registada com sucesso.");
            loadVehicles();
        }
    }

    private void openFuelDialog() {
        if (currentVehicles.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Registe primeiro pelo menos uma viatura.");
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(this);

        JComboBox<VehicleDTO> vehicleCombo = new JComboBox<>(currentVehicles.toArray(new VehicleDTO[0]));
        UIHelper.styleComboBox(vehicleCombo);
        vehicleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof VehicleDTO v) {
                    setText(v.licensePlate() + " - " + v.make() + " " + v.model() + " (Odóm: " + v.currentOdometerKm() + " Km)");
                }
                return this;
            }
        });

        JTextField odoField = new JTextField(10);
        JTextField litersField = new JTextField(8);
        JTextField priceField = new JTextField("86.25", 8);
        JTextField stationField = new JTextField(15);
        JTextField driverField = new JTextField(15);

        vehicleCombo.addActionListener(e -> {
            VehicleDTO v = (VehicleDTO) vehicleCombo.getSelectedItem();
            if (v != null) {
                odoField.setText(v.currentOdometerKm().toPlainString());
                if (v.assignedDriverName() != null) {
                    driverField.setText(v.assignedDriverName());
                }
            }
        });

        if (vehicleCombo.getSelectedItem() != null) {
            VehicleDTO v = (VehicleDTO) vehicleCombo.getSelectedItem();
            odoField.setText(v.currentOdometerKm().toPlainString());
            if (v.assignedDriverName() != null) driverField.setText(v.assignedDriverName());
        }

        JPanel form = UIHelper.createDialogForm(
                "Viatura *:", vehicleCombo,
                "Novo Odómetro (Km) *:", odoField,
                "Litros Abastecidos *:", litersField,
                "Preço por Litro (MT) *:", priceField,
                "Posto de Combustível:", stationField,
                "Condutor / Operador:", driverField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Registar Abastecimento", "fas-gas-pump",
                "Apontamento de combustível e apuramento do rendimento Km/L", form);

        dialog.setOnSave(() -> {
            VehicleDTO v = (VehicleDTO) vehicleCombo.getSelectedItem();
            if (v == null) throw new IllegalArgumentException("Selecione a viatura.");

            BigDecimal odo;
            try {
                odo = new BigDecimal(odoField.getText().trim().replace(",", "."));
                if (odo.compareTo(v.currentOdometerKm()) < 0) {
                    throw new IllegalArgumentException("Odómetro não pode ser inferior ao actual (" + v.currentOdometerKm() + " Km).");
                }
            } catch (IllegalArgumentException iae) {
                odoField.requestFocus();
                throw iae;
            } catch (Exception ex) {
                odoField.requestFocus();
                throw new IllegalArgumentException("Odómetro inválido.");
            }

            BigDecimal liters;
            try {
                liters = new BigDecimal(litersField.getText().trim().replace(",", "."));
                if (liters.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                litersField.requestFocus();
                throw new IllegalArgumentException("Quantidade de litros deve ser maior que zero.");
            }

            BigDecimal price;
            try {
                price = new BigDecimal(priceField.getText().trim().replace(",", "."));
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                priceField.requestFocus();
                throw new IllegalArgumentException("Preço por litro deve ser positivo.");
            }

            RecordFuelLogRequest req = new RecordFuelLogRequest(
                    v.id(),
                    LocalDateTime.now(),
                    odo,
                    liters,
                    price,
                    stationField.getText().trim().isEmpty() ? null : stationField.getText().trim(),
                    driverField.getText().trim().isEmpty() ? null : driverField.getText().trim()
            );

            fleetApiClient.recordFuel(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Abastecimento registado e consumo actualizado.");
            loadVehicles();
        }
    }

    private void showFuelHistory() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow >= currentVehicles.size()) return;
        VehicleDTO vehicle = currentVehicles.get(modelRow);

        Window parent = SwingUtilities.getWindowAncestor(this);
        JPanel detailsPanel = new JPanel(new BorderLayout(0, 10));
        detailsPanel.setOpaque(false);

        String[] cols = {"Data/Hora", "Odómetro (Km)", "Litros", "Preço/L", "Custo Total", "Distância", "Consumo (Km/L)", "Posto"};
        DefaultTableModel histModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable histTable = new JTable(histModel);
        UIHelper.styleTable(histTable);
        histTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());

        JScrollPane histScroll = new JScrollPane(histTable);
        histScroll.setPreferredSize(new Dimension(750, 260));
        detailsPanel.add(histScroll, BorderLayout.CENTER);

        try {
            List<FuelLogDTO> logs = fleetApiClient.listFuelLogs(vehicle.id());
            for (FuelLogDTO l : logs) {
                String dtStr = l.logTimestamp() != null ? l.logTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
                String distStr = l.distanceTraveledSinceLastFillKm() != null ? l.distanceTraveledSinceLastFillKm().toPlainString() + " Km" : "—";
                String consStr = l.consumptionRateKmPerLiter() != null && l.consumptionRateKmPerLiter().compareTo(BigDecimal.ZERO) > 0
                        ? l.consumptionRateKmPerLiter().toPlainString() + " Km/L"
                        : "—";

                histModel.addRow(new Object[]{
                        dtStr,
                        l.odometerKm() + " Km",
                        l.litersFilled() + " L",
                        l.pricePerLiter() + " MT",
                        l.totalCost(),
                        distStr,
                        consStr,
                        l.gasStationName() != null ? l.gasStationName() : "—"
                });
            }
        } catch (Exception ex) {
            feedback.show(FeedbackType.ERROR, "Falha ao obter histórico de abastecimento: " + ex.getMessage());
        }

        ModernFormDialog dialog = new ModernFormDialog(parent, "Histórico de Abastecimentos: " + vehicle.licensePlate(),
                "fas-gas-pump", "Registo cronológico de combustível e rendimento do motor", detailsPanel);
        dialog.showDialog();
    }
}
