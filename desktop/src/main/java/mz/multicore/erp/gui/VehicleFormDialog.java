package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest;
import mz.multicore.erp.modules.fleet.model.FuelType;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Diálogo modal para registo de novas viaturas na frota.
 */
public final class VehicleFormDialog {

    private VehicleFormDialog() {}

    public static void show(Window parent, FleetApiClient fleetApiClient, InlineFeedbackPanel feedback, Runnable onSuccess) {
        JTextField plateField = new JTextField(10);
        JTextField makeField = new JTextField(15);
        JTextField modelField = new JTextField(15);
        JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
        JComboBox<FuelType> fuelCombo = new JComboBox<>(FuelType.values());
        UIHelper.styleComboBox(fuelCombo);
        DefaultListCellRenderer baseRenderer = new DefaultListCellRenderer();
        fuelCombo.setRenderer((list, val, idx, isSelected, cellHasFocus) -> {
            Component c = baseRenderer.getListCellRendererComponent(list, val, idx, isSelected, cellHasFocus);
            if (val instanceof FuelType f && c instanceof JLabel lbl) {
                lbl.setText(f.label());
            }
            return c;
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
                    plate, make, model, year, (FuelType) fuelCombo.getSelectedItem(),
                    odo, driverField.getText().trim().isEmpty() ? null : driverField.getText().trim(),
                    insDate, inspDate
            );
            fleetApiClient.createVehicle(req);
        });

        if (dialog.showDialog()) {
            feedback.show(FeedbackType.SUCCESS, "Viatura registada com sucesso.");
            if (onSuccess != null) onSuccess.run();
        }
    }
}
