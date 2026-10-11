package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.components.ArrowScrollPanel;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiEvidenceDTO;
import mz.multicore.erp.modules.intelligence.dto.AiProfessionalDocumentRequest;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftResponse;
import mz.multicore.erp.modules.intelligence.dto.AiManagedDocumentDTO;
import mz.multicore.erp.modules.intelligence.dto.AiDocumentVersionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/** Assistente somente de leitura para gestores, alimentado exclusivamente por agregados do backend. */
public class AiManagementAssistantPanel extends JPanel {

    private final AiAssistantApiClient apiClient;
    private final Consumer<String> navigationHandler;
    private final JTextArea questionArea = new JTextArea(2, 60);
    private final JTextArea answerArea = new JTextArea(4, 60);
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel evidenceModel;
    private final ModernButton reportButton = UIHelper.createSecondaryButton("Gerar Relatório PDF");
    private final JComboBox<String> documentType = new JComboBox<>(new String[]{
            "Relatório", "Memorando", "Proposta", "Carta", "Acta", "Plano de Acção"});
    private final JTextField documentTitle = new JTextField();
    private final JTextArea documentInstructions = new JTextArea(3, 60);
    private final ModernButton documentButton = UIHelper.createPrimaryButton("Gerar Documento");
    private final ModernButton saveDocumentButton = UIHelper.createSecondaryButton("Guardar no Histórico");
    private final DefaultTableModel documentModel = new DefaultTableModel(
            new String[]{"ID", "Número", "Título", "Tipo", "Versão", "Estado", "Autor", "Criado em"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable documentTable = new JTable(documentModel);
    private final JTextField documentSearch = new JTextField();
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private AiAssistantResponse lastResponse;
    private AiOperationalDraftResponse lastDraft;
    private final JTextArea draftRequest = new JTextArea(3, 60);
    private final ModernButton openModuleButton = UIHelper.createSecondaryButton("Abrir no Módulo");

    private final JTextArea draftResultArea = new JTextArea();

    public AiManagementAssistantPanel(AiAssistantApiClient apiClient) {
        this(apiClient, null);
    }

    public AiManagementAssistantPanel(AiAssistantApiClient apiClient, Consumer<String> navigationHandler) {
        this.apiClient = apiClient;
        this.navigationHandler = navigationHandler;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(UIHelper.createHeading("Assistente de Gestão & Inteligência"), BorderLayout.WEST);
        JLabel mode = new JLabel("Análise executiva, rascunhos operacionais e elaboração de documentos institucionais");
        mode.setForeground(UIHelper.TEXT_MUTED);
        heading.add(mode, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        evidenceModel = new DefaultTableModel(new String[]{"Indicador", "Valor", "Período"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Consulta & Análise", UIHelper.icon("fas-brain", 16, UIHelper.ACCENT_BLUE), buildAnalysisTab());
        tabs.addTab("Rascunhos Operacionais", UIHelper.icon("fas-magic", 16, UIHelper.PENDING_YELLOW), buildDraftsTab());
        tabs.addTab("Documentos Institucionais", UIHelper.icon("fas-file-alt", 16, UIHelper.APPROVED_GREEN), buildDocumentsTab());
        tabs.addTab("Arquivo & Versões", UIHelper.icon("fas-history", 16, UIHelper.MODULE_ACCOUNTING), buildHistoryTab());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildAnalysisTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 14));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel questionCard = buildQuestionCard();
        questionCard.setPreferredSize(new Dimension(900, 160));
        tab.add(questionCard, BorderLayout.NORTH);

        JPanel splitBottom = new JPanel(new GridLayout(1, 2, 14, 0));
        splitBottom.setOpaque(false);

        // Resposta da IA
        ModernPanel answerCard = new ModernPanel(16);
        answerCard.setLayout(new BorderLayout(0, 10));
        answerCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        answerArea.setEditable(false);
        answerArea.setLineWrap(true);
        answerArea.setWrapStyleWord(true);
        UIHelper.styleTextArea(answerArea);
        JScrollPane answerScroll = new JScrollPane(answerArea);
        UIHelper.styleScrollPane(answerScroll);
        answerCard.add(UIHelper.tableCardTop("Análise Executiva", feedback), BorderLayout.NORTH);
        answerCard.add(answerScroll, BorderLayout.CENTER);
        splitBottom.add(answerCard);

        // Tabela de Evidências
        JTable evidenceTable = new JTable(evidenceModel);
        UIHelper.styleTable(evidenceTable);
        evidenceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        evidenceTable.setFillsViewportHeight(true);
        evidenceTable.getColumnModel().getColumn(0).setPreferredWidth(320);
        evidenceTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        evidenceTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        evidenceTable.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.quantity());
        JScrollPane evidenceScroll = new JScrollPane(evidenceTable);
        UIHelper.styleScrollPane(evidenceScroll);

        ModernPanel evidenceCard = new ModernPanel(16);
        evidenceCard.setLayout(new BorderLayout(0, 10));
        evidenceCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel evidenceHint = new JLabel("Indicadores que sustentam a resposta.");
        evidenceHint.setForeground(UIHelper.TEXT_MUTED);
        reportButton.setIcon(UIHelper.icon("fas-file-pdf", 14, UIHelper.REJECTED_RED));
        reportButton.setEnabled(false);
        reportButton.addActionListener(event -> generateReport());
        evidenceCard.add(UIHelper.tableCardTop("Evidências da Análise", evidenceHint, reportButton), BorderLayout.NORTH);
        evidenceCard.add(evidenceScroll, BorderLayout.CENTER);
        splitBottom.add(evidenceCard);

        tab.add(splitBottom, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildQuestionCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setText("Como estão as vendas nos últimos 30 dias?");
        UIHelper.styleTextArea(questionArea);
        JScrollPane scroll = new JScrollPane(questionArea);
        UIHelper.styleScrollPane(scroll);
        ModernButton button = UIHelper.createPrimaryButton("Analisar");
        button.setName("askManagementAiButton");
        button.setIcon(UIHelper.icon("fas-lightbulb", 14));
        button.addActionListener(event -> ask(button));
        JLabel notice = new JLabel("A IA apenas recomenda; confirme sempre os valores nos módulos operacionais.");
        notice.setForeground(UIHelper.PENDING_YELLOW);
        card.add(UIHelper.tableCardTop("Pergunta", notice, button), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDraftsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 14));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

        ModernPanel inputCard = new ModernPanel(16);
        inputCard.setLayout(new BorderLayout(0, 10));
        inputCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        inputCard.setPreferredSize(new Dimension(900, 190));
        draftRequest.setLineWrap(true);
        draftRequest.setWrapStyleWord(true);
        draftRequest.setText("Ex.: Criar cotação; cliente: ABC; produto: Serviço mensal; quantidade: 1");
        UIHelper.styleTextArea(draftRequest);
        JScrollPane scroll = new JScrollPane(draftRequest);
        UIHelper.styleScrollPane(scroll);
        ModernButton prepare = UIHelper.createPrimaryButton("Preparar Rascunho");
        prepare.setIcon(UIHelper.icon("fas-magic", 14));
        prepare.addActionListener(event -> prepareOperationalDraft(prepare));
        JLabel hint = new JLabel("Extrai apenas dados explícitos; não grava, não emite e não inventa cadastros.");
        hint.setForeground(UIHelper.TEXT_MUTED);
        inputCard.add(UIHelper.tableCardTop("Instrução em Linguagem Natural", hint, prepare), BorderLayout.NORTH);
        inputCard.add(scroll, BorderLayout.CENTER);
        tab.add(inputCard, BorderLayout.NORTH);

        ModernPanel resultCard = new ModernPanel(16);
        resultCard.setLayout(new BorderLayout(0, 10));
        resultCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        draftResultArea.setEditable(false);
        draftResultArea.setLineWrap(true);
        draftResultArea.setWrapStyleWord(true);
        draftResultArea.setText("Os detalhes e campos identificados no rascunho serão apresentados aqui após a preparação.");
        UIHelper.styleTextArea(draftResultArea);
        JScrollPane resultScroll = new JScrollPane(draftResultArea);
        UIHelper.styleScrollPane(resultScroll);

        openModuleButton.setIcon(UIHelper.icon("fas-external-link-alt", 14, UIHelper.ACCENT_BLUE));
        openModuleButton.setEnabled(false);
        openModuleButton.addActionListener(event -> {
            if (lastDraft != null && navigationHandler != null) navigationHandler.accept(lastDraft.targetModule());
        });
        JLabel resultNotice = new JLabel("A revisão final e confirmação ocorrem obrigatoriamente no módulo de destino.");
        resultNotice.setForeground(UIHelper.PENDING_YELLOW);
        resultCard.add(UIHelper.tableCardTop("Rascunho Controlado para Revisão", resultNotice, openModuleButton), BorderLayout.NORTH);
        resultCard.add(resultScroll, BorderLayout.CENTER);
        tab.add(resultCard, BorderLayout.CENTER);

        return tab;
    }

    private JPanel buildDocumentsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 14));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

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

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildHistoryTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 14));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

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
        documentSearch.addActionListener(event -> refreshDocuments());
        ModernButton refresh = UIHelper.createRefreshButton(this::refreshDocuments);
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
        card.add(UIHelper.tableCardTop("Histórico e Arquivo de Versões", filters, refresh, workflow, versions), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshDocuments);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void ask(ModernButton source) {
        String question = questionArea.getText().trim();
        if (question.length() < 3) {
            feedback.show(FeedbackType.WARNING, "Escreva uma pergunta com pelo menos três caracteres.");
            questionArea.requestFocusInWindow();
            return;
        }
        feedback.clear();
        UIHelper.submitAsync(source, () -> apiClient.ask(question), this::apply,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível consultar o assistente: " + error.getMessage()));
    }

    private void apply(AiAssistantResponse response) {
        lastResponse = response;
        reportButton.setEnabled(true);
        answerArea.setText(response.answer());
        answerArea.setCaretPosition(0);
        evidenceModel.setRowCount(0);
        for (AiEvidenceDTO item : response.evidence()) {
            evidenceModel.addRow(new Object[]{item.label(), item.value(), item.period()});
        }
        feedback.show(FeedbackType.INFO,
                response.externalModelUsed() ? "Análise gerada pelo modelo configurado." : "Análise local segura; modelo externo indisponível ou não configurado.");
    }

    private void generateReport() {
        if (lastResponse == null) return;
        UIHelper.submitAsync(reportButton, () -> apiClient.renderReport(lastResponse),
                pdf -> PrintPreviewDialog.show(this, pdf, "relatorio-gestao-ia", "Relatório de Gestão Assistido por IA"),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o relatório: " + error.getMessage()));
    }

    private void generateProfessionalDocument() {
        String title = documentTitle.getText().trim();
        String instructions = documentInstructions.getText().trim();
        if (title.isBlank() || instructions.length() < 5) {
            feedback.show(FeedbackType.WARNING, "Indique o título e descreva o documento pretendido.");
            if (title.isBlank()) documentTitle.requestFocusInWindow();
            return;
        }
        String type = switch (documentType.getSelectedIndex()) {
            case 1 -> "MEMORANDO";
            case 2 -> "PROPOSTA";
            case 3 -> "CARTA";
            case 4 -> "ACTA";
            case 5 -> "PLANO_ACCAO";
            default -> "RELATORIO";
        };
        AiProfessionalDocumentRequest request = new AiProfessionalDocumentRequest(type, title, instructions, lastResponse);
        UIHelper.submitAsync(documentButton, () -> apiClient.renderDocument(request),
                pdf -> PrintPreviewDialog.show(this, pdf, "documento-profissional-ia", title),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o documento: " + error.getMessage()));
    }

    private void saveProfessionalDocument() {
        AiProfessionalDocumentRequest request = documentRequest();
        if (request == null) return;
        UIHelper.submitAsync(saveDocumentButton, () -> apiClient.saveDocument(request), document -> {
            feedback.show(FeedbackType.SUCCESS, "Documento " + document.documentNumber() + " guardado como rascunho.");
            refreshDocuments();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível guardar o documento: " + error.getMessage()));
    }

    private AiProfessionalDocumentRequest documentRequest() {
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
        return new AiProfessionalDocumentRequest(type, title, instructions, lastResponse);
    }

    private void refreshDocuments() {
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
        if (dialog.showDialog()) refreshDocuments();
    }

    private void runDocumentAction(String label, java.util.function.Function<Long, AiManagedDocumentDTO> action) {
        Long id = selectedDocumentId();
        if (id == null) return;
        UIHelper.loadAsync(this, () -> action.apply(id), result -> {
            feedback.show(FeedbackType.SUCCESS, "Documento actualizado: " + result.status() + ".");
            refreshDocuments();
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
        AiProfessionalDocumentRequest request = documentRequest();
        if (request == null) return;
        UIHelper.loadAsync(this, () -> apiClient.createDocumentVersion(id, request), result -> {
            feedback.show(FeedbackType.SUCCESS, "Nova versão " + result.currentVersion() + " guardada como rascunho.");
            refreshDocuments();
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
            showVersionComparison(versions.get(0), versions.get(1));
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível comparar as versões: " + error.getMessage()));
    }

    private void showVersionComparison(AiDocumentVersionDTO current, AiDocumentVersionDTO previous) {
        JTextArea oldText = comparisonText(previous);
        JTextArea newText = comparisonText(current);
        JPanel comparison = new JPanel(new GridLayout(1, 2, 12, 0));
        comparison.setOpaque(false);
        comparison.add(versionColumn("Versão " + previous.versionNumber(), oldText));
        comparison.add(versionColumn("Versão " + current.versionNumber(), newText));
        new ModernFormDialog(SwingUtilities.getWindowAncestor(this), "Comparar Versões", "fas-columns", comparison)
                .asReadOnly("Fechar").setSize(920, 560).showDialog();
    }

    private JTextArea comparisonText(AiDocumentVersionDTO version) {
        JTextArea area = new JTextArea(version.generatedContent());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        UIHelper.styleTextArea(area);
        return area;
    }

    private JPanel versionColumn(String title, JTextArea text) {
        JPanel column = new JPanel(new BorderLayout(0, 6));
        column.setOpaque(false);
        column.add(UIHelper.createSubheading(title), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(text);
        UIHelper.styleScrollPane(scroll);
        column.add(scroll, BorderLayout.CENTER);
        return column;
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

    private void prepareOperationalDraft(ModernButton source) {
        String request = draftRequest.getText().trim();
        if (request.length() < 8) {
            feedback.show(FeedbackType.WARNING, "Descreva o rascunho e os dados conhecidos.");
            draftRequest.requestFocusInWindow();
            return;
        }
        UIHelper.submitAsync(source, () -> apiClient.prepareOperationalDraft(request), draft -> {
            lastDraft = draft;
            openModuleButton.setEnabled(navigationHandler != null);
            String fields = draft.explicitFields().isEmpty() ? "Nenhum campo estruturado reconhecido."
                    : "Campos explícitos: " + String.join("; ", draft.explicitFields());
            String missing = draft.missingFields().isEmpty() ? "Dados mínimos reconhecidos para revisão."
                    : "Dados em falta: " + String.join(", ", draft.missingFields());
            String text = draft.summary() + "\n\n" + fields + "\n" + missing + "\n\n" + draft.warning();
            answerArea.setText(text);
            answerArea.setCaretPosition(0);
            draftResultArea.setText(text);
            draftResultArea.setCaretPosition(0);
            feedback.show(FeedbackType.INFO, "Rascunho preparado para revisão no módulo " + draft.targetModule() + ".");
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível preparar o rascunho: " + error.getMessage()));
    }

}
