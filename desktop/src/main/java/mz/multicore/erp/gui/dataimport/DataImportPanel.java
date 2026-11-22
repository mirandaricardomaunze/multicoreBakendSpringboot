package mz.multicore.erp.gui.dataimport;

import mz.multicore.erp.desktop.client.DataImportApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Ecrã "Importar dados" das Configurações (IMPORTACAO_DADOS_SPEC §3.4): escolher a entidade (cadastros ou
 * saldos iniciais: stock, dívidas de clientes e a fornecedores), descarregar
 * o modelo, escolher o ficheiro, ver a pré-visualização (linha, estado, coluna, mensagem) e confirmar.
 * Confirmar só fica activo com uma pré-visualização sem erros; o servidor volta a validar tudo.
 */
public class DataImportPanel extends JPanel {

    private static final String[] COLUMNS = {"Linha", "Estado", "Coluna", "Mensagem"};

    private final DataImportApiClient api;
    private final JComboBox<ImportEntityOption> entityCombo =
            UIHelper.createComboBox(ImportEntityOption.values(), ImportEntityOption::toString);
    private final ModernButton templateButton = UIHelper.createSecondaryButton("Descarregar modelo");
    private final ModernButton chooseFileButton = UIHelper.createSecondaryButton("Escolher ficheiro");
    private final ModernButton confirmButton = UIHelper.createPrimaryButton("Confirmar importação");
    private final JLabel summaryLabel = new JLabel("Escolha a entidade, preencha o modelo e escolha o ficheiro CSV.");
    private final DefaultTableModel previewModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private String fileName;
    private byte[] content;
    private ImportPreviewDTO preview;

    public DataImportPanel(DataImportApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(0, Spacing.MD));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));

        templateButton.setIcon(UIHelper.icon("fas-file-download", 14));
        chooseFileButton.setIcon(UIHelper.icon("fas-folder-open", 14));
        confirmButton.setIcon(UIHelper.icon("fas-check", 14));
        confirmButton.setEnabled(false);
        entityCombo.addActionListener(e -> clearPreview());
        templateButton.addActionListener(e -> downloadTemplate());
        chooseFileButton.addActionListener(e -> chooseFile());
        confirmButton.addActionListener(e -> confirmImport());

        add(UIHelper.createHeading("Importar dados de outro ERP"), BorderLayout.NORTH);
        add(buildCard(), BorderLayout.CENTER);
    }

    private ModernPanel buildCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        filters.setOpaque(false);
        JLabel entityLabel = new JLabel("Entidade:");
        entityLabel.setFont(Typography.BODY_BOLD);
        entityLabel.setForeground(UIHelper.TEXT_MUTED);
        filters.add(entityLabel);
        filters.add(entityCombo);

        summaryLabel.setFont(Typography.BODY);
        summaryLabel.setForeground(UIHelper.TEXT_MUTED);
        JPanel top = new JPanel(new BorderLayout(0, Spacing.SM));
        top.setOpaque(false);
        top.add(UIHelper.tableCardTop("Pré-visualização", filters, templateButton, chooseFileButton, confirmButton),
                BorderLayout.NORTH);
        top.add(summaryLabel, BorderLayout.SOUTH);

        JTable table = new JTable(previewModel);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Mostra a classificação devolvida pelo servidor e decide se Confirmar fica activo. */
    public void showPreview(String fileName, byte[] content, ImportPreviewDTO preview) {
        this.fileName = fileName;
        this.content = content.clone();
        this.preview = preview;
        previewModel.setRowCount(0);
        for (ImportRowDTO row : preview.rows()) {
            previewModel.addRow(new Object[]{row.lineNumber(), row.status().label(),
                    row.column() == null ? "" : row.column(), row.message() == null ? "" : row.message()});
        }
        summaryLabel.setText(ImportPreviewSummary.describe(fileName, preview));
        confirmButton.setEnabled(!preview.hasErrors());
    }

    private void clearPreview() {
        fileName = null;
        content = null;
        preview = null;
        previewModel.setRowCount(0);
        summaryLabel.setText("Escolha o ficheiro CSV de " + selectedEntity().lowerLabel() + ".");
        confirmButton.setEnabled(false);
    }

    private ImportEntityOption selectedEntity() {
        return (ImportEntityOption) entityCombo.getSelectedItem();
    }

    private void downloadTemplate() {
        ImportEntityOption entity = selectedEntity();
        JFileChooser chooser = csvChooser();
        chooser.setSelectedFile(new File(entity.templateFileName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File target = chooser.getSelectedFile();
        UIHelper.submitAsync(templateButton, () -> {
            Files.write(target.toPath(), api.template(entity.path()));
            return target;
        }, saved -> ToastManager.show(this, FeedbackType.SUCCESS, "Modelo guardado em " + saved.getAbsolutePath() + "."),
                error -> showError("descarregar o modelo", error));
    }

    private void chooseFile() {
        JFileChooser chooser = csvChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException ex) {
            showError("ler o ficheiro", ex);
            return;
        }
        ImportEntityOption entity = selectedEntity();
        UIHelper.submitAsync(chooseFileButton, () -> api.preview(entity.path(), bytes), result -> {
            if (entity == selectedEntity()) showPreview(file.getName(), bytes, result);
        }, error -> showError("pré-visualizar o ficheiro", error));
    }

    private void confirmImport() {
        if (preview == null || preview.hasErrors()) return;
        ImportEntityOption entity = selectedEntity();
        if (!ModernMessageDialog.confirm(this, FeedbackType.WARNING, "Confirmar importação",
                "Gravar " + entity.objectPhrase() + " do ficheiro \"" + fileName + "\" na empresa activa?", "Importar")) {
            return;
        }
        String name = fileName;
        byte[] bytes = content;
        UIHelper.submitAsync(confirmButton, () -> api.commit(entity.path(), name, bytes), result -> {
            clearPreview();
            ToastManager.show(this, FeedbackType.SUCCESS, ImportPreviewSummary.committed(entity, result));
        }, error -> showError("confirmar a importação", error));
    }

    private void showError(String action, Throwable error) {
        String detail = error == null || error.getMessage() == null ? "erro desconhecido." : error.getMessage();
        ToastManager.show(this, FeedbackType.ERROR, "Não foi possível " + action + ": " + detail);
    }

    private static JFileChooser csvChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros CSV (*.csv)", "csv"));
        return chooser;
    }

    // ── Acesso para os harness de UI (DataImportPanelHarnessTest) ──────────────────────────────

    public JComboBox<ImportEntityOption> entityCombo() { return entityCombo; }

    public AbstractButton templateButton() { return templateButton; }

    public AbstractButton chooseFileButton() { return chooseFileButton; }

    public AbstractButton confirmButton() { return confirmButton; }

    public DefaultTableModel previewTableModel() { return previewModel; }

    public JLabel summaryLabel() { return summaryLabel; }
}
