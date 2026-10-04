package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ShelfLabelsDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderLineDTO;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo canónico de conferência física e recepção de encomendas a fornecedor.
 * Suporta segregação entre quantidade em bom estado, mercadoria danificada e faltas definitivas.
 */
public class PurchaseOrderReceivingDialog {

    public static final int COL_PRODUCT = 0;
    public static final int COL_ORDERED = 1;
    public static final int COL_RECEIVED = 2;
    public static final int COL_OUTSTANDING = 3;
    public static final int COL_TO_RECEIVE = 4;
    public static final int COL_DAMAGED = 5;
    public static final int COL_MISSING = 6;
    public static final int COL_NOTES = 7;

    public static void show(Window parent, PurchaseOrderDTO order, Consumer<ReceivePurchaseOrderRequest> onConfirm) {
        if (order == null || order.lines() == null || order.lines().isEmpty()) return;

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        // Card superior com contexto da encomenda
        JPanel infoCard = new JPanel(new GridLayout(1, 3, 16, 0));
        infoCard.setOpaque(true);
        infoCard.setBackground(UIHelper.BG_CARD);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(10, 14, 10, 14)
        ));

        infoCard.add(createHeaderField("Nº Encomenda:", order.orderNumber() != null ? order.orderNumber() : "#" + order.id()));
        infoCard.add(createHeaderField("Fornecedor:", order.supplierName() != null ? order.supplierName() : "—"));
        infoCard.add(createHeaderField("Entrega Prevista:", order.expectedDate() != null ? order.expectedDate().toString() : "—"));
        content.add(infoCard, BorderLayout.NORTH);

        // Grelha de conferência
        String[] cols = {
                "Produto", "Encomendado", "Já Recebido", "Pendente",
                "A Receber (Boas)", "Danificado", "Falta Definitiva", "Notas / Divergência"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c >= COL_TO_RECEIVE;
            }
        };

        List<PurchaseOrderLineDTO> lines = order.lines();
        for (PurchaseOrderLineDTO l : lines) {
            BigDecimal outstanding = l.outstandingQuantity();
            model.addRow(new Object[]{
                    l.productName(),
                    l.quantity(),
                    l.receivedQuantity(),
                    outstanding,
                    outstanding, // valor inicial sugerido: restante em boas condições
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    ""
            });
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);

        // Renderers com 2 casas decimais
        table.getColumnModel().getColumn(COL_ORDERED).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_RECEIVED).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_OUTSTANDING).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_TO_RECEIVE).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_DAMAGED).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(COL_MISSING).setCellRenderer(TableCellRenderers.quantity());

        table.getColumnModel().getColumn(COL_PRODUCT).setPreferredWidth(200);
        table.getColumnModel().getColumn(COL_TO_RECEIVE).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_DAMAGED).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_MISSING).setPreferredWidth(110);
        table.getColumnModel().getColumn(COL_NOTES).setPreferredWidth(160);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(860, 260));
        content.add(scroll, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(
                parent,
                "Conferência de Recepção de Mercadorias",
                "fas-truck-loading",
                "Indique as quantidades físicas conferidas na descarga para dar entrada no stock.",
                content
        );

        ModernButton printLabelsBtn = UIHelper.createSecondaryButton("Etiquetas");
        printLabelsBtn.setIcon(UIHelper.icon("fas-barcode", 14, UIHelper.ACCENT_CYAN));
        printLabelsBtn.setToolTipText("Imprimir etiquetas para os artigos com quantidade a receber nesta conferência");
        printLabelsBtn.addActionListener(e -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            List<ProductDTO> labelProducts = extractLabelProducts(lines, model);
            if (labelProducts.isEmpty()) {
                ToastManager.show(dlg.getDialog(), FeedbackType.WARNING, "Indique pelo menos uma quantidade a receber para imprimir etiquetas.");
                return;
            }
            ShelfLabelsDialog.show(dlg.getDialog(), labelProducts);
        });
        dlg.addActionButton(printLabelsBtn);

        dlg.setOnSave(() -> {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();

            List<ReceivePurchaseOrderRequest.ReceiveLine> toReceive = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                PurchaseOrderLineDTO line = lines.get(i);
                BigDecimal goodQty = parseDecimal(model.getValueAt(i, COL_TO_RECEIVE));
                BigDecimal damagedQty = parseDecimal(model.getValueAt(i, COL_DAMAGED));
                BigDecimal missingQty = parseDecimal(model.getValueAt(i, COL_MISSING));
                String notes = String.valueOf(model.getValueAt(i, COL_NOTES)).trim();

                BigDecimal totalAccounted = goodQty.add(damagedQty).add(missingQty);
                if (totalAccounted.compareTo(line.outstandingQuantity()) > 0) {
                    throw new IllegalArgumentException(
                            "Linha '" + line.productName() + "': a soma conferida (" + totalAccounted
                                    + ") não pode exceder o saldo pendente (" + line.outstandingQuantity() + ")."
                    );
                }

                if (goodQty.signum() > 0 || damagedQty.signum() > 0 || missingQty.signum() > 0) {
                    toReceive.add(new ReceivePurchaseOrderRequest.ReceiveLine(
                            line.id(), goodQty, damagedQty, missingQty, notes.isEmpty() ? null : notes
                    ));
                }
            }

            if (toReceive.isEmpty()) {
                throw new IllegalArgumentException("Indique pelo menos uma quantidade a receber, danificada ou em falta.");
            }

            onConfirm.accept(new ReceivePurchaseOrderRequest(toReceive));
        });

        dlg.showDialog();
    }

    public static List<ProductDTO> extractLabelProducts(List<PurchaseOrderLineDTO> lines, DefaultTableModel model) {
        return extractLabelProducts(lines, model, false);
    }

    public static List<ProductDTO> extractLabelProducts(List<PurchaseOrderLineDTO> lines, DefaultTableModel model, boolean repeatByQuantity) {
        List<ProductDTO> result = new ArrayList<>();
        if (lines == null || model == null) return result;

        for (int i = 0; i < Math.min(lines.size(), model.getRowCount()); i++) {
            PurchaseOrderLineDTO line = lines.get(i);
            BigDecimal goodQty = parseDecimal(model.getValueAt(i, COL_TO_RECEIVE));
            if (goodQty.signum() > 0) {
                String sku = line.productSku() != null && !line.productSku().isBlank()
                        ? line.productSku() : "ART-" + line.productId();
                String barcode = (line.productSku() != null && !line.productSku().isBlank())
                        ? line.productSku()
                        : (line.serialNumber() != null ? line.serialNumber() : sku);
                BigDecimal price = line.unitPrice() != null ? line.unitPrice() : BigDecimal.ZERO;
                ProductDTO p = new ProductDTO(
                        line.productId(),
                        sku,
                        sku,
                        barcode,
                        line.productName(),
                        price,
                        price,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        1,
                        "UNIT",
                        true,
                        null,
                        "Geral",
                        null,
                        line.taxRate() != null ? line.taxRate() : BigDecimal.ZERO,
                        "IVA",
                        "Entrada por Encomenda",
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
                int count = repeatByQuantity ? Math.min(100, Math.max(1, goodQty.intValue())) : 1;
                for (int c = 0; c < count; c++) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private static JPanel createHeaderField(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        val.setForeground(UIHelper.TEXT_LIGHT);
        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private static BigDecimal parseDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd.max(BigDecimal.ZERO);
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).max(BigDecimal.ZERO);
        String s = val.toString().trim().replace(" ", "").replace(',', '.');
        if (s.isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal d = new BigDecimal(s);
            return d.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : d;
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
