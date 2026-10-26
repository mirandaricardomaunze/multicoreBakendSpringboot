package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.InventoryItemDTO;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;
import mz.multicore.erp.modules.inventory.dto.UpdateInventoryItemCountRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utilitário de configuração e renderização da tabela de contagem física.
 */
public final class PhysicalInventoryTableHelper {

    private PhysicalInventoryTableHelper() {}

    public static void configureTableInteractions(JPanel parent,
                                                   JTable table,
                                                   DefaultTableModel tableModel,
                                                   JTextField barcodeScanField,
                                                   JButton triggerBtn,
                                                   Supplier<InventorySessionDTO> sessionSupplier,
                                                   InventoryPhysicalCountingApiClient apiClient,
                                                   Consumer<InventorySessionDTO> onSessionUpdated) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT);
                    if (column == 4 || column == 6) {
                        String valStr = value != null ? value.toString() : "0";
                        if (valStr.startsWith("+") || (!valStr.startsWith("-") && !valStr.equals("0") && !valStr.equals("0.00"))) {
                            c.setForeground(UIHelper.APPROVED_GREEN);
                        } else if (valStr.startsWith("-")) {
                            c.setForeground(UIHelper.REJECTED_RED);
                        } else {
                            c.setForeground(UIHelper.TEXT_LIGHT);
                        }
                    } else {
                        c.setForeground(UIHelper.TEXT_LIGHT);
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        UIHelper.installRowDoubleClickHandler(table, modelRow -> {
            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                String code = String.valueOf(tableModel.getValueAt(modelRow, 0));
                if (barcodeScanField != null) {
                    barcodeScanField.setText(code);
                    barcodeScanField.requestFocusInWindow();
                }
            }
        });

        UIHelper.installRowContextMenu(table, modelRow -> {
            if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return null;
            String code = String.valueOf(tableModel.getValueAt(modelRow, 0));
            JPopupMenu menu = new JPopupMenu();
            JMenuItem copyCode = new JMenuItem("Copiar Código (" + code + ")");
            copyCode.setIcon(UIHelper.icon("fas-copy", 12, UIHelper.ACCENT_BLUE));
            copyCode.addActionListener(e -> {
                StringSelection sel = new StringSelection(code);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
                ToastManager.show(parent, FeedbackType.INFO, "Código copiado: " + code);
            });
            JMenuItem selectItem = new JMenuItem("Bipar / Contar Produto");
            selectItem.setIcon(UIHelper.icon("fas-barcode", 12, UIHelper.APPROVED_GREEN));
            selectItem.addActionListener(e -> {
                if (barcodeScanField != null) {
                    barcodeScanField.setText(code);
                    barcodeScanField.requestFocusInWindow();
                }
            });
            menu.add(copyCode);
            menu.add(selectItem);
            return menu;
        });

        tableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 3) {
                int row = e.getFirstRow();
                InventorySessionDTO session = sessionSupplier.get();
                if (row >= 0 && row < table.getRowCount() && session != null) {
                    try {
                        String code = (String) tableModel.getValueAt(row, 0);
                        Object valObj = tableModel.getValueAt(row, 3);
                        BigDecimal count = new BigDecimal(valObj.toString().trim());
                        InventoryItemDTO item = session.items().stream()
                                .filter(i -> i.productCode().equals(code))
                                .findFirst().orElse(null);
                        if (item != null) {
                            UpdateInventoryItemCountRequest req = new UpdateInventoryItemCountRequest(item.id(), count, null, false, null);
                            UIHelper.submitAsync(triggerBtn, () -> apiClient.recordCount(session.id(), req), res -> {
                                if (onSessionUpdated != null) onSessionUpdated.accept(res);
                            }, null);
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}
