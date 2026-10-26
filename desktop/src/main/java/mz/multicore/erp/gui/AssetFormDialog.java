package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AssetApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.model.AssetCategory;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Diálogo modal para registo de novos activos fixos e património. */
public final class AssetFormDialog {

    private AssetFormDialog() {}

    public static boolean show(Component parent, AssetApiClient assetApiClient) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JTextField codeField = new JTextField(12);
        JTextField nameField = new JTextField(25);
        JComboBox<AssetCategory> categoryCombo = new JComboBox<>(AssetCategory.values());
        UIHelper.styleComboBox(categoryCombo);
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object val, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, val, idx, sel, focus);
                if (val instanceof AssetCategory cat) setText(cat.label() + " (" + (int)(cat.defaultAnnualRate() * 100) + "%)");
                return this;
            }
        });

        DateField dateField = new DateField();
        dateField.setText(LocalDate.now().toString());

        JTextField costField = new JTextField(12);
        JTextField residualField = new JTextField("0.00", 12);
        JTextField rateField = new JTextField(8);
        JTextField serialField = new JTextField(15);
        JTextField locationField = new JTextField(15);
        JTextField employeeField = new JTextField(15);

        categoryCombo.addActionListener(e -> {
            AssetCategory selected = (AssetCategory) categoryCombo.getSelectedItem();
            if (selected != null) {
                rateField.setText(String.valueOf(BigDecimal.valueOf(selected.defaultAnnualRate() * 100).stripTrailingZeros()));
            }
        });
        if (categoryCombo.getSelectedItem() != null) {
            rateField.setText(String.valueOf(BigDecimal.valueOf(((AssetCategory) categoryCombo.getSelectedItem()).defaultAnnualRate() * 100).stripTrailingZeros()));
        }

        JPanel dialogForm = UIHelper.createDialogForm(
                "Código do Activo *:", codeField,
                "Designação / Nome *:", nameField,
                "Categoria Contabilística *:", categoryCombo,
                "Data de Aquisição *:", dateField,
                "Custo de Aquisição (MT) *:", costField,
                "Valor Residual (MT):", residualField,
                "Taxa Anual IRPC (%) *:", rateField,
                "Número de Série:", serialField,
                "Localização Física:", locationField,
                "Responsável:", employeeField
        );

        ModernFormDialog dialog = new ModernFormDialog(window, "Registar Novo Activo Fixo", "fas-building",
                "Cadastramento de imobilizado e definição das quotas de amortização", dialogForm);

        dialog.setOnSave(() -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) { codeField.requestFocus(); throw new IllegalArgumentException("O código do activo é obrigatório."); }
            String name = nameField.getText().trim();
            if (name.isEmpty()) { nameField.requestFocus(); throw new IllegalArgumentException("A designação do activo é obrigatória."); }

            LocalDate acqDate;
            try { acqDate = LocalDate.parse(dateField.getText().trim()); }
            catch (Exception ex) { dateField.requestFocus(); throw new IllegalArgumentException("A data de aquisição é inválida. Use AAAA-MM-DD."); }

            BigDecimal cost;
            try {
                cost = new BigDecimal(costField.getText().trim().replace(",", "."));
                if (cost.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) { costField.requestFocus(); throw new IllegalArgumentException("O custo de aquisição deve ser um valor numérico positivo."); }

            BigDecimal residual = BigDecimal.ZERO;
            if (!residualField.getText().trim().isEmpty()) {
                try { residual = new BigDecimal(residualField.getText().trim().replace(",", ".")); }
                catch (Exception ex) { residualField.requestFocus(); throw new IllegalArgumentException("O valor residual deve ser numérico."); }
            }

            BigDecimal rate;
            try {
                rate = new BigDecimal(rateField.getText().trim().replace(",", "."));
                if (rate.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) { rateField.requestFocus(); throw new IllegalArgumentException("A taxa de amortização deve ser maior que zero."); }

            AssetCategory cat = (AssetCategory) categoryCombo.getSelectedItem();
            CreateAssetRequest req = new CreateAssetRequest(code, name, cat, acqDate, cost, residual, rate,
                    serialField.getText().trim().isEmpty() ? null : serialField.getText().trim(),
                    locationField.getText().trim().isEmpty() ? null : locationField.getText().trim(),
                    employeeField.getText().trim().isEmpty() ? null : employeeField.getText().trim());

            assetApiClient.createAsset(req);
        });

        return dialog.showDialog();
    }
}
