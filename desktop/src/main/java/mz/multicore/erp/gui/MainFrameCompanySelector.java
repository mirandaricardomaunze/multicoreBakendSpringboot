package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/**
 * Seletor corporativo de empresas e contextualização da sessão no {@link MainFrame}.
 */
final class MainFrameCompanySelector {

    private MainFrameCompanySelector() {
    }

    static JComboBox<DesktopSession.CompanyAccess> buildCompanyCombo(
            DesktopSessionStore desktopSessionStore,
            Runnable onCompanyChanged
    ) {
        DesktopSession session = desktopSessionStore.requireSession();
        List<DesktopSession.CompanyAccess> companies = session.companies();
        JComboBox<DesktopSession.CompanyAccess> combo =
                new JComboBox<>(companies.toArray(new DesktopSession.CompanyAccess[0]));

        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MainFrame.class);
        String prefKey = "last_selected_company_id_" + (session.username() != null ? session.username() : "default");
        long lastCompanyId = prefs.getLong(prefKey, -1L);

        DesktopSession.CompanyAccess initial = companies.stream()
                .filter(c -> c.id().equals(lastCompanyId))
                .findFirst()
                .orElse(companies.isEmpty() ? null : companies.get(0));

        if (initial != null) {
            combo.setSelectedItem(initial);
            selectDesktopCompany(desktopSessionStore, initial);
        }

        combo.addActionListener(e -> {
            DesktopSession.CompanyAccess selected = (DesktopSession.CompanyAccess) combo.getSelectedItem();
            if (selected != null) {
                try {
                    prefs.putLong(prefKey, selected.id());
                } catch (Exception ignored) {
                }
                selectDesktopCompany(desktopSessionStore, selected);
                if (onCompanyChanged != null) {
                    onCompanyChanged.run();
                }
            }
        });

        UIHelper.styleComboBox(combo);
        combo.setToolTipText("Empresa ativa");
        combo.setPreferredSize(new Dimension(210, 32));
        applyCompanyRenderer(combo);

        return combo;
    }

    static void applyCompanyRenderer(JComboBox<DesktopSession.CompanyAccess> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? UIHelper.GRID : UIHelper.BG_CARD);
                setForeground(UIHelper.TEXT_LIGHT);
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (value instanceof DesktopSession.CompanyAccess company) {
                    String roleColor = isSelected ? "#E5E7EB" : "#9CA3AF";
                    setText("<html>" + company.name()
                            + " <font color='" + roleColor + "'>· " + UIHelper.humanRole(company.role())
                            + "</font></html>");
                }
                return this;
            }
        });
    }

    static void selectDesktopCompany(DesktopSessionStore desktopSessionStore, DesktopSession.CompanyAccess company) {
        DesktopSession session = desktopSessionStore.requireSession();
        session.selectCompany(company.id());
        CurrentUserContext.setCurrentUser(session.username(), company.role());
        CurrentUserContext.setCurrentCompanyId(company.id());
    }
}
