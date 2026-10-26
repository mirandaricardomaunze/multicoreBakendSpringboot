package mz.multicore.erp.gui.intelligence;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Vista especializada de rascunhos operacionais controlados a partir de linguagem natural.
 */
public class AiOperationalDraftsView extends JPanel {

    private final AiAssistantApiClient apiClient;
    private final Consumer<String> navigationHandler;
    private final JTextArea draftRequest = new JTextArea(3, 60);
    private final JTextArea draftResultArea = new JTextArea();
    private final ModernButton openModuleButton = UIHelper.createSecondaryButton("Abrir no Módulo");
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private AiOperationalDraftResponse lastDraft;

    public AiOperationalDraftsView(AiAssistantApiClient apiClient, Consumer<String> navigationHandler) {
        this.apiClient = Objects.requireNonNull(apiClient, "O cliente de inteligência é obrigatório.");
        this.navigationHandler = navigationHandler;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 0, 0, 0));

        add(buildInputCard(), BorderLayout.NORTH);
        add(buildResultCard(), BorderLayout.CENTER);
    }

    private JPanel buildInputCard() {
        ModernPanel inputCard = new ModernPanel(16);
        inputCard.setLayout(new BorderLayout(0, 8));
        inputCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        draftRequest.setLineWrap(true);
        draftRequest.setWrapStyleWord(true);
        draftRequest.setText("Ex.: Criar cotação; cliente: ABC; produto: Serviço mensal; quantidade: 1");
        draftRequest.setRows(2);
        UIHelper.styleTextArea(draftRequest);
        JScrollPane scroll = new JScrollPane(draftRequest);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(500, 46));

        ModernButton prepare = UIHelper.createPrimaryButton("Preparar Rascunho");
        prepare.setIcon(UIHelper.icon("fas-magic", 14));
        prepare.addActionListener(event -> prepareOperationalDraft(prepare));
        prepare.setPreferredSize(new Dimension(180, 46));

        JLabel hint = new JLabel("Extrai apenas dados explícitos; não grava, não emite e não inventa cadastros.");
        hint.setForeground(UIHelper.TEXT_MUTED);
        hint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);
        topRow.add(UIHelper.createSubheading("Instrução em Linguagem Natural"), BorderLayout.WEST);
        topRow.add(hint, BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(scroll, BorderLayout.CENTER);
        inputRow.add(prepare, BorderLayout.EAST);

        inputCard.add(topRow, BorderLayout.NORTH);
        inputCard.add(inputRow, BorderLayout.CENTER);
        return inputCard;
    }

    private JPanel buildResultCard() {
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
        resultCard.add(feedback, BorderLayout.SOUTH);
        resultCard.add(resultScroll, BorderLayout.CENTER);
        return resultCard;
    }

    private void prepareOperationalDraft(ModernButton source) {
        String request = draftRequest.getText().trim();
        if (request.length() < 8) {
            feedback.show(FeedbackType.WARNING, "Descreva o rascunho e os dados conhecidos.");
            draftRequest.requestFocusInWindow();
            return;
        }
        feedback.clear();
        UIHelper.submitAsync(source, () -> apiClient.prepareOperationalDraft(request), draft -> {
            lastDraft = draft;
            openModuleButton.setEnabled(navigationHandler != null);
            String fields = draft.explicitFields().isEmpty() ? "Nenhum campo estruturado reconhecido."
                    : "Campos explícitos: " + String.join("; ", draft.explicitFields());
            String missing = draft.missingFields().isEmpty() ? "Dados mínimos reconhecidos para revisão."
                    : "Dados em falta: " + String.join(", ", draft.missingFields());
            String text = draft.summary() + "\n\n" + fields + "\n" + missing + "\n\n" + draft.warning();
            draftResultArea.setText(text);
            draftResultArea.setCaretPosition(0);
            feedback.show(FeedbackType.INFO, "Rascunho preparado para revisão no módulo " + draft.targetModule() + ".");
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível preparar o rascunho: " + error.getMessage()));
    }
}
