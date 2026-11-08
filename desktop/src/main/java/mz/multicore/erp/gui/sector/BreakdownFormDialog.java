package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ButcheryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.butchery.dto.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * "Nova Desmancha": the butcher weighs the carcass, picks the model (which lists the expected cuts) and types the
 * weight of every cut. The summary under the table shows, while typing, the cuts total, the loss and whether it is
 * above the tolerance of the model, so a wrong weighing is seen before it is posted.
 */
final class BreakdownFormDialog {

    private static final String NO_TEMPLATE = "Sem modelo (cortes livres)";

    private BreakdownFormDialog() {
    }

    static void show(JComponent parent, ButcheryApiClient api, List<BreakdownTemplateDTO> templates, List<ProductDTO> products,
                     List<WarehouseDTO> warehouses, Consumer<CarcassBreakdownDTO> onPosted) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        List<ProductDTO> tracked = products.stream().filter(ProductDTO::stockTracked).toList();
        List<BreakdownTemplateDTO> active = templates.stream().filter(BreakdownTemplateDTO::active).toList();

        JComboBox<String> template = new JComboBox<>();
        template.addItem(NO_TEMPLATE);
        active.forEach(t -> template.addItem(t.name() + "  ·  " + t.sourceProductName()));
        UIHelper.styleComboBox(template);
        SearchableComboBox<ProductDTO> carcass = productCombo(tracked, "Pesquisar carcaça (ex.: boi inteiro, quarto traseiro)");
        JComboBox<String> warehouse = new JComboBox<>(warehouses.stream().map(WarehouseDTO::name).toArray(String[]::new));
        UIHelper.styleComboBox(warehouse);
        DecimalField inputKg = new DecimalField("", 3, true);
        MoneyField inputCost = new MoneyField("");
        inputCost.setToolTipText("Vazio = preço de custo da carcaça x peso");
        JTextField reference = new JTextField();
        UIHelper.styleTextField(reference);

