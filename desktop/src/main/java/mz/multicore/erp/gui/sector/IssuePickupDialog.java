package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PickupApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pickup.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Hands over part of the paid material. The operator types who collects it and how much of each article
 * leaves now; the server re-checks every balance. A request key generated when the dialog opens makes a
 * retry after a network failure return the same slip instead of issuing a second one.
 */
final class IssuePickupDialog {

    private static final int QTY_COLUMN = 4;

    private IssuePickupDialog() {
    }

    static void show(JComponent parent, PickupApiClient api, PickupAccountDTO account, Consumer<MaterialPickupDTO> onIssued) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        List<PickupBalanceLineDTO> lines = account.lines().stream().filter(l -> l.balance().signum() > 0).toList();
        if (lines.isEmpty()) {
            ToastManager.show(parent, FeedbackType.INFO, "Já não há material por levantar nesta fatura.");
            return;
        }
        String requestKey = UUID.randomUUID().toString();

        JTextField collector = new JTextField();
        JTextField document = new JTextField();
        JTextField plate = new JTextField();
        JTextField notes = new JTextField();
        for (JTextField f : new JTextField[]{collector, document, plate, notes}) UIHelper.styleTextField(f);
        FormField collectorForm = new FormField("Quem levanta", collector, true, "Nome do cliente ou do motorista");

        DefaultTableModel model = new DefaultTableModel(new String[]{"Código", "Artigo", "Unid.", "Por levantar", "Levantar agora"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == QTY_COLUMN; }
        };
        for (PickupBalanceLineDTO line : lines) {
            model.addRow(new Object[]{line.productSku(), line.productName(), PickupsPanel.unit(line.unitCode()),
                    ProductUnitsPanel.plain(line.balance()), ""});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        DecimalField editorField = new DecimalField("", 3, false);
        table.getColumnModel().getColumn(QTY_COLUMN).setCellEditor(new DefaultCellEditor(editorField));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(620, Math.min(260, 40 + lines.size() * 32)));

        ModernButton allBtn = UIHelper.createSecondaryButton("Levantar tudo");
        allBtn.setIcon(UIHelper.icon("fas-check-double", 13));
        allBtn.addActionListener(e -> {
            stopEditing(table);
            for (int r = 0; r < lines.size(); r++) model.setValueAt(ProductUnitsPanel.plain(lines.get(r).balance()), r, QTY_COLUMN);
        });
        ModernButton clearBtn = UIHelper.createSecondaryButton("Limpar");
        clearBtn.setIcon(UIHelper.icon("fas-eraser", 13));
        clearBtn.addActionListener(e -> {
            stopEditing(table);
            for (int r = 0; r < lines.size(); r++) model.setValueAt("", r, QTY_COLUMN);
        });

        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(ResponsiveSupport.cardTop("Quantidades a entregar", null, clearBtn, allBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        JPanel form = UIHelper.createDialogForm(1, new Object[]{
                "", collectorForm,
                "Documento (BI, carta):", document,
                "Matrícula da viatura:", plate,
                "Observações:", notes});
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(card, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Entregar Material", "fas-truck-loading",
                account.summary().clientName() + " · fatura " + account.summary().invoiceNumber(), content);
        dialog.setConfirmButton("Emitir Talão", "fas-receipt");
        MaterialPickupDTO[] result = new MaterialPickupDTO[1];
        dialog.setOnSaveAsync(() -> {
            stopEditing(table);
            if (!collectorForm.validateRequired()) throw new IllegalArgumentException("Indique quem levanta o material.");
            List<IssuePickupLineRequest> requested = new ArrayList<>();
            for (int r = 0; r < lines.size(); r++) {
                BigDecimal qty = quantityAt(model, r);
                if (qty.signum() == 0) continue;
                PickupBalanceLineDTO line = lines.get(r);
                if (qty.compareTo(line.balance()) > 0) {
                    throw new IllegalArgumentException("Só restam " + ProductUnitsPanel.plain(line.balance()) + " "
                            + PickupsPanel.unit(line.unitCode()) + " de " + line.productName() + ".");
                }
                requested.add(new IssuePickupLineRequest(line.invoiceLineId(), qty));
            }
            if (requested.isEmpty()) throw new IllegalArgumentException("Indique a quantidade a entregar em pelo menos um artigo.");
            IssuePickupRequest request = new IssuePickupRequest(companyId, account.summary().id(), collector.getText().trim(),
                    document.getText().trim(), plate.getText().trim(), notes.getText().trim(), requestKey, requested);
            return () -> { result[0] = api.issue(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onIssued.accept(result[0]);
    }

    private static BigDecimal quantityAt(DefaultTableModel model, int row) {
        Object value = model.getValueAt(row, QTY_COLUMN);
        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal qty = new BigDecimal(MoneyField.normalize(text));
            if (qty.signum() < 0) throw new IllegalArgumentException("As quantidades não podem ser negativas.");
            return qty;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Quantidade inválida na linha " + (row + 1) + ": " + text);
        }
    }

    private static void stopEditing(JTable table) {
        if (table.isEditing() && table.getCellEditor() != null) table.getCellEditor().stopCellEditing();
    }
}
