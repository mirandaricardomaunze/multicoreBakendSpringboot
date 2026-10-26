package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;

import javax.swing.*;

/**
 * Operações de predefinições, DTOs e persistência assíncrona de colunas de documentos.
 */
public final class ConfigDocumentColumnsOperations {

    private ConfigDocumentColumnsOperations() {}

    public record ColumnBoxGroup(
            JCheckBox barcode, JCheckBox reference, JCheckBox description,
            JCheckBox expiry, JCheckBox quantity, JCheckBox packages,
            JCheckBox boxes, JCheckBox boxPercentage, JCheckBox unitPrice,
            JCheckBox tax, JCheckBox subtotal, JTextField footerField
    ) {}

    public static void setCols(ColumnBoxGroup g, boolean bar, boolean ref, boolean desc, boolean exp,
                               boolean qty, boolean pk, boolean bx, boolean bxPct, boolean pr,
                               boolean tx, boolean sub) {
        if (g == null || g.barcode() == null) return;
        g.barcode().setSelected(bar); g.reference().setSelected(ref); g.description().setSelected(desc);
        g.expiry().setSelected(exp); g.quantity().setSelected(qty); g.packages().setSelected(pk);
        g.boxes().setSelected(bx); g.boxPercentage().setSelected(bxPct); g.unitPrice().setSelected(pr);
        g.tax().setSelected(tx); g.subtotal().setSelected(sub);
    }

    public static void loadColumns(ConfigPanel owner, DocumentType documentType, ColumnBoxGroup g) {
        if (owner.documentConfigApiClient == null || g == null || g.barcode() == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.documentConfigApiClient.getColumns(companyId, documentType),
                cols -> applyColumns(g, cols),
                error -> owner.showConfigError("configuração das colunas", error));
    }

    public static void applyColumns(ColumnBoxGroup g, DocumentColumnsDTO cols) {
        g.barcode().setSelected(cols.barcode());
        g.reference().setSelected(cols.reference());
        g.description().setSelected(cols.description());
        g.expiry().setSelected(cols.expiry());
        g.quantity().setSelected(cols.quantity());
        g.packages().setSelected(cols.packages());
        g.boxes().setSelected(cols.boxes());
        g.boxPercentage().setSelected(cols.boxPercentage());
        g.unitPrice().setSelected(cols.unitPrice());
        g.tax().setSelected(cols.tax());
        g.subtotal().setSelected(cols.subtotal());
        g.footerField().setText(cols.footer() == null ? "" : cols.footer());
    }

    public static void saveColumns(ConfigPanel owner, DocumentType documentType, ColumnBoxGroup g) {
        DocumentColumnsDTO dto = new DocumentColumnsDTO(
                g.barcode().isSelected(), g.reference().isSelected(), g.description().isSelected(),
                g.expiry().isSelected(), g.quantity().isSelected(), g.packages().isSelected(),
                g.boxes().isSelected(), g.boxPercentage().isSelected(), g.unitPrice().isSelected(),
                g.tax().isSelected(), g.subtotal().isSelected(),
                g.footerField().getText().trim().isEmpty() ? null : g.footerField().getText().trim()
        );
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A guardar configuração do documento…", () -> {
            owner.documentConfigApiClient.save(companyId, documentType, dto);
            return null;
        }, ignored -> {
            ToastManager.success(owner, "Configuração de " + documentType.label() + " guardada com sucesso.");
            owner.loadAuditLogs();
        }, error -> owner.showConfigError("configuração do documento", error));
    }

    public static JPanel buildPresetsRow(Runnable onCommercial, Runnable onLogistics, Runnable onAll) {
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        JLabel lbl = new JLabel("Predefinições:");
        lbl.setForeground(UIHelper.TEXT_MUTED);
        lbl.setFont(new java.awt.Font(UIHelper.FONT, java.awt.Font.BOLD, 12));
        row.add(lbl);

        mz.multicore.erp.gui.components.ModernButton btn1 = UIHelper.createSecondaryButton("Comercial Executivo");
        btn1.setIcon(UIHelper.semanticIcon("fas-file-invoice", UIHelper.ICON_XS));
        btn1.addActionListener(e -> onCommercial.run());

        mz.multicore.erp.gui.components.ModernButton btn2 = UIHelper.createSecondaryButton("Logístico / Armazém");
        btn2.setIcon(UIHelper.semanticIcon("fas-truck", UIHelper.ICON_XS));
        btn2.addActionListener(e -> onLogistics.run());

        mz.multicore.erp.gui.components.ModernButton btn3 = UIHelper.createSecondaryButton("Todas as Colunas");
        btn3.setIcon(UIHelper.semanticIcon("fas-check-double", UIHelper.ICON_XS));
        btn3.addActionListener(e -> onAll.run());

        row.add(btn1); row.add(btn2); row.add(btn3);
        return row;
    }
}
