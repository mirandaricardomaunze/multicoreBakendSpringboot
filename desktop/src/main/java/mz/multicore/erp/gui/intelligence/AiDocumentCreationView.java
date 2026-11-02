package mz.multicore.erp.gui.intelligence;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiProfessionalDocumentRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Vista especializada de elaboração de documentos institucionais assistidos por inteligência.
 */
public class AiDocumentCreationView extends JPanel {

    private final AiAssistantApiClient apiClient;
    private final Supplier<AiAssistantResponse> lastResponseSupplier;
    private final Runnable onDocumentSaved;
    private final JComboBox<String> documentType = new JComboBox<>(new String[]{
            "Relatório", "Memorando", "Proposta", "Carta", "Acta", "Plano de Acção"});
    private final JTextField documentTitle = new JTextField();
    private final JTextArea documentInstructions = new JTextArea(3, 60);
    private final ModernButton documentButton = UIHelper.createSuccessButton("Gerar Documento");
    private final ModernButton saveDocumentButton = UIHelper.createSecondaryButton("Guardar no Histórico");
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public AiDocumentCreationView(AiAssistantApiClient apiClient,
                                  Supplier<AiAssistantResponse> lastResponseSupplier,
                                  Runnable onDocumentSaved) {
        this.apiClient = Objects.requireNonNull(apiClient, "O cliente de inteligência é obrigatório.");
        this.lastResponseSupplier = lastResponseSupplier != null ? lastResponseSupplier : () -> null;
        this.onDocumentSaved = onDocumentSaved;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 0, 0, 0));

        add(buildCard(), BorderLayout.CENTER);
    }

    private JPanel buildCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        UIHelper.styleComboBox(documentType);
        UIHelper.styleTextField(documentTitle);
        documentTitle.putClientProperty("JTextField.placeholderText", "Título do documento (ex.: Relatório Mensal de Operações)");
        documentInstructions.setLineWrap(true);
        documentInstructions.setWrapStyleWord(true);
        documentInstructions.setText("Descreva o objectivo, destinatário e os pontos que devem constar no documento.");
        UIHelper.styleTextArea(documentInstructions);

        documentButton.setIcon(UIHelper.icon("fas-file-pdf", 14, UIHelper.REJECTED_RED));
        documentButton.addActionListener(event -> generateProfessionalDocument());
        saveDocumentButton.setIcon(UIHelper.icon("fas-save", 14, UIHelper.ACCENT_BLUE));
        saveDocumentButton.addActionListener(event -> saveProfessionalDocument());

        JPanel formFields = new JPanel(new BorderLayout(10, 12));
        formFields.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        documentType.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));
        topRow.add(documentType, BorderLayout.WEST);
        topRow.add(documentTitle, BorderLayout.CENTER);
        formFields.add(topRow, BorderLayout.NORTH);

        JScrollPane instructionsScroll = new JScrollPane(documentInstructions);
        UIHelper.styleScrollPane(instructionsScroll);
        formFields.add(instructionsScroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Documentos fiscais e transaccionais continuam a exigir confirmação no módulo responsável.");
        hint.setForeground(UIHelper.TEXT_MUTED);

        card.add(UIHelper.tableCardTop("Elaboração de Documento Institucional", hint, saveDocumentButton, documentButton), BorderLayout.NORTH);
        card.add(formFields, BorderLayout.CENTER);
        card.add(feedback, BorderLayout.SOUTH);
        return card;
    }

    public AiProfessionalDocumentRequest buildCurrentRequest() {
        String title = documentTitle.getText().trim();
        String instructions = documentInstructions.getText().trim();
        if (title.isBlank() || instructions.length() < 5) {
            feedback.show(FeedbackType.WARNING, "Indique o título e descreva o documento pretendido.");
            if (title.isBlank()) documentTitle.requestFocusInWindow();
            return null;
        }
        String type = switch (documentType.getSelectedIndex()) {
            case 1 -> "MEMORANDO"; case 2 -> "PROPOSTA"; case 3 -> "CARTA";
            case 4 -> "ACTA"; case 5 -> "PLANO_ACCAO"; default -> "RELATORIO";
        };
        return new AiProfessionalDocumentRequest(type, title, instructions, lastResponseSupplier.get());
    }

    private void generateProfessionalDocument() {
        AiProfessionalDocumentRequest request = buildCurrentRequest();
        if (request == null) return;
        UIHelper.submitAsync(documentButton, () -> apiClient.renderDocument(request),
                pdf -> PrintPreviewDialog.show(this, pdf, "documento-profissional-ia", request.title()),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o documento: " + error.getMessage()));
    }

    private void saveProfessionalDocument() {
        AiProfessionalDocumentRequest request = buildCurrentRequest();
        if (request == null) return;
        UIHelper.submitAsync(saveDocumentButton, () -> apiClient.saveDocument(request), document -> {
            feedback.show(FeedbackType.SUCCESS, "Documento " + document.documentNumber() + " guardado como rascunho.");
            if (onDocumentSaved != null) onDocumentSaved.run();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível guardar o documento: " + error.getMessage()));
    }
}
