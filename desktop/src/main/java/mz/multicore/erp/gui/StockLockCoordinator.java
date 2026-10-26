package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Coordenador do bloqueio de stock para contagem cega (ocultação de saldos a não-administradores).
 */
public class StockLockCoordinator {

    private final StockPanel owner;
    private JLabel stockLockBanner;
    private ActionMenuButton moreBtn;
    private ActionMenuButton levelsMoreBtn;
    private boolean stockLockKnown;
    private boolean stockLocked;

    public StockLockCoordinator(StockPanel owner) {
        this.owner = owner;
    }

    public void initControls(ActionMenuButton moreBtn, ActionMenuButton levelsMoreBtn, JLabel stockLockBanner) {
        this.moreBtn = moreBtn;
        this.levelsMoreBtn = levelsMoreBtn;
        this.stockLockBanner = stockLockBanner;
        if (this.stockLockBanner != null) {
            this.stockLockBanner.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
            this.stockLockBanner.setBorder(new EmptyBorder(8, 2, 0, 0));
            this.stockLockBanner.setVisible(false);
        }
    }

    public static boolean isAdmin() {
        return SignedInUser.isAdmin();
    }

    public boolean isStockHidden() {
        return !isAdmin() && (!stockLockKnown || stockLocked);
    }

    public void refreshStockLock() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.isStockCountLocked(companyId),
                this::applyStockLock, error -> applyStockLock(false));
    }

    public void applyStockLock(boolean locked) {
        stockLockKnown = true;
        stockLocked = locked;
        if (moreBtn != null && moreBtn.actionCount() > 1) {
            moreBtn.actionAt(1).setVisible(isAdmin());
            moreBtn.actionAt(1).setText(locked ? "Destrancar Stock" : "Trancar Stock");
            moreBtn.actionAt(1).setIcon(UIHelper.icon(locked ? "fas-lock-open" : "fas-lock", 14));
        }
        if (levelsMoreBtn != null && levelsMoreBtn.actionCount() > 4) {
            levelsMoreBtn.actionAt(4).setVisible(isAdmin());
            levelsMoreBtn.actionAt(4).setText(locked ? "Destrancar Stock" : "Trancar Stock");
            levelsMoreBtn.actionAt(4).setIcon(UIHelper.icon(locked ? "fas-lock-open" : "fas-lock", 14));
        }
        if (stockLockBanner != null) {
            if (!locked) {
                stockLockBanner.setVisible(false);
            } else {
                stockLockBanner.setVisible(true);
                stockLockBanner.setIcon(UIHelper.icon("fas-lock", 13, UIHelper.PENDING_YELLOW));
                stockLockBanner.setForeground(UIHelper.PENDING_YELLOW);
                stockLockBanner.setText(isAdmin()
                        ? "Stock trancado — os funcionários não veem quantidades. Como administrador, vê tudo."
                        : "Stock trancado — quantidades visíveis apenas para administradores.");
            }
        }
    }

    public void toggleStockLock() {
        if (!isAdmin()) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        boolean target = !stockLocked;
        UIHelper.runWithProgress(owner, "A actualizar bloqueio do stock…", () -> {
            owner.inventoryApiClient.setStockCountLocked(companyId, target);
            return null;
        }, ignored -> owner.onPanelSelected(), owner::showStockError);
    }
}
