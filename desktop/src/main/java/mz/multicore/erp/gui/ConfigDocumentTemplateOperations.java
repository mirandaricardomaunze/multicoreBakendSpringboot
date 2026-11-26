package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;
import mz.multicore.erp.modules.documents.model.DocumentType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formulário, conversão para DTO e chamadas assíncronas do modelo de documento
 * (MODELOS_DOCUMENTO_SPEC §3.1, §3.4). A validação de contraste e limites é do backend.
 */
public final class ConfigDocumentTemplateOperations {

    static final int MAX_BANK_ACCOUNTS = 3;
    private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final String[] BANK_COLUMNS = {"Banco", "Titular", "NIB / IBAN"};

    private ConfigDocumentTemplateOperations() {}

    public record TemplateForm(JTextField colorField, JPanel swatch, JComboBox<DocumentLogoPosition> logoCombo,
                               JTextArea notesArea, DefaultTableModel banksModel, JCheckBox qrBox) {}

    public static TemplateForm newForm() {
        JTextField color = new JTextField(8);
        UIHelper.styleTextField(color);
        JPanel swatch = new JPanel();
        swatch.setPreferredSize(new java.awt.Dimension(28, 28));
        swatch.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        JComboBox<DocumentLogoPosition> logo = UIHelper.createComboBox(DocumentLogoPosition.values(),
                ConfigDocumentTemplateOperations::logoLabel);
        JTextArea notes = new JTextArea(3, 40);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        UIHelper.styleTextArea(notes);
        DefaultTableModel banks = new DefaultTableModel(BANK_COLUMNS, MAX_BANK_ACCOUNTS);
        JCheckBox qr = new JCheckBox("Mostrar QR code de consulta quando existir", true);
        qr.setOpaque(false);
        qr.setForeground(UIHelper.TEXT_LIGHT);
        TemplateForm form = new TemplateForm(color, swatch, logo, notes, banks, qr);
        UIHelper.onTextChange(color, () -> paintSwatch(form));
        paintSwatch(form);
        return form;
    }

    static String logoLabel(DocumentLogoPosition position) {
        return switch (position) {
            case LEFT -> "Esquerda";
            case CENTER -> "Centro";
            case RIGHT -> "Direita";
        };
    }

    /** Cor da amostra, ou {@code null} se o texto não for {@code #RRGGBB}. */
    public static Color swatchColor(String hex) {
        if (hex == null || !HEX.matcher(hex.trim()).matches()) return null;
        return Color.decode(hex.trim());
    }

    private static void paintSwatch(TemplateForm form) {
        Color color = swatchColor(form.colorField().getText());
        form.swatch().setOpaque(color != null);
        form.swatch().setBackground(color == null ? UIHelper.BG_DARK : color);
        form.swatch().repaint();
    }

    public static void apply(TemplateForm form, DocumentTemplateDTO dto) {
        form.colorField().setText(dto.accentColor() == null ? "" : dto.accentColor());
        form.logoCombo().setSelectedItem(dto.logoPosition() == null ? DocumentLogoPosition.LEFT : dto.logoPosition());
        form.notesArea().setText(dto.notes() == null ? "" : dto.notes());
        for (int row = 0; row < MAX_BANK_ACCOUNTS; row++) {
            DocumentBankAccountDTO account = row < dto.bankAccounts().size() ? dto.bankAccounts().get(row) : null;
            form.banksModel().setValueAt(account == null ? "" : account.bankName(), row, 0);
            form.banksModel().setValueAt(account == null ? "" : account.holder(), row, 1);
            form.banksModel().setValueAt(account == null ? "" : account.accountNumber(), row, 2);
        }
        form.qrBox().setSelected(dto.showQrCode());
        paintSwatch(form);
    }

    public static DocumentTemplateDTO toDto(TemplateForm form) {
        List<DocumentBankAccountDTO> accounts = new ArrayList<>();
        for (int row = 0; row < form.banksModel().getRowCount(); row++) {
            String bank = cell(form, row, 0), holder = cell(form, row, 1), number = cell(form, row, 2);
            if (bank != null || holder != null || number != null) {
                accounts.add(new DocumentBankAccountDTO(bank, holder, number));
            }
        }
        DocumentLogoPosition logo = (DocumentLogoPosition) form.logoCombo().getSelectedItem();
        return new DocumentTemplateDTO(blankToNull(form.colorField().getText()),
                logo == null ? DocumentLogoPosition.LEFT : logo,
                blankToNull(form.notesArea().getText()), accounts, form.qrBox().isSelected());
    }

    private static String cell(TemplateForm form, int row, int column) {
        Object value = form.banksModel().getValueAt(row, column);
        return value == null ? null : blankToNull(value.toString());
    }

    private static String blankToNull(String text) {
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }

    public static void load(ConfigPanel owner, DocumentType documentType, TemplateForm form) {
        if (owner.documentConfigApiClient == null || form == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.documentConfigApiClient.getTemplate(companyId, documentType),
                dto -> apply(form, dto),
                error -> owner.showConfigError("modelo do documento", error));
    }

    public static void save(ConfigPanel owner, DocumentType documentType, TemplateForm form) {
        DocumentTemplateDTO dto = toDto(form);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A guardar modelo do documento…",
                () -> owner.documentConfigApiClient.saveTemplate(companyId, documentType, dto),
                saved -> {
                    apply(form, saved);
                    ToastManager.success(owner, "Modelo de " + documentType.label() + " guardado com sucesso.");
                    owner.loadAuditLogs();
                }, error -> owner.showConfigError("modelo do documento", error));
    }

    public static void preview(ConfigPanel owner, TemplateForm form) {
        DocumentTemplateDTO dto = toDto(form);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A gerar factura de exemplo…",
                () -> owner.documentConfigApiClient.previewTemplate(companyId, dto),
                pdf -> PrintPreviewDialog.show(owner, pdf, "modelo-exemplo", "Factura de exemplo"),
                error -> owner.showConfigError("pré-visualização do modelo", error));
    }
}
