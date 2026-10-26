package mz.multicore.erp.gui.intelligence;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.intelligence.dto.AiDocumentVersionDTO;
import mz.multicore.erp.modules.intelligence.dto.AiManagedDocumentDTO;
import mz.multicore.erp.modules.intelligence.dto.AiProfessionalDocumentRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Vista especializada de gestão de arquivo, versionamento e aprovações de documentos de inteligência.
 */
public class AiDocumentHistoryView extends JPanel {

    private final AiAssistantApiClient apiClient;
    private final Supplier<AiProfessionalDocumentRequest> documentRequestSupplier;
    private final DefaultTableModel documentModel = new DefaultTableModel(
            new String[]{"ID", "Número", "Título", "Tipo", "Versão", "Estado", "Autor", "Criado em"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable documentTable = new JTable(documentModel);
    private final JTextField documentSearch = new JTextField();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AiDocumentHistoryView(AiAssistantApiClient apiClient,
                                 Supplier<AiProfessionalDocumentRequest> documentRequestSupplier) {
        this.apiClient = Objects.requireNonNull(apiClient, "O cliente de inteligência é obrigatório.");
        this.documentRequestSupplier = documentRequestSupplier != null ? documentRequestSupplier : () -> null;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 0, 0, 0));

        add(buildCard(), BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::refresh);
    }

    private JPanel buildCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        UIHelper.styleTable(documentTable);
        documentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        documentTable.setFillsViewportHeight(true);
        documentTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        hideDocumentIdColumn();
        JScrollPane scroll = new JScrollPane(documentTable);
        UIHelper.styleScrollPane(scroll);

        UIHelper.styleTextField(documentSearch);
        documentSearch.putClientProperty("JTextField.placeholderText", "Pesquisar número, título, tipo ou estado");
        documentSearch.addActionListener(event -> refresh());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        ActionMenuButton workflow = UIHelper.createActionMenuButton("Fluxo do Documento")
                .addAction("Submeter para revisão", UIHelper.icon("fas-paper-plane", 14), this::submitSelectedDocument)
                .addAction("Aprovar", UIHelper.icon("fas-check", 14, UIHelper.APPROVED_GREEN), this::approveSelectedDocument)
                .addAction("Rejeitar", UIHelper.icon("fas-times", 14, UIHelper.REJECTED_RED), this::rejectSelectedDocument)
                .addAction("Arquivar", UIHelper.icon("fas-archive", 14), this::archiveSelectedDocument);

        ActionMenuButton versions = UIHelper.createActionMenuButton("Versões")
                .addAction("Abrir PDF", UIHelper.icon("fas-file-pdf", 14, UIHelper.REJECTED_RED), this::openSelectedDocumentPdf)
                .addAction("Criar nova versão", UIHelper.icon("fas-copy", 14, UIHelper.ACCENT_BLUE), this::createSelectedDocumentVersion)
                .addAction("Comparar versões", UIHelper.icon("fas-columns", 14), this::compareSelectedDocumentVersions);

        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        filters.add(documentSearch, BorderLayout.CENTER);

        card.add(UIHelper.tableCardTop("Histórico e Aprovação", filters, refreshBtn, workflow, versions), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(feedback, BorderLayout.SOUTH);
        return card;
    }

