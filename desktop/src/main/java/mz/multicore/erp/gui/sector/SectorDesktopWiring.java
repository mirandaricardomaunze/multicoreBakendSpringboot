package mz.multicore.erp.gui.sector;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.desktop.client.UnitsApiClient;

import javax.swing.*;
import java.awt.*;

/**
 * Connects the business-sector add-ons that belong inside existing screens without changing those panels
 * (several are at the line ceiling of their decomposition harnesses):
 * <ul>
 *   <li>invoice and order grids gain "Unidade" (Ctrl+U), "Frete" (Ctrl+Alt+F) and a visible draft total;</li>
 *   <li>the Stock panel gains the "Granel" tab and the product detail its "Unidades" tab.</li>
 * </ul>
 * Whole modules (Preços, Levantamentos, Obras) are separate screens built by {@code SectorModules}.
 * Without these calls (tests and legacy constructors) the panels stay exactly as they were.
 */
public final class SectorDesktopWiring {

    /** Swing client-property key holding the {@link UnitsApiClient} on the Stock panel. */
    static final String UNITS_CLIENT_KEY = "multicore.sector.unitsApiClient";
    static final String BULK_TAB = "Granel";
    static final String UNIT_ENTRY_KEY = "multicore.sector.unitQuantityEntry";
    static final String FREIGHT_ENTRY_KEY = "multicore.sector.freightEntry";
    static final String TOOLBAR_KEY = "multicore.sector.draftToolbar";

    private SectorDesktopWiring() {
    }

    /** Appends the "Granel" tab to the Stock panel; it loads when first shown. */
    public static boolean installBulkStock(JComponent stockPanel, mz.multicore.erp.desktop.client.BulkStockApiClient bulkApi,
                                           mz.multicore.erp.desktop.client.InventoryApiClient inventoryApi,
                                           ComercialApiClient comercialApi) {
        if (stockPanel == null || bulkApi == null) return false;
        JTabbedPane tabs = directTabbedPane(stockPanel);
        if (tabs == null) return false;
        if (tabs.indexOfTab(BULK_TAB) >= 0) return true;
        BulkStockPanel panel = new BulkStockPanel(bulkApi, inventoryApi, comercialApi);
        tabs.addTab(BULK_TAB, mz.multicore.erp.gui.components.UIHelper.icon("fas-mountain", 16,
                mz.multicore.erp.gui.components.UIHelper.ACCENT_ORANGE), panel);
        tabs.setToolTipTextAt(tabs.getTabCount() - 1, "Areia, brita e pedra: inventário por medição do monte");
        return true;
    }

