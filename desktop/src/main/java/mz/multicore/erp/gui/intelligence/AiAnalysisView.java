package mz.multicore.erp.gui.intelligence;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiEvidenceDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Vista especializada de Consulta & Análise Executiva com evidências e indicadores quantitativos.
 */
public class AiAnalysisView extends JPanel {

    private final AiAssistantApiClient apiClient;
    private final JTextArea questionArea = new JTextArea(2, 60);
    private final JTextArea answerArea = new JTextArea(4, 60);
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JTabbedPane analysisResultsTabs = new JTabbedPane();
    private final DefaultTableModel evidenceModel;
    private final ModernButton reportButton = UIHelper.createSecondaryButton("Gerar Relatório PDF");
    private AiAssistantResponse lastResponse;
    private Consumer<AiAssistantResponse> onResponseListener;

    public AiAnalysisView(AiAssistantApiClient apiClient) {
        this.apiClient = Objects.requireNonNull(apiClient, "O cliente de inteligência é obrigatório.");
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 0, 0, 0));

        evidenceModel = new DefaultTableModel(new String[]{"Indicador", "Valor", "Período"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        add(buildQuestionCard(), BorderLayout.NORTH);
        add(buildResultsCard(), BorderLayout.CENTER);
    }

    public void setOnResponseListener(Consumer<AiAssistantResponse> listener) {
        this.onResponseListener = listener;
    }

    public AiAssistantResponse getLastResponse() {
        return lastResponse;
    }

    private JPanel buildQuestionCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setText("Como estão as vendas nos últimos 30 dias?");
        questionArea.setRows(2);
        UIHelper.styleTextArea(questionArea);
        JScrollPane scroll = new JScrollPane(questionArea);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(500, 46));

        ModernButton button = UIHelper.createPrimaryButton("Analisar");
        button.setName("askManagementAiButton");
        button.setIcon(UIHelper.icon("fas-lightbulb", 14));
        button.addActionListener(event -> ask(button));
        button.setPreferredSize(new Dimension(130, 46));

        JLabel notice = new JLabel("A IA apenas recomenda; confirme sempre os valores nos módulos operacionais.");
        notice.setForeground(UIHelper.PENDING_YELLOW);
        notice.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        topRow.add(UIHelper.createSubheading("Pergunta"), BorderLayout.WEST);
        topRow.add(notice, BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(scroll, BorderLayout.CENTER);
        inputRow.add(button, BorderLayout.EAST);

        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chipsRow.setOpaque(false);
        JLabel chipsLabel = new JLabel("Sugestões:");
        chipsLabel.setForeground(UIHelper.TEXT_MUTED);
        chipsLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        chipsRow.add(chipsLabel);
        addQuickChip(chipsRow, "Vendas 30 Dias", "Como estão as vendas nos últimos 30 dias?");
        addQuickChip(chipsRow, "Stock Crítico", "Quais artigos estão esgotados ou com stock crítico?");
        addQuickChip(chipsRow, "Contas a Receber", "Quais clientes têm facturas vencidas e saldos pendentes?");
        addQuickChip(chipsRow, "Desempenho Geral", "Qual é o resumo executivo das operações da empresa?");

        card.add(topRow, BorderLayout.NORTH);
        card.add(inputRow, BorderLayout.CENTER);
        card.add(chipsRow, BorderLayout.SOUTH);
        return card;
    }

    private void addQuickChip(JPanel container, String label, String query) {
        ModernButton chip = UIHelper.createSecondaryButton(label);
        chip.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        chip.setPreferredSize(new Dimension(chip.getPreferredSize().width, 26));
        chip.addActionListener(e -> {
            questionArea.setText(query);
            questionArea.requestFocusInWindow();
        });
        container.add(chip);
    }

    private JComponent buildResultsCard() {
        UIHelper.styleTabbedPaneMulticore(analysisResultsTabs);

        // 1. Aba: Análise Executiva
        ModernPanel answerCard = new ModernPanel(16);
        answerCard.setLayout(new BorderLayout(0, 10));
        answerCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        answerArea.setEditable(false);
        answerArea.setLineWrap(true);
        answerArea.setWrapStyleWord(true);
        UIHelper.styleTextArea(answerArea);
        JScrollPane answerScroll = new JScrollPane(answerArea);
        UIHelper.styleScrollPane(answerScroll);
        reportButton.setIcon(UIHelper.icon("fas-file-pdf", 14, UIHelper.REJECTED_RED));
        reportButton.setEnabled(false);
        reportButton.addActionListener(event -> generateReport());
        answerCard.add(UIHelper.tableCardTop("Análise Executiva", feedback, reportButton), BorderLayout.NORTH);
        answerCard.add(answerScroll, BorderLayout.CENTER);
        analysisResultsTabs.addTab("Análise Executiva", UIHelper.icon("fas-file-alt", 15, UIHelper.ACCENT_BLUE), answerCard);

        // 2. Aba: Evidências & Indicadores (Tabela com altura e largura completas)
        JTable evidenceTable = new JTable(evidenceModel);
        UIHelper.styleTable(evidenceTable);
        evidenceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        evidenceTable.setFillsViewportHeight(true);
        evidenceTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        evidenceTable.putClientProperty("emptyText", "Sem evidências.");
        evidenceTable.putClientProperty("emptySubtext", "Submeta uma pergunta para ver os indicadores calculados.");
        evidenceTable.getColumnModel().getColumn(0).setPreferredWidth(360);
        evidenceTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        evidenceTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        evidenceTable.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.quantity());
        UIHelper.ensureHeadersFit(evidenceTable);
        JScrollPane evidenceScroll = new JScrollPane(evidenceTable);
        UIHelper.styleScrollPane(evidenceScroll);

        ModernPanel evidenceCard = new ModernPanel(16);
        evidenceCard.setLayout(new BorderLayout(0, 10));
        evidenceCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel evidenceHint = new JLabel("Indicadores e dados contabilísticos/operacionais que sustentam a análise.");
        evidenceHint.setForeground(UIHelper.TEXT_MUTED);
        ModernButton exportBtn = UIHelper.createExportButton(evidenceTable, "evidencias-analise-ia");
        evidenceCard.add(UIHelper.tableCardTop("Evidências da Análise", evidenceHint, exportBtn), BorderLayout.NORTH);
        evidenceCard.add(evidenceScroll, BorderLayout.CENTER);
        analysisResultsTabs.addTab("Evidências & Indicadores", UIHelper.icon("fas-table", 15, UIHelper.APPROVED_GREEN), evidenceCard);

        return analysisResultsTabs;
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
        this.lastResponse = response;
        reportButton.setEnabled(true);
        answerArea.setText(response.answer());
        answerArea.setCaretPosition(0);
        evidenceModel.setRowCount(0);
        for (AiEvidenceDTO item : response.evidence()) {
            evidenceModel.addRow(new Object[]{item.label(), item.value(), item.period()});
        }
        int count = response.evidence() != null ? response.evidence().size() : 0;
        if (analysisResultsTabs.getTabCount() > 1) {
            analysisResultsTabs.setTitleAt(1, count > 0 ? "Evidências & Indicadores (" + count + ")" : "Evidências & Indicadores");
        }
        feedback.show(FeedbackType.INFO,
                response.externalModelUsed() ? "Análise gerada pelo modelo configurado." : "Análise local segura; modelo externo indisponível ou não configurado.");
        if (onResponseListener != null) {
            onResponseListener.accept(response);
        }
    }

    private void generateReport() {
        if (lastResponse == null) return;
        UIHelper.submitAsync(reportButton, () -> apiClient.renderReport(lastResponse),
                pdf -> PrintPreviewDialog.show(this, pdf, "relatorio-gestao-ia", "Relatório de Gestão Assistido por IA"),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o relatório: " + error.getMessage()));
    }
}