    public void refresh() {
        UIHelper.loadAsync(this, () -> apiClient.searchDocuments(documentSearch.getText().trim()), this::showDocuments,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o histórico: " + error.getMessage()));
    }

    private void showDocuments(List<AiManagedDocumentDTO> documents) {
        documentModel.setRowCount(0);
        for (AiManagedDocumentDTO document : documents) {
            documentModel.addRow(new Object[]{document.id(), document.documentNumber(), document.title(),
                    document.documentType(), document.currentVersion(), document.status(), document.authorUsername(),
                    document.createdAt() == null ? "" : document.createdAt().format(DATE_TIME)});
        }
    }

    private void submitSelectedDocument() { runDocumentAction("submeter", apiClient::submitDocument); }
    private void approveSelectedDocument() { runDocumentAction("aprovar", apiClient::approveDocument); }
    private void archiveSelectedDocument() { runDocumentAction("arquivar", apiClient::archiveDocument); }

    private void rejectSelectedDocument() {
        Long id = selectedDocumentId();
        if (id == null) return;
        JTextArea reason = new JTextArea(4, 40);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        UIHelper.styleTextArea(reason);
        JPanel form = new JPanel(new BorderLayout(0, 6));
        form.setOpaque(false);
        form.add(new JLabel("Motivo da rejeição"), BorderLayout.NORTH);
        form.add(new JScrollPane(reason), BorderLayout.CENTER);
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(this),
                "Rejeitar Documento", "fas-times-circle", form).setConfirmButton("Rejeitar", "fas-times");
        dialog.setOnSaveAsync(() -> {
            String value = reason.getText().trim();
            if (value.length() < 3) {
                reason.requestFocusInWindow();
                throw new IllegalArgumentException("Indique o motivo da rejeição.");
            }
            return () -> apiClient.rejectDocument(id, value);
        });
        if (dialog.showDialog()) refresh();
    }

    private void runDocumentAction(String label, Function<Long, AiManagedDocumentDTO> action) {
        Long id = selectedDocumentId();
        if (id == null) return;
        UIHelper.loadAsync(this, () -> action.apply(id), result -> {
            feedback.show(FeedbackType.SUCCESS, "Documento actualizado: " + result.status() + ".");
            refresh();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível " + label + " o documento: " + error.getMessage()));
    }

    private void openSelectedDocumentPdf() {
        int row = documentTable.getSelectedRow();
        Long id = selectedDocumentId();
        if (id == null) return;
        int modelRow = documentTable.convertRowIndexToModel(row);
        int version = ((Number) documentModel.getValueAt(modelRow, 4)).intValue();
        String title = String.valueOf(documentModel.getValueAt(modelRow, 2));
        UIHelper.loadAsync(this, () -> apiClient.getDocumentPdf(id, version),
                bytes -> PrintPreviewDialog.show(this, bytes, "documento-ia-v" + version, title),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível abrir o PDF: " + error.getMessage()));
    }

    private void createSelectedDocumentVersion() {
        Long id = selectedDocumentId();
        if (id == null) return;
        AiProfessionalDocumentRequest request = documentRequestSupplier.get();
        if (request == null) return;
        UIHelper.loadAsync(this, () -> apiClient.createDocumentVersion(id, request), result -> {
            feedback.show(FeedbackType.SUCCESS, "Nova versão " + result.currentVersion() + " guardada como rascunho.");
            refresh();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível criar a nova versão: " + error.getMessage()));
    }

    private void compareSelectedDocumentVersions() {
        Long id = selectedDocumentId();
        if (id == null) return;
        UIHelper.loadAsync(this, () -> apiClient.documentVersions(id), versions -> {
            if (versions.size() < 2) {
                feedback.show(FeedbackType.WARNING, "O documento precisa de pelo menos duas versões para comparar.");
                return;
            }
            AiDocumentComparisonDialog.show(this, versions.get(0), versions.get(1));
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível comparar as versões: " + error.getMessage()));
    }

    private Long selectedDocumentId() {
        int row = documentTable.getSelectedRow();
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione um documento no histórico.");
            return null;
        }
        return ((Number) documentModel.getValueAt(documentTable.convertRowIndexToModel(row), 0)).longValue();
    }

    private void hideDocumentIdColumn() {
        documentTable.getColumnModel().getColumn(0).setMinWidth(0);
        documentTable.getColumnModel().getColumn(0).setMaxWidth(0);
        documentTable.getColumnModel().getColumn(0).setPreferredWidth(0);
    }
}