    /**
     * Adds "Quantidade noutra unidade" (Ctrl+U and the right-click menu) to the invoice and order draft grids.
     * Returns how many grids received it.
     */
    public static int installUnitQuantityEntry(JComponent comercialPanel, UnitsApiClient unitsApiClient) {
        if (comercialPanel == null || unitsApiClient == null) return 0;
        int installed = 0;
        for (JTable table : mz.multicore.erp.gui.CommercialDraftLineAccess.draftTables(comercialPanel)) {
            if (Boolean.TRUE.equals(table.getClientProperty(UNIT_ENTRY_KEY))) continue;
            Runnable action = () -> {
                Long productId = mz.multicore.erp.gui.CommercialDraftLineAccess.selectedProductId(comercialPanel, table);
                if (productId == null) {
                    mz.multicore.erp.gui.components.ToastManager.show(table, mz.multicore.erp.gui.components.FeedbackType.INFO,
                            "Seleccione uma linha com produto para indicar a quantidade noutra unidade.");
                    return;
                }
                UnitQuantityDialog.show(table, unitsApiClient, productId,
                        qty -> mz.multicore.erp.gui.CommercialDraftLineAccess.setSelectedQuantity(comercialPanel, table, qty));
            };
            table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_DOWN_MASK), UNIT_ENTRY_KEY);
            table.getActionMap().put(UNIT_ENTRY_KEY, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
            });
            JPopupMenu menu = table.getComponentPopupMenu() == null ? new JPopupMenu() : table.getComponentPopupMenu();
            JMenuItem item = new JMenuItem("Quantidade noutra unidade (palete, saco…)",
                    mz.multicore.erp.gui.components.UIHelper.icon("fas-ruler-combined", 13,
                            mz.multicore.erp.gui.components.UIHelper.MODULE_STOCK));
            item.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            item.addActionListener(e -> action.run());
            menu.add(item);
            table.setComponentPopupMenu(menu);
            String tip = table.getToolTipText();
            table.setToolTipText((tip == null || tip.isBlank() ? "" : tip + " · ") + "Ctrl+U: quantidade noutra unidade");
            table.putClientProperty(UNIT_ENTRY_KEY, Boolean.TRUE);
            installed++;
        }
        return installed;
    }

    /** Adds "Adicionar frete" (Ctrl+Alt+F and the right-click menu) to the invoice and order draft grids. */
    public static int installFreightEntry(JComponent comercialPanel, ComercialApiClient comercialApi) {
        if (comercialPanel == null || comercialApi == null) return 0;
        int installed = 0;
        KeyStroke key = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK);
        for (JTable table : mz.multicore.erp.gui.CommercialDraftLineAccess.draftTables(comercialPanel)) {
            if (Boolean.TRUE.equals(table.getClientProperty(FREIGHT_ENTRY_KEY))) continue;
            Runnable action = () -> FreightZoneDialog.show(table, comercialApi, zone -> {
                if (!mz.multicore.erp.gui.CommercialDraftLineAccess.addProduct(comercialPanel, table, zone)) {
                    mz.multicore.erp.gui.components.ToastManager.show(table, mz.multicore.erp.gui.components.FeedbackType.WARNING,
                            "Abra um documento em edição para adicionar o frete.");
                }
            });
            table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, FREIGHT_ENTRY_KEY);
            table.getActionMap().put(FREIGHT_ENTRY_KEY, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
            });
            JPopupMenu menu = table.getComponentPopupMenu() == null ? new JPopupMenu() : table.getComponentPopupMenu();
            JMenuItem item = new JMenuItem("Adicionar frete por zona…",
                    mz.multicore.erp.gui.components.UIHelper.icon("fas-truck", 13, mz.multicore.erp.gui.components.UIHelper.ACCENT_SKY));
            item.setAccelerator(key);
            item.addActionListener(e -> action.run());
            menu.add(item);
            table.setComponentPopupMenu(menu);
            table.putClientProperty(FREIGHT_ENTRY_KEY, Boolean.TRUE);
            installed++;
        }
        return installed;
    }

    /** Lets the order editor list and create the client's projects (SA-C09). */
    public static void attachProjects(JComponent comercialPanel, mz.multicore.erp.desktop.client.ProjectsApiClient projectsApiClient) {
        if (comercialPanel != null && projectsApiClient != null) {
            comercialPanel.putClientProperty(mz.multicore.erp.gui.OrderEditorExtras.PROJECTS_KEY, projectsApiClient);
        }
    }

    /**
     * Makes the add-ons visible in the draft grids: "Unidade" and "Frete" buttons next to "Adicionar linha",
     * the remove button enabled only with a selected line, and the draft total repeated beside the buttons so
     * it is visible without scrolling. Requires {@link #installUnitQuantityEntry} and {@link #installFreightEntry}.
     */
    public static int installDraftToolbarExtras(JComponent comercialPanel) {
        int installed = 0;
        for (JTable table : mz.multicore.erp.gui.CommercialDraftLineAccess.draftTables(comercialPanel)) {
            if (Boolean.TRUE.equals(table.getClientProperty(TOOLBAR_KEY))) continue;
            Container card = SwingUtilities.getAncestorOfClass(mz.multicore.erp.gui.components.ModernPanel.class, table);
            if (card == null) continue;
            AbstractButton add = findButton(card, "adicionar linha");
            AbstractButton remove = findButton(card, "remover");
            if (remove != null) {
                remove.setEnabled(false);
                table.getSelectionModel().addListSelectionListener(e ->
                        remove.setEnabled(mz.multicore.erp.gui.components.TableFilter.selectedModelRow(table) >= 0));
            }
            if (add != null && add.getParent() != null) {
                Container bar = add.getParent();
                int at = indexOf(bar, add);
                bar.add(actionButton("Unidade", "fas-ruler-combined", "Quantidade noutra unidade: palete, saco… (Ctrl+U)", table, UNIT_ENTRY_KEY), at);
                bar.add(actionButton("Frete", "fas-truck", "Adicionar frete por zona de entrega (Ctrl+Alt+F)", table, FREIGHT_ENTRY_KEY), at + 1);
                bar.revalidate();
                installTotalsBar(comercialPanel, table, card);
            }
            table.putClientProperty(TOOLBAR_KEY, Boolean.TRUE);
            installed++;
        }
        return installed;
    }

    /**
     * Professional totals summary (lines, subtotal, VAT, total) on the line under the card title, visible without
     * scrolling. The single-sentence "Total Rascunho" label is hidden; it still drives the refresh, because the
     * editor rewrites it every time the draft changes.
     */
    private static void installTotalsBar(JComponent comercialPanel, JTable table, Container card) {
        javax.swing.JLabel legacyTotal = mz.multicore.erp.gui.CommercialDraftLineAccess.totalLabel(comercialPanel, table);
        javax.swing.JLabel hint = findLabel(card, "edição directa");
        if (hint == null) hint = findLabel(card, "preparação tabular");
        if (legacyTotal == null || hint == null || !(hint.getParent() instanceof JComponent holder)) return;
        DraftTotalsBar bar = new DraftTotalsBar();
        Runnable refresh = () -> {
            DraftTotalsBar.Totals totals = mz.multicore.erp.gui.CommercialDraftLineAccess.totals(comercialPanel, table);
            if (totals != null) bar.show(totals);
        };
        legacyTotal.addPropertyChangeListener("text", e -> refresh.run());
        table.getModel().addTableModelListener(e -> SwingUtilities.invokeLater(refresh));
        legacyTotal.setVisible(false);
        int index = indexOf(holder, hint);
        Object constraints = holder.getLayout() instanceof java.awt.BorderLayout border ? border.getConstraints(hint) : null;
        JPanel row = new JPanel(new java.awt.BorderLayout(12, 0));
        row.setOpaque(false);
        holder.remove(hint);
        row.add(hint, java.awt.BorderLayout.WEST);
        row.add(bar, java.awt.BorderLayout.EAST);
        if (constraints != null) holder.add(row, constraints);
        else holder.add(row, index);
        holder.revalidate();
        refresh.run();
    }

    private static javax.swing.JLabel findLabel(Container root, String textStart) {
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JLabel l && l.getText() != null
                    && l.getText().trim().toLowerCase(java.util.Locale.ROOT).startsWith(textStart)) return l;
            if (child instanceof Container c) {
                javax.swing.JLabel found = findLabel(c, textStart);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Icon-only button so the line toolbar keeps room for the card title; the name stays for screen readers. */
    private static mz.multicore.erp.gui.components.ModernButton actionButton(String text, String icon, String tooltip,
                                                                             JTable table, String actionKey) {
        mz.multicore.erp.gui.components.ModernButton button = mz.multicore.erp.gui.components.UIHelper.createSecondaryButton("");
        button.setIcon(mz.multicore.erp.gui.components.UIHelper.icon(icon, 14));
        button.getAccessibleContext().setAccessibleName(text);
        button.setName(text);
        button.setPreferredSize(new java.awt.Dimension(42, mz.multicore.erp.gui.components.UIHelper.FORM_CONTROL_HEIGHT));
        button.setToolTipText(tooltip);
        button.addActionListener(e -> {
            Action action = table.getActionMap().get(actionKey);
            if (action != null) action.actionPerformed(new java.awt.event.ActionEvent(table, java.awt.event.ActionEvent.ACTION_PERFORMED, actionKey));
        });
        return button;
    }

    private static AbstractButton findButton(Container root, String textStart) {
        for (Component child : root.getComponents()) {
            if (child instanceof AbstractButton b && b.getText() != null
                    && b.getText().trim().toLowerCase(java.util.Locale.ROOT).startsWith(textStart)) return b;
            if (child instanceof Container c) {
                AbstractButton found = findButton(c, textStart);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int indexOf(Container parent, Component child) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == child) return i;
        }
        return parent.getComponentCount();
    }

    public static void attachUnits(JComponent stockPanel, UnitsApiClient unitsApiClient) {
        if (stockPanel != null && unitsApiClient != null) stockPanel.putClientProperty(UNITS_CLIENT_KEY, unitsApiClient);
    }

    /** "Unidades" tab for the product detail, or {@code null} when the Stock panel has no units client. */
    public static JComponent productUnitsTab(JComponent stockPanel, Long productId) {
        if (stockPanel == null || productId == null) return null;
        Object client = stockPanel.getClientProperty(UNITS_CLIENT_KEY);
        return client instanceof UnitsApiClient units ? new ProductUnitsPanel(units, productId) : null;
    }

    private static JTabbedPane directTabbedPane(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane tabs) return tabs;
        }
        return null;
    }
}
