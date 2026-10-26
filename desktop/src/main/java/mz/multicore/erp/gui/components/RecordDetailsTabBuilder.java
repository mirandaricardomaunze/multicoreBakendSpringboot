package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Construtor das abas de itens de documento e rastreabilidade/auditoria de sessão.
 */
public final class RecordDetailsTabBuilder {

    private RecordDetailsTabBuilder() {}

    public static JComponent buildDocumentLinesTable(OrderDTO order) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cols = {"#", "Artigo / Designação", "Lote", "Quantidade", "Embalagem", "Preço Unit.", "Total"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (order.lines() != null) {
            int lineNum = 1;
            for (var line : order.lines()) {
                String lote = (line.batchNumber() != null && !line.batchNumber().isBlank() && !line.batchNumber().startsWith("LEGACY"))
                        ? line.batchNumber() : "—";
                String emb = line.unitsPerBox() > 1 ? line.unitsPerBox() + " un/cx" : "Unidade";
                model.addRow(new Object[]{
                        lineNum++,
                        line.productName(),
                        lote,
                        line.quantity() + " un.",
                        emb,
                        String.format("%.2f MT", line.unitPrice()),
                        String.format("%.2f MT", line.lineTotal())
                });
            }
        }

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public static JComponent buildAuditTable(JTable table, int row) {
        List<RecordDetailsDialog.PropertyItem> auditItems = new ArrayList<>();
        auditItems.add(new RecordDetailsDialog.PropertyItem("Operador em Sessão", CurrentUserContext.getUsername()));
        String role = CurrentUserContext.getRole();
        auditItems.add(new RecordDetailsDialog.PropertyItem("Perfil / Credencial", role != null && !role.isBlank() ? role : "Operador Autorizado"));
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        auditItems.add(new RecordDetailsDialog.PropertyItem("Empresa / Tenant", companyId != null ? "Empresa #" + companyId : "Multicore ERP Principal"));
        auditItems.add(new RecordDetailsDialog.PropertyItem("Carimbo de Data / Hora", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        auditItems.add(new RecordDetailsDialog.PropertyItem("Referência de Grelha", "Linha #" + (row + 1) + " de " + table.getRowCount() + " (" + table.getColumnCount() + " colunas)"));
        auditItems.add(new RecordDetailsDialog.PropertyItem("Modo de Inspeção", "Estritamente Só-Leitura (Garantia de Integridade)"));
        auditItems.add(new RecordDetailsDialog.PropertyItem("Motor Canónico", "RecordDetailsDialog Tier-1 Enterprise Inspector"));

        return RecordDetailsDialog.buildPropertySheet(auditItems);
    }
}
