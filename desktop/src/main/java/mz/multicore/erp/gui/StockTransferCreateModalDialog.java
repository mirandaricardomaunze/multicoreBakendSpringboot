package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferLineRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo modal para criação rápida de Guia de Transferência de Stock com cálculo de carga e FEFO.
 */
final class StockTransferCreateModalDialog {

    private StockTransferCreateModalDialog() {}

    private record FefoPreview(String batch, String expiration) {}

    public static void show(StockPanel owner) {
        if (owner.warehousesList.size() < 2) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns insuficientes",
                    "Registe pelo menos dois armazéns para realizar uma transferência.");
            return;
        }
        List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário",
                    "Registe produtos antes de realizar uma transferência.");
            return;
        }

        JComboBox<String> originCombo = new JComboBox<>();
        JComboBox<String> destinationCombo = new JComboBox<>();
        UIHelper.styleComboBox(originCombo);
        UIHelper.styleComboBox(destinationCombo);
        for (WarehouseDTO w : owner.warehousesList) {
            originCombo.addItem(w.name());
            destinationCombo.addItem(w.name());
        }
        if (owner.warehousesList.size() > 1) destinationCombo.setSelectedIndex(1);

        JTextField driverField = new JTextField();
        JTextField vehiclePlateField = new JTextField();
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(driverField);
        UIHelper.styleTextField(vehiclePlateField);
        UIHelper.styleTextField(responsibleField);
        UIHelper.styleTextField(vehicleField);
        UIHelper.styleTextField(notesField);
        driverField.putClientProperty("JTextField.placeholderText", "Nome do motorista (obrigatório)");
        vehiclePlateField.putClientProperty("JTextField.placeholderText", "Matrícula do veículo (obrigatório, Ex: ABC-123-MC)");
        responsibleField.putClientProperty("JTextField.placeholderText", "Responsável pela transferência (opcional)");
        vehicleField.putClientProperty("JTextField.placeholderText", "Viatura / Transporte (opcional)");
        notesField.putClientProperty("JTextField.placeholderText", "Observações sobre a transferência");

        String[] lineCols = {"Produto", "Quantidade", "Embalagem", "Caixa", "% Caixa", "Lote (FEFO)", "Validade (FEFO)"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 0 || c == 1; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);

        ProductSearchComboBox productEditorCombo = new ProductSearchComboBox();
        productEditorCombo.setProducts(products);
        linesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(productEditorCombo));
        linesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Object text = value instanceof ProductDTO product
                        ? ProductSearchComboBox.displayLabel(product)
                        : value;
                return super.getTableCellRendererComponent(table, text, selected, focus, row, column);
            }
        });

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(740, 200));

        JLabel loadInfoLabel = new JLabel("Carga: a calcular…");
        loadInfoLabel.setIcon(UIHelper.icon("fas-truck", 16, UIHelper.ACCENT_BLUE));
        loadInfoLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        loadInfoLabel.setForeground(UIHelper.TEXT_LIGHT);
        ModernPanel loadInfoPanel = new ModernPanel(8);
        loadInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        loadInfoPanel.add(loadInfoLabel);

        Runnable refreshTransferFEFO = () -> {
            int wIdx = originCombo.getSelectedIndex();
            WarehouseDTO origin = (wIdx >= 0 && wIdx < owner.warehousesList.size()) ? owner.warehousesList.get(wIdx) : null;
            List<ProductDTO> selectedProducts = new ArrayList<>();
            BigDecimal totalLoadWeight = BigDecimal.ZERO;
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                Object productValue = linesModel.getValueAt(i, 0);
                ProductDTO product = productValue instanceof ProductDTO selected ? selected : null;
                selectedProducts.add(product);

                String qtyStr = String.valueOf(linesModel.getValueAt(i, 1)).trim();
                BigDecimal qty = BigDecimal.ZERO;
                try {
                    qty = new BigDecimal(qtyStr);
                } catch (Exception ignored) {}

                if (product != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                    int pkgsPerBox = product.packagesPerBox() > 0 ? product.packagesPerBox() : 1;
                    int unitsPerPkg = product.unitsPerPackage() > 0 ? product.unitsPerPackage() : 1;
                    long unitsPerBox = (long) pkgsPerBox * unitsPerPkg;

                    BigDecimal pkgs = qty.divide(BigDecimal.valueOf(unitsPerPkg), 2, RoundingMode.HALF_UP);
                    BigDecimal boxes = qty.divide(BigDecimal.valueOf(unitsPerBox), 2, RoundingMode.HALF_UP);
                    BigDecimal pct = qty.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(unitsPerBox), 1, RoundingMode.HALF_UP);

                    linesModel.setValueAt(pkgs.stripTrailingZeros().toPlainString(), i, 2);
                    linesModel.setValueAt(boxes.stripTrailingZeros().toPlainString(), i, 3);
                    linesModel.setValueAt(pct.stripTrailingZeros().toPlainString() + "%", i, 4);

                    BigDecimal w = product.grossUnitWeightKg() != null ? product.grossUnitWeightKg() : product.netUnitWeightKg();
                    if (w != null) totalLoadWeight = totalLoadWeight.add(w.multiply(qty));
                } else {
                    linesModel.setValueAt("—", i, 2);
                    linesModel.setValueAt("—", i, 3);
                    linesModel.setValueAt("—", i, 4);
                }

                linesModel.setValueAt("A carregar…", i, 5);
                linesModel.setValueAt("", i, 6);
            }

            if (totalLoadWeight.compareTo(BigDecimal.ZERO) > 0) {
                String vehicleType = mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator.recommendedVehicleType(totalLoadWeight);
                BigDecimal tons = totalLoadWeight.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
                loadInfoLabel.setText("Carga Estimada: " + totalLoadWeight.setScale(2, RoundingMode.HALF_UP)
                        + " kg (" + tons.stripTrailingZeros().toPlainString() + " Ton)  |  Viatura Recomendada: " + vehicleType);
                vehicleField.putClientProperty("JTextField.placeholderText", "Sugerido: " + vehicleType);
            } else {
                loadInfoLabel.setText("Carga Total: Aferir peso na balança para alocação de viatura");
            }

            if (origin == null) return;
            Long originId = origin.id();
            UIHelper.loadAsync(owner, () -> {
                List<FefoPreview> previews = new ArrayList<>();
                for (ProductDTO product : selectedProducts) {
                    if (product == null) {
                        previews.add(new FefoPreview("", ""));
                        continue;
                    }
                    var opt = owner.inventoryApiClient.findNextFEFO(product.id(), originId);
                    if (opt.isPresent()) {
                        var b = opt.get();
                        previews.add(new FefoPreview(b.batchNumber() == null ? "—" : b.batchNumber(),
                                b.expirationDate() == null ? "—" : b.expirationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                    } else {
                        previews.add(new FefoPreview("Sem stock", "—"));
                    }
                }
                return previews;
            }, previews -> {
                int currentIdx = originCombo.getSelectedIndex();
                if (currentIdx < 0 || !owner.warehousesList.get(currentIdx).id().equals(originId)) return;
                for (int i = 0; i < previews.size() && i < linesModel.getRowCount(); i++) {
                    linesModel.setValueAt(previews.get(i).batch(), i, 5);
                    linesModel.setValueAt(previews.get(i).expiration(), i, 6);
                }
            }, error -> {
                for (int i = 0; i < linesModel.getRowCount(); i++) {
                    linesModel.setValueAt("Falha ao carregar", i, 5);
                    linesModel.setValueAt("", i, 6);
                }
            });
        };

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        ModernButton removeLineBtn = UIHelper.createDangerButton("- Remover");
        addLineBtn.addActionListener(ev -> {
            linesModel.addRow(new Object[]{products.get(0), "1", "—", "—", "—", "", ""});
            refreshTransferFEFO.run();
        });
        removeLineBtn.addActionListener(ev -> {
            int sel = TableFilter.selectedModelRow(linesTable);
            if (sel >= 0) linesModel.removeRow(sel);
        });
        JPanel lineButtons = UIHelper.actionsBar(removeLineBtn, addLineBtn);

        linesModel.addRow(new Object[]{products.get(0), "1", "—", "—", "—", "", ""});
        originCombo.addActionListener(ev -> refreshTransferFEFO.run());
        linesModel.addTableModelListener(ev -> {
            if (ev.getColumn() == 0 || ev.getColumn() == 1) refreshTransferFEFO.run();
        });
        refreshTransferFEFO.run();

        JPanel header = UIHelper.createDialogForm(
                "Armazém de Origem:", originCombo,
                "Armazém de Destino:", destinationCombo,
                "Motorista *:", driverField,
                "Matrícula do Veículo *:", vehiclePlateField,
                "Responsável:", responsibleField,
                "Viatura / Transporte:", vehicleField,
                "Observações:", notesField
        );

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.setOpaque(false);
        dialogPanel.add(header, BorderLayout.NORTH);
        JPanel linesWrap = new JPanel(new BorderLayout(0, 6));
        linesWrap.setOpaque(false);
        JPanel linesHeader = new JPanel(new BorderLayout(8, 0));
        linesHeader.setOpaque(false);
        linesHeader.add(new JLabel("Linhas da Transferência:"), BorderLayout.WEST);
        linesHeader.add(lineButtons, BorderLayout.EAST);
        linesWrap.add(linesHeader, BorderLayout.NORTH);
        linesWrap.add(linesScroll, BorderLayout.CENTER);
        linesWrap.add(loadInfoPanel, BorderLayout.SOUTH);
        dialogPanel.add(linesWrap, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Nova Transferência de Stock", "fas-exchange-alt", "Mova stock entre armazéns", dialogPanel);
        dlg.setOnSave(() -> {
            int originI = originCombo.getSelectedIndex();
            int destI = destinationCombo.getSelectedIndex();
            if (originI == destI) {
                throw new IllegalArgumentException("O armazém de origem e o de destino devem ser diferentes.");
            }
            if (driverField.getText().trim().isEmpty()) {
                driverField.requestFocusInWindow();
                throw new IllegalArgumentException("O nome do motorista é obrigatório para emitir a guia de transferência.");
            }
            if (vehiclePlateField.getText().trim().isEmpty()) {
                vehiclePlateField.requestFocusInWindow();
                throw new IllegalArgumentException("A matrícula do veículo é obrigatória para emitir a guia de transferência.");
            }
            if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
            if (linesModel.getRowCount() == 0) {
                throw new IllegalArgumentException("Adicione pelo menos um produto à transferência.");
            }
        });
        boolean confirmed = dlg.showDialog();
        if (!confirmed) return;

        int originIdx = originCombo.getSelectedIndex();
        int destIdx = destinationCombo.getSelectedIndex();
        if (originIdx == destIdx) {
            owner.showStockNotice(FeedbackType.ERROR, "Armazéns inválidos",
                    "O armazém de origem e o de destino devem ser diferentes.");
            return;
        }
        if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
        if (linesModel.getRowCount() == 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Transferência vazia",
                    "Adicione pelo menos um produto à transferência.");
            return;
        }

        List<CreateStockTransferLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                Object productValue = linesModel.getValueAt(i, 0);
                String qtyStr = String.valueOf(linesModel.getValueAt(i, 1)).trim();
                BigDecimal qty = new BigDecimal(qtyStr);
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException("Quantidade deve ser positiva na linha " + (i + 1));
                }
                if (!(productValue instanceof ProductDTO product)) {
                    throw new IllegalArgumentException("Pesquise e seleccione o produto da linha " + (i + 1) + ".");
                }
                lines.add(new CreateStockTransferLineRequest(product.id(), qty));
            }
        } catch (NumberFormatException ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", ex.getMessage());
            return;
        } catch (Exception ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Não foi possível preparar a transferência", ex.getMessage());
            return;
        }

        WarehouseDTO origin = owner.warehousesList.get(originIdx);
        WarehouseDTO destination = owner.warehousesList.get(destIdx);

        String driver = driverField.getText().trim();
        String plate = vehiclePlateField.getText().trim();
        if (driver.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Motorista obrigatório",
                    "O nome do motorista é obrigatório para emitir a guia de transferência.");
            return;
        }
        if (plate.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Matrícula obrigatória",
                    "A matrícula do veículo é obrigatória para emitir a guia de transferência.");
            return;
        }
        String resp = responsibleField.getText().trim();
        String vehicle = vehicleField.getText().trim();
        if (vehicle.isEmpty() && !plate.isEmpty()) {
            vehicle = plate;
        }
        String notes = notesField.getText().trim();

        CreateStockTransferRequest request = new CreateStockTransferRequest(
                CurrentUserContext.getCurrentCompanyId(),
                origin.id(),
                destination.id(),
                resp.isEmpty() ? null : resp,
                vehicle.isEmpty() ? null : vehicle,
                notes.isEmpty() ? null : notes,
                lines,
                driver.isEmpty() ? null : driver,
                plate.isEmpty() ? null : plate);
        UIHelper.runWithProgress(owner, "A criar transferência…", () -> owner.stockTransferApiClient.create(request), created -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Guia " + created.transferNumber() + " criada em rascunho.\nSubmeta-a para encaminhar a separação de artigos no armazém.");
        }, owner::showStockError);
    }
}