        DefaultTableModel cuts = new DefaultTableModel(new String[]{"Corte", "Peso (kg)", "Esperado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };
        List<ProductDTO> cutProducts = new ArrayList<>();
        JTable table = new JTable(cuts);
        UIHelper.styleTable(table);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(520, 220));
        JLabel summary = new JLabel(" ");
        summary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));

        Runnable refreshSummary = () -> {
            BigDecimal input = safe(inputKg);
            BigDecimal output = BigDecimal.ZERO;
            for (int r = 0; r < cuts.getRowCount(); r++) output = output.add(parse(cuts.getValueAt(r, 1)));
            BigDecimal loss = input.subtract(output);
            BigDecimal lossPct = input.signum() == 0 ? BigDecimal.ZERO
                    : loss.multiply(BigDecimal.valueOf(100)).divide(input, 1, RoundingMode.HALF_UP);
            BigDecimal tolerance = template.getSelectedIndex() > 0 ? active.get(template.getSelectedIndex() - 1).lossTolerancePct() : null;
            boolean over = loss.signum() < 0 || (tolerance != null && lossPct.compareTo(tolerance) > 0);
            summary.setText("Cortes " + TableCellRenderers.format(output, 3, " kg") + "   ·   Perda "
                    + TableCellRenderers.format(loss, 3, " kg") + " (" + lossPct.toPlainString() + "%)"
                    + (tolerance == null ? "" : "   ·   Tolerância " + tolerance.stripTrailingZeros().toPlainString() + "%")
                    + (loss.signum() < 0 ? "   ·   os cortes pesam mais do que a carcaça" : ""));
            summary.setForeground(over ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
        };
        cuts.addTableModelListener(e -> refreshSummary.run());
        onChange(inputKg, refreshSummary);

        template.addActionListener(e -> {
            if (template.getSelectedIndex() <= 0) return;
            BreakdownTemplateDTO t = active.get(template.getSelectedIndex() - 1);
            carcass.selectFirst(p -> p.id().equals(t.sourceProductId()));
            cuts.setRowCount(0);
            cutProducts.clear();
            for (BreakdownTemplateLineDTO l : t.lines()) {
                tracked.stream().filter(p -> p.id().equals(l.productId())).findFirst().ifPresent(p -> {
                    cutProducts.add(p);
                    cuts.addRow(new Object[]{p.name(), "", l.expectedYieldPct().stripTrailingZeros().toPlainString() + "%"});
                });
            }
            refreshSummary.run();
        });

        SearchableComboBox<ProductDTO> extra = productCombo(tracked, "Acrescentar corte…");
        ModernButton add = UIHelper.createSecondaryButton("Acrescentar");
        add.setIcon(UIHelper.icon("fas-plus", 13));
        add.addActionListener(e -> {
            ProductDTO p = extra.selectedValue();
            if (p == null || cutProducts.stream().anyMatch(c -> c.id().equals(p.id()))) return;
            cutProducts.add(p);
            cuts.addRow(new Object[]{p.name(), "", "—"});
        });
        ModernButton weigh = UIHelper.createSecondaryButton("Ler balança");
        weigh.setIcon(UIHelper.icon("fas-balance-scale", 13));
        weigh.setToolTipText("Põe o peso estável da balança no corte seleccionado (ou na carcaça, se nenhum estiver seleccionado)");
        weigh.addActionListener(e -> {
            BigDecimal weight = stableWeight(parent);
            if (weight == null) return;
            int row = TableFilter.selectedModelRow(table);
            if (row < 0) inputKg.setText(weight.toPlainString());
            else cuts.setValueAt(weight.toPlainString(), row, 1);
        });
        JPanel addButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        addButtons.setOpaque(false);
        addButtons.add(add);
        addButtons.add(weigh);
        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(extra, BorderLayout.CENTER);
        addRow.add(addButtons, BorderLayout.EAST);
        JPanel cutsBox = new JPanel(new BorderLayout(0, 6));
        cutsBox.setOpaque(false);
        cutsBox.add(addRow, BorderLayout.NORTH);
        cutsBox.add(scroll, BorderLayout.CENTER);
        cutsBox.add(summary, BorderLayout.SOUTH);
        cutsBox.setBorder(new EmptyBorder(4, 0, 0, 0));

        FormField carcassForm = new FormField("Carcaça", carcass, true, null);
        FormField kgForm = new FormField("Peso da carcaça (kg)", inputKg, true, "Pese na balança antes de cortar");
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"Modelo de desmancha:", template, "", carcassForm,
                "Armazém:", warehouse, "", kgForm, "Custo total da carcaça (MT):", inputCost,
                "Referência (guia do matadouro, fornecedor):", reference, "Cortes obtidos:", cutsBox});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Desmancha", "fas-drumstick-bite",
                "Carcaça em cortes, com custo por kg", form).setConfirmButton("Registar Desmancha", "fas-check");
        String requestKey = "DM-" + UUID.randomUUID();
        CarcassBreakdownDTO[] result = new CarcassBreakdownDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            ProductDTO source = carcass.selectedValue();
            if (source == null) {
                carcassForm.setError("Escolha a carcaça.");
                throw new IllegalArgumentException("Escolha a carcaça.");
            }
            BigDecimal kg = safe(inputKg);
            if (kg.signum() <= 0) {
                kgForm.setError("Indique o peso da carcaça.");
                throw new IllegalArgumentException("Indique o peso da carcaça.");
            }
            List<BreakdownLineRequest> lines = new ArrayList<>();
            for (int r = 0; r < cuts.getRowCount(); r++) {
                BigDecimal cutKg = parse(cuts.getValueAt(r, 1));
                if (cutKg.signum() > 0) lines.add(new BreakdownLineRequest(cutProducts.get(r).id(), cutKg));
            }
            if (lines.isEmpty()) throw new IllegalArgumentException("Indique o peso de pelo menos um corte.");
            Long templateId = template.getSelectedIndex() > 0 ? active.get(template.getSelectedIndex() - 1).id() : null;
            BigDecimal cost = inputCost.getText().isBlank() ? null : inputCost.value();
            PostBreakdownRequest request = new PostBreakdownRequest(companyId, warehouses.get(warehouse.getSelectedIndex()).id(),
                    templateId, source.id(), kg, cost, reference.getText().trim(), null, lines, requestKey);
            return () -> { result[0] = api.post(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onPosted.accept(result[0]);
    }

    static SearchableComboBox<ProductDTO> productCombo(List<ProductDTO> products, String hint) {
        SearchableComboBox<ProductDTO> combo = new SearchableComboBox<>(hint,
                p -> p.name() + (p.sku() == null ? "" : "  ·  " + p.sku()), p -> p.name() + " " + p.sku());
        combo.setItems(products);
        return combo;
    }

    /** Stable weight of the workstation scale, or {@code null} with a message when it cannot be used. */
    static BigDecimal stableWeight(JComponent parent) {
        var reader = mz.multicore.erp.modules.pos.scale.ScaleConnection.connectedReader();
        var reading = reader.getCurrentReading();
        if (reader.isSimulationMode()) {
            ToastManager.show(parent, FeedbackType.WARNING, "Sem balança neste posto: configure-a em Configurações › Balança deste Posto.");
            return null;
        }
        if (reading.status() != mz.multicore.erp.modules.pos.scale.SerialScaleReader.ScaleStatus.STABLE || reading.weightKg().signum() <= 0) {
            ToastManager.show(parent, FeedbackType.WARNING, "O peso ainda não está estável. Aguarde e tente de novo.");
            return null;
        }
        return reading.weightKg();
    }

    static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        });
    }

    private static BigDecimal safe(DecimalField field) {
        try {
            BigDecimal value = field.value();
            return value == null ? BigDecimal.ZERO : value;
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }

    static BigDecimal parse(Object value) {
        if (value == null) return BigDecimal.ZERO;
        String text = value.toString().trim().replace(" ", "").replace(",", ".");
        if (text.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
