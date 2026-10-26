package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fleet.dto.FuelLogDTO;
import mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest;
import mz.multicore.erp.modules.fleet.dto.VehicleDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Coordenador das operações de abastecimento e consulta de histórico de combustível.
 */
public final class FuelOperationsCoordinator {

    private final FleetApiClient fleetApiClient;

    public FuelOperationsCoordinator(FleetApiClient fleetApiClient) {
        this.fleetApiClient = fleetApiClient;
    }

    public void openFuelDialog(Window parent, List<VehicleDTO> currentVehicles, InlineFeedbackPanel feedback, Runnable onSuccess) {
        if (currentVehicles.isEmpty()) {
            feedback.show(FeedbackType.WARNING, "Registe primeiro pelo menos uma viatura.");
            return;
        }

        JComboBox<VehicleDTO> vehicleCombo = new JComboBox<>(currentVehicles.toArray(new VehicleDTO[0]));
        UIHelper.styleComboBox(vehicleCombo);
        DefaultListCellRenderer baseRenderer = new DefaultListCellRenderer();
        vehicleCombo.setRenderer((list, val, idx, isSelected, cellHasFocus) -> {
            Component c = baseRenderer.getListCellRendererComponent(list, val, idx, isSelected, cellHasFocus);
            if (val instanceof VehicleDTO v && c instanceof JLabel lbl) {
                lbl.setText(v.licensePlate() + " - " + v.make() + " " + v.model() + " (Odóm: " + v.currentOdometerKm() + " Km)");
            }
            return c;
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
                    v.id(), LocalDateTime.now(), odo, liters, price,
                    stationField.getText().trim().isEmpty() ? null : stationField.getText().trim(),
                    driverField.getText().trim().isEmpty() ? null : driverField.getText().trim()
            );

            fleetApiClient.recordFuel(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Abastecimento registado e consumo actualizado.");
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void showFuelHistoryDialog(Window parent, VehicleDTO vehicle, InlineFeedbackPanel feedback) {
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
