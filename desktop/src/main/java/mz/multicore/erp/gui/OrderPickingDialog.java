package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.CompleteOrderPickingRequest;
import mz.multicore.erp.modules.comercial.dto.OrderPickingConfirmationLineRequest;
import mz.multicore.erp.modules.comercial.dto.OrderPickingDetailsDTO;
import mz.multicore.erp.modules.comercial.dto.OrderPickingLineDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo corporativo de conferência óptica de separação de encomendas (Picking & Packing).
 * Suporta leitor óptico de código de barras USB/Bluetooth, contagem de caixas e embalagens.
 * Ver {@code docs/PICKING_PACKING_CONFERENCIA_SPEC.md}.
 */
public class OrderPickingDialog {

    public static final int COL_STATUS = 0;
    public static final int COL_BARCODE = 1;
    public static final int COL_REF = 2;
    public static final int COL_NAME = 3;
    public static final int COL_REQUESTED = 4;
    public static final int COL_PACKAGES = 5;
    public static final int COL_BOXES = 6;
    public static final int COL_CONFIRMED = 7;
    public static final int COL_BATCH = 8;
    public static final int COL_LINE_ID = 9;

    public static void show(Window parent, OrderPickingDetailsDTO details, Runnable onPrintA4,
                            Consumer<CompleteOrderPickingRequest> onConfirm) {
        if (details == null || details.lines() == null || details.lines().isEmpty()) {
            ToastManager.show(parent, FeedbackType.WARNING, "A encomenda não possui linhas para separação.");
            return;
        }

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        // 1. Cabeçalho contextual
        JPanel infoCard = new JPanel(new GridLayout(1, 4, 12, 0));
        infoCard.setOpaque(true);
        infoCard.setBackground(UIHelper.BG_CARD);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(8, 12, 8, 12)
        ));

        infoCard.add(createMetaChip("ENCOMENDA", details.orderNumber(), UIHelper.ACCENT_BLUE));
        infoCard.add(createMetaChip("CLIENTE", details.clientName(), UIHelper.TEXT_LIGHT));
        infoCard.add(createMetaChip("ARMAZÉM", details.warehouseName(), UIHelper.TEXT_LIGHT));
        infoCard.add(createMetaChip("TOTAL CAIXAS", details.totalBoxes() + " cx", UIHelper.MODULE_STOCK));
        content.add(infoCard, BorderLayout.NORTH);

        // 2. Barra de Leitura Óptica
        JPanel scanBar = new JPanel(new BorderLayout(10, 0));
        scanBar.setOpaque(false);
        scanBar.setBorder(new EmptyBorder(4, 0, 4, 0));

        JPanel scanInputPanel = new JPanel(new BorderLayout(6, 0));
        scanInputPanel.setOpaque(false);
        JLabel scanIcon = new JLabel(UIHelper.icon("fas-barcode", 16, UIHelper.ACCENT_BLUE));
        JTextField scanField = new JTextField();
        UIHelper.styleTextField(scanField);
        scanField.putClientProperty("JTextField.placeholderText", "Bipar código de barras, referência ou SKU... [Enter]");
        scanInputPanel.add(scanIcon, BorderLayout.WEST);
        scanInputPanel.add(scanField, BorderLayout.CENTER);

        JPanel scanActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scanActionsPanel.setOpaque(false);
        JCheckBox boxModeCheck = new JCheckBox("Bipar Caixa Inteira");
        boxModeCheck.setOpaque(false);
        boxModeCheck.setForeground(UIHelper.TEXT_LIGHT);
        boxModeCheck.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        ModernButton btnCheckAll = new ModernButton("Conferir Tudo");
        btnCheckAll.setIcon(UIHelper.icon("fas-check-double", 14));
        btnCheckAll.setPreferredSize(new Dimension(135, 32));

        ModernButton btnReset = new ModernButton("Reiniciar");
        btnReset.setIcon(UIHelper.icon("fas-undo", 14));
        btnReset.setPreferredSize(new Dimension(100, 32));

        scanActionsPanel.add(boxModeCheck);
        scanActionsPanel.add(btnCheckAll);
        scanActionsPanel.add(btnReset);

        scanBar.add(scanInputPanel, BorderLayout.CENTER);
        scanBar.add(scanActionsPanel, BorderLayout.EAST);

        // 3. Tabela de Linhas de Separação
        String[] columns = {"Estado", "Cód. Barras", "Ref.", "Designação", "Qtd. Pedida", "Emb.", "Cx.", "Qtd. Conferida", "Lote", "LineID"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == COL_CONFIRMED || col == COL_BATCH;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == COL_CONFIRMED || col == COL_REQUESTED) return BigDecimal.class;
                return String.class;
            }
        };

        for (OrderPickingLineDTO line : details.lines()) {
            BigDecimal confirmed = line.confirmedQuantity() != null && line.confirmedQuantity().signum() > 0
                    ? line.confirmedQuantity() : BigDecimal.ZERO;
            String status = confirmed.compareTo(line.requestedQuantity()) >= 0 ? "Conferido" : "Pendente";
            String emb = line.packagesPerBox() != null && line.packagesPerBox() > 1 ? String.valueOf(line.packagesPerBox()) : "—";
            String cx = line.boxes() != null ? line.boxes().stripTrailingZeros().toPlainString() : "0.00";
            String batch = line.batchNumber() != null ? line.batchNumber() : "";

            model.addRow(new Object[]{
                    status,
                    line.barcode() != null ? line.barcode() : "—",
                    line.productReference() != null ? line.productReference() : "—",
                    line.productName(),
                    line.requestedQuantity(),
                    emb,
                    cx,
                    confirmed,
                    batch,
                    line.lineId()
            });
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);

        table.removeColumn(table.getColumnModel().getColumn(COL_LINE_ID));

        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(90);
        table.getColumnModel().getColumn(COL_BARCODE).setPreferredWidth(125);
        table.getColumnModel().getColumn(COL_REF).setPreferredWidth(85);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(260);
        table.getColumnModel().getColumn(COL_REQUESTED).setPreferredWidth(85);
        table.getColumnModel().getColumn(COL_PACKAGES).setPreferredWidth(55);
        table.getColumnModel().getColumn(COL_BOXES).setPreferredWidth(65);
        table.getColumnModel().getColumn(COL_CONFIRMED).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_BATCH).setPreferredWidth(110);

        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(COL_REQUESTED).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_CONFIRMED).setCellRenderer(TableCellRenderers.quantity());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COL_PACKAGES).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(COL_BOXES).setCellRenderer(rightRenderer);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(880, 280));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(scanBar, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        content.add(centerPanel, BorderLayout.CENTER);

        // 4. Rodapé e Resumo
        JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel progressLabel = new JLabel();
        progressLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));

        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);
        notesField.putClientProperty("JTextField.placeholderText", "Observações do conferidor / expedição (opcional)...");

        Runnable updateProgress = () -> {
            int total = model.getRowCount();
            int completed = 0;
            for (int i = 0; i < total; i++) {
                BigDecimal req = (BigDecimal) model.getValueAt(i, COL_REQUESTED);
                BigDecimal conf = (BigDecimal) model.getValueAt(i, COL_CONFIRMED);
                if (conf != null && conf.compareTo(req) >= 0) {
                    completed++;
                    model.setValueAt("Conferido", i, COL_STATUS);
                } else {
                    model.setValueAt("Pendente", i, COL_STATUS);
                }
            }
            progressLabel.setText("Conferidos: " + completed + " de " + total + " artigos");
            progressLabel.setForeground(completed == total ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_LIGHT);
        };
        updateProgress.run();

        // Ação do leitor de código de barras
        scanField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String scanned = scanField.getText().trim();
                    if (scanned.isBlank()) return;

                    boolean found = false;
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String barcode = String.valueOf(model.getValueAt(i, COL_BARCODE));
                        String ref = String.valueOf(model.getValueAt(i, COL_REF));
                        String name = String.valueOf(model.getValueAt(i, COL_NAME));

                        if (scanned.equalsIgnoreCase(barcode) || scanned.equalsIgnoreCase(ref)
                                || name.toLowerCase().contains(scanned.toLowerCase())) {
                            BigDecimal req = (BigDecimal) model.getValueAt(i, COL_REQUESTED);
                            BigDecimal current = (BigDecimal) model.getValueAt(i, COL_CONFIRMED);
                            if (current == null) current = BigDecimal.ZERO;

                            OrderPickingLineDTO lineDTO = details.lines().get(i);
                            BigDecimal increment = BigDecimal.ONE;
                            if (boxModeCheck.isSelected()) {
                                int pkgs = lineDTO.packagesPerBox() != null ? lineDTO.packagesPerBox() : 1;
                                int units = lineDTO.unitsPerPackage() != null ? lineDTO.unitsPerPackage() : 1;
                                long boxUnits = (long) pkgs * units;
                                if (boxUnits > 1) {
                                    increment = BigDecimal.valueOf(boxUnits);
                                }
                            }

                            BigDecimal next = current.add(increment);
                            if (next.compareTo(req) > 0) {
                                boolean ok = UIHelper.confirm(parent,
                                        "O artigo " + name + " atingiu a quantidade pedida (" + req + ").\nDeseja confirmar quantidade superior (" + next + ")?",
                                        "Aviso de Separação");
                                if (!ok) {
                                    scanField.setText("");
                                    return;
                                }
                            }

                            model.setValueAt(next, i, COL_CONFIRMED);
                            table.setRowSelectionInterval(i, i);
                            table.scrollRectToVisible(table.getCellRect(i, 0, true));
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        Toolkit.getDefaultToolkit().beep();
                        ToastManager.show(parent, FeedbackType.WARNING, "Artigo não encontrado nesta encomenda: " + scanned);
                    }

                    scanField.setText("");
                    updateProgress.run();
                }
            }
        });

        btnCheckAll.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                BigDecimal req = (BigDecimal) model.getValueAt(i, COL_REQUESTED);
                model.setValueAt(req, i, COL_CONFIRMED);
            }
            updateProgress.run();
        });

        btnReset.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(BigDecimal.ZERO, i, COL_CONFIRMED);
            }
            updateProgress.run();
        });

        table.getModel().addTableModelListener(e -> {
            if (e.getColumn() == COL_CONFIRMED) {
                updateProgress.run();
            }
        });

        JPanel leftBottom = new JPanel(new GridLayout(2, 1, 0, 4));
        leftBottom.setOpaque(false);
        leftBottom.add(progressLabel);
        leftBottom.add(notesField);
        bottomPanel.add(leftBottom, BorderLayout.CENTER);

        content.add(bottomPanel, BorderLayout.SOUTH);

        // Modal canónico
        ModernFormDialog dialog = new ModernFormDialog(parent, "Conferência Óptica de Separação", "fas-boxes", content);
        dialog.setConfirmButton("Concluir Separação", "fas-check");

        if (onPrintA4 != null) {
            ModernButton btnPrintA4 = new ModernButton("Imprimir Guia A4");
            btnPrintA4.setIcon(UIHelper.icon("fas-print", 14));
            btnPrintA4.addActionListener(e -> onPrintA4.run());
            dialog.addActionButton(btnPrintA4);
        }

        dialog.setOnSave(() -> {
            List<OrderPickingConfirmationLineRequest> confirmations = new ArrayList<>();
            boolean hasPending = false;

            for (int i = 0; i < model.getRowCount(); i++) {
                BigDecimal req = (BigDecimal) model.getValueAt(i, COL_REQUESTED);
                BigDecimal conf = (BigDecimal) model.getValueAt(i, COL_CONFIRMED);
                if (conf == null) conf = BigDecimal.ZERO;
                String batch = (String) model.getValueAt(i, COL_BATCH);
                Long lineId = details.lines().get(i).lineId();

                if (conf.compareTo(req) < 0) {
                    hasPending = true;
                }
                confirmations.add(new OrderPickingConfirmationLineRequest(lineId, conf, batch));
            }

            if (hasPending) {
                boolean ok = UIHelper.confirm(parent,
                        "Existem artigos com quantidade conferida inferior à solicitada.\nDeseja concluir a separação mesmo assim?",
                        "Confirmar Separação Parcial");
                if (!ok) return;
            }

            String terminal = System.getenv("COMPUTERNAME");
            if (terminal == null || terminal.isBlank()) terminal = "POSTO-EXPEDICAO";

            CompleteOrderPickingRequest request = new CompleteOrderPickingRequest(
                    details.orderId(),
                    terminal,
                    notesField.getText().trim(),
                    confirmations
            );

            dialog.close();
            onConfirm.accept(request);
        });

        SwingUtilities.invokeLater(scanField::requestFocusInWindow);
        dialog.showDialog();
    }

    private static JPanel createMetaChip(String label, String value, Color valueColor) {
        JPanel chip = new JPanel(new GridLayout(2, 1, 0, 2));
        chip.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        val.setForeground(valueColor);
        chip.add(lbl);
        chip.add(val);
        return chip;
    }
}
