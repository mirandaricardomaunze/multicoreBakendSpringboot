package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.desktop.client.SystemMonitoringApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.platform.PlatformClientVersionsView;
import mz.multicore.erp.gui.platform.PlatformCompaniesView;
import mz.multicore.erp.gui.platform.PlatformFeedbackHandler;
import mz.multicore.erp.gui.platform.PlatformSubscriptionsView;
import mz.multicore.erp.gui.platform.PlatformSupportTicketsView;
import mz.multicore.erp.gui.platform.PlatformSystemMonitoringView;
import mz.multicore.erp.gui.platform.PlatformUsersView;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Consola da plataforma (superadmin).
 * Orquestrador modular decomposto em sub-views especializadas no pacote mz.multicore.erp.gui.platform.
 */
public class PlataformaPanel extends JPanel implements PlatformFeedbackHandler {

    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private final PlatformCompaniesView companiesView;
    private final PlatformSubscriptionsView subscriptionsView;
    private final PlatformUsersView usersView;
    private final PlatformSupportTicketsView supportTicketsView;
    private final PlatformClientVersionsView clientVersionsView;

    public PlataformaPanel(PlatformApiClient platformApiClient) {
        this(platformApiClient, null);
    }

    public PlataformaPanel(PlatformApiClient platformApiClient, SystemMonitoringApiClient monitoringApiClient) {
        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        this.companiesView = new PlatformCompaniesView(platformApiClient, this);
        this.subscriptionsView = new PlatformSubscriptionsView(platformApiClient, this);
        this.usersView = new PlatformUsersView(platformApiClient, companiesView::getCompanies, this);
        this.supportTicketsView = new PlatformSupportTicketsView(platformApiClient, this);
        this.clientVersionsView = new PlatformClientVersionsView(platformApiClient, this);

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Empresas", UIHelper.icon("fas-building", 16, UIHelper.ACCENT_BLUE), companiesView);
        tabbedPane.addTab("Assinaturas & Pagamentos", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.APPROVED_GREEN),
                subscriptionsView);
        tabbedPane.addTab("Utilizadores", UIHelper.icon("fas-users-cog", 16, UIHelper.ACCENT), usersView);
        tabbedPane.addTab("Assistência", UIHelper.icon("fas-headset", 16, UIHelper.ACCENT_CYAN), supportTicketsView);
        tabbedPane.addTab("Versões dos Clientes", UIHelper.icon("fas-code-branch", 16, UIHelper.ACCENT_ORANGE),
                clientVersionsView);

        if (monitoringApiClient != null) {
            tabbedPane.addTab("Saúde & Diagnóstico", UIHelper.icon("fas-heartbeat", 16, UIHelper.ACCENT_CYAN),
                    new PlatformSystemMonitoringView(monitoringApiClient));
        }

        add(feedback, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    /** Chamado pela MainFrame quando o painel fica activo. */
    public void onPanelSelected() {
        companiesView.loadCompanies();
        subscriptionsView.loadSubscriptions();
        usersView.loadUsers();
        supportTicketsView.loadTickets();
        clientVersionsView.loadClientVersions();
    }

    @Override
    public void showError(String area, Throwable error, Runnable retry) {
        feedback.show(FeedbackType.ERROR, "Não foi possível processar " + area,
                error.getMessage(), "Tentar novamente", retry != null ? retry : this::onPanelSelected);
    }

    @Override
    public void showNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    @Override
    public void showSuccess(String message) {
        ToastManager.success(this, message);
    }

    /** Retrocompatibilidade estática para renderização de conversas de suporte. */
    public static String renderThread(List<SupportMessageDTO> messages) {
        return PlatformSupportTicketsView.renderThread(messages);
    }

    public PlatformCompaniesView getCompaniesView() { return companiesView; }
    public PlatformSubscriptionsView getSubscriptionsView() { return subscriptionsView; }
    public PlatformUsersView getUsersView() { return usersView; }
    public PlatformSupportTicketsView getSupportTicketsView() { return supportTicketsView; }
    public PlatformClientVersionsView getClientVersionsView() { return clientVersionsView; }
}
