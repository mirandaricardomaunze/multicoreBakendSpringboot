package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.intelligence.AiAnalysisView;
import mz.multicore.erp.gui.intelligence.AiDocumentCreationView;
import mz.multicore.erp.gui.intelligence.AiDocumentHistoryView;
import mz.multicore.erp.gui.intelligence.AiOperationalDraftsView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Assistente somente de leitura para gestores, alimentado exclusivamente por agregados do backend.
 * Orquestrador canónico de abas: análise executiva, rascunhos operacionais, elaboração de documentos e histórico.
 */
public class AiManagementAssistantPanel extends JPanel {

    private final AiAnalysisView analysisView;
    private final AiOperationalDraftsView draftsView;
    private final AiDocumentCreationView documentsView;
    private final AiDocumentHistoryView historyView;

    public AiManagementAssistantPanel(AiAssistantApiClient apiClient) {
        this(apiClient, null);
    }

    public AiManagementAssistantPanel(AiAssistantApiClient apiClient, Consumer<String> navigationHandler) {
        Objects.requireNonNull(apiClient, "O cliente de inteligência é obrigatório.");
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

        this.analysisView = new AiAnalysisView(apiClient);
        this.draftsView = new AiOperationalDraftsView(apiClient, navigationHandler);

        AiDocumentHistoryView[] historyHolder = new AiDocumentHistoryView[1];
        this.documentsView = new AiDocumentCreationView(apiClient,
                analysisView::getLastResponse,
                () -> { if (historyHolder[0] != null) historyHolder[0].refresh(); });
        this.historyView = new AiDocumentHistoryView(apiClient, documentsView::buildCurrentRequest);
        historyHolder[0] = this.historyView;

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Consulta & Análise", UIHelper.icon("fas-brain", 16, UIHelper.ACCENT_BLUE), analysisView);
        tabs.addTab("Rascunho Operacional Controlado", UIHelper.icon("fas-magic", 16, UIHelper.PENDING_YELLOW), draftsView);
        tabs.addTab("Criar Documento Profissional", UIHelper.icon("fas-file-alt", 16, UIHelper.APPROVED_GREEN), documentsView);
        tabs.addTab("Arquivo & Versões", UIHelper.icon("fas-history", 16, UIHelper.MODULE_ACCOUNTING), historyView);

        add(tabs, BorderLayout.CENTER);
    }

    public AiAnalysisView getAnalysisView() {
        return analysisView;
    }

    public AiOperationalDraftsView getDraftsView() {
        return draftsView;
    }

    public AiDocumentCreationView getDocumentsView() {
        return documentsView;
    }

    public AiDocumentHistoryView getHistoryView() {
        return historyView;
    }
}
