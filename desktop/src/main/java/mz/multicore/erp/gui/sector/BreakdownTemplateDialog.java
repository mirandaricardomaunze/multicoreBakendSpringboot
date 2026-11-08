package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ButcheryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.butchery.dto.BreakdownTemplateDTO;
import mz.multicore.erp.modules.butchery.dto.BreakdownTemplateLineDTO;
import mz.multicore.erp.modules.butchery.dto.SaveBreakdownTemplateRequest;
import mz.multicore.erp.modules.butchery.dto.TemplateLineRequest;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Breakdown model: the carcass, the cuts it should give with their expected yield, and the loss accepted. */
final class BreakdownTemplateDialog {

    private BreakdownTemplateDialog() {
    }

    static void show(JComponent parent, ButcheryApiClient api, List<ProductDTO> products, BreakdownTemplateDTO existing,
                     Consumer<BreakdownTemplateDTO> onSaved) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        List<ProductDTO> tracked = products.stream().filter(ProductDTO::stockTracked).toList();
        JTextField name = new JTextField(existing == null ? "" : existing.name());
        UIHelper.styleTextField(name);
        SearchableComboBox<ProductDTO> carcass = BreakdownFormDialog.productCombo(tracked, "Pesquisar a carcaça");
        if (existing != null) carcass.selectFirst(p -> p.id().equals(existing.sourceProductId()));
        DecimalField tolerance = new DecimalField(existing == null ? "8" : existing.lossTolerancePct().stripTrailingZeros().toPlainString(), 2, false);
        JCheckBox active = new JCheckBox("Modelo activo", existing == null || existing.active());
        active.setOpaque(false);
        active.setForeground(UIHelper.TEXT_LIGHT);

        DefaultTableModel lines = new DefaultTableModel(new String[]{"Corte", "Rendimento esperado (%)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };
        List<ProductDTO> cutProducts = new ArrayList<>();
        if (existing != null) {
            for (BreakdownTemplateLineDTO l : existing.lines()) {
                tracked.stream().filter(p -> p.id().equals(l.productId())).findFirst().ifPresent(p -> {
                    cutProducts.add(p);
                    lines.addRow(new Object[]{p.name(), l.expectedYieldPct().stripTrailingZeros().toPlainString()});
                });
            }
        }
        JTable table = new JTable(lines);
        UIHelper.styleTable(table);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(480, 200));
        JLabel total = new JLabel(" ");
        total.setForeground(UIHelper.TEXT_MUTED);
        Runnable refresh = () -> {
            BigDecimal sum = BigDecimal.ZERO;
            for (int r = 0; r < lines.getRowCount(); r++) sum = sum.add(BreakdownFormDialog.parse(lines.getValueAt(r, 1)));
            total.setText("Rendimento esperado total: " + sum.stripTrailingZeros().toPlainString() + "%  ·  o resto é perda (osso não vendido, sangue, gordura).");
            total.setForeground(sum.compareTo(BigDecimal.valueOf(100)) > 0 ? UIHelper.REJECTED_RED : UIHelper.TEXT_MUTED);
        };
        lines.addTableModelListener(e -> refresh.run());
        refresh.run();

        SearchableComboBox<ProductDTO> extra = BreakdownFormDialog.productCombo(tracked, "Acrescentar corte ao modelo…");
        ModernButton add = UIHelper.createSecondaryButton("Acrescentar");
        add.setIcon(UIHelper.icon("fas-plus", 13));
        add.addActionListener(e -> {
            ProductDTO p = extra.selectedValue();
            if (p == null || cutProducts.stream().anyMatch(c -> c.id().equals(p.id()))) return;
            cutProducts.add(p);
            lines.addRow(new Object[]{p.name(), "0"});
        });
        ModernButton remove = UIHelper.createSecondaryButton("Retirar");
        remove.setIcon(UIHelper.icon("fas-trash-alt", 13));
        remove.addActionListener(e -> {
            int model = TableFilter.selectedModelRow(table);
            if (model < 0) return;
            cutProducts.remove(model);
            lines.removeRow(model);
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(remove);
        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(extra, BorderLayout.CENTER);
        addRow.add(buttons, BorderLayout.EAST);
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);
        box.add(addRow, BorderLayout.NORTH);
        box.add(scroll, BorderLayout.CENTER);
        box.add(total, BorderLayout.SOUTH);

        FormField nameForm = new FormField("Nome do modelo", name, true, "Ex.: Boi inteiro, Quarto traseiro, Porco");
        FormField carcassForm = new FormField("Carcaça", carcass, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", nameForm, "", carcassForm,
                "Perda aceite (%):", tolerance, "Cortes e rendimento esperado:", box, "Estado:", active});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, existing == null ? "Novo Modelo de Desmancha" : "Editar Modelo",
                "fas-sitemap", "Cortes esperados de cada carcaça", form);
        BreakdownTemplateDTO[] result = new BreakdownTemplateDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            if (!nameForm.validateRequired()) throw new IllegalArgumentException("Indique o nome do modelo.");
            ProductDTO source = carcass.selectedValue();
            if (source == null) {
                carcassForm.setError("Escolha a carcaça.");
                throw new IllegalArgumentException("Escolha a carcaça.");
            }
            if (cutProducts.isEmpty()) throw new IllegalArgumentException("Acrescente pelo menos um corte.");
            List<TemplateLineRequest> request = new ArrayList<>();
            for (int r = 0; r < lines.getRowCount(); r++) {
                request.add(new TemplateLineRequest(cutProducts.get(r).id(), BreakdownFormDialog.parse(lines.getValueAt(r, 1))));
            }
            BigDecimal tol = tolerance.value() == null ? BigDecimal.ZERO : tolerance.value();
            SaveBreakdownTemplateRequest save = new SaveBreakdownTemplateRequest(existing == null ? null : existing.id(), companyId,
                    name.getText().trim(), source.id(), tol, active.isSelected(), request);
            return () -> { result[0] = api.saveTemplate(save); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onSaved.accept(result[0]);
    }
}
