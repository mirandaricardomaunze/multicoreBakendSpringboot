package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.VersionApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.desktop.update.ClientUpdateService;
import mz.multicore.erp.gui.components.StatusBar;
import mz.multicore.erp.gui.pos.suspended.PosSuspendedSaleStore;

import javax.swing.JFrame;
import java.awt.event.WindowEvent;

/** Wires the status-bar version notice of the main window to the one-click update (spec §4.1). */
final class MainFrameClientUpdate {

    private final JFrame frame;
    private final StatusBar statusBar;
    private final VersionApiClient versionApiClient;
    private final POSApiClient posApiClient;
    private final DesktopSessionStore sessionStore;

    MainFrameClientUpdate(JFrame frame, StatusBar statusBar, VersionApiClient versionApiClient,
                          POSApiClient posApiClient, DesktopSessionStore sessionStore) {
        this.frame = frame;
        this.statusBar = statusBar;
        this.versionApiClient = versionApiClient;
        this.posApiClient = posApiClient;
        this.sessionStore = sessionStore;
    }

    void start() {
        ClientUpdateFlow.start(frame, new ClientUpdateService(versionApiClient.apiConfig()),
                new ClientUpdateFlow.Context(false, this::tillSessionOpen, this::suspendedSales,
                        // The normal close path: saves the window, logs out, then the frame exits the program.
                        () -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)),
                        statusBar::setUpdateScheduled));
    }

    /** Called off the EDT. If the server cannot answer, refuse rather than risk closing an open till. */
    private boolean tillSessionOpen() {
        DesktopSession session = sessionStore.requireSession();
        if (session.superAdmin() || posApiClient == null) return false;
        try {
            return posApiClient.getActiveSession(session.username(), session.activeCompanyId()).isPresent();
        } catch (RuntimeException error) {
            return true;
        }
    }

    private int suspendedSales() {
        DesktopSession session = sessionStore.requireSession();
        if (session.superAdmin()) return 0;
        return new PosSuspendedSaleStore().forCompany(session.activeCompanyId()).size();
    }
}
