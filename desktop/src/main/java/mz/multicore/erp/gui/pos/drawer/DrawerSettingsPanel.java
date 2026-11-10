package mz.multicore.erp.gui.pos.drawer;

import mz.multicore.erp.desktop.config.DesktopLocalSettings;
import mz.multicore.erp.desktop.config.DesktopLocalSettings.DrawerSettings;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pos.drawer.CashDrawer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * "Gaveta deste Posto" (Configurações): the cash drawer wired to this computer's receipt printer. Saved in the
 * local file of the workstation. "Testar Abertura" sends the pulse now, so the installer confirms the wiring.
 */
public class DrawerSettingsPanel extends JPanel {

    private final JCheckBox enabled = new JCheckBox("Abrir a gaveta no fim de cada venda paga em numerário");
    private final JComboBox<String> printer = new JComboBox<>();
    private final JComboBox<String> pin = new JComboBox<>(new String[]{"Pino 2 (o mais comum)", "Pino 5"});

    public DrawerSettingsPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        enabled.setOpaque(false);
        enabled.setForeground(UIHelper.TEXT_LIGHT);
        printer.setEditable(true);
        UIHelper.styleComboBox(printer);
        UIHelper.styleComboBox(pin);
        JLabel help = new JLabel("<html><div style='width:520px'>A gaveta liga-se à impressora de talões (cabo RJ11). Escolha essa "
                + "impressora e o pino indicado no manual da gaveta. A gaveta só abre em vendas a numerário: cartão, "
                + "transferência e crédito não a abrem.</div></html>");
        help.setForeground(UIHelper.TEXT_MUTED);
        ModernButton save = UIHelper.createSuccessButton("Gravar");
        save.setIcon(UIHelper.icon("fas-save", 14));
        save.addActionListener(e -> save());
        ModernButton test = UIHelper.createSecondaryButton("Testar Abertura");
        test.setIcon(UIHelper.icon("fas-cash-register", 14));
        test.addActionListener(e -> test());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(save);
        actions.add(test);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", help, "", enabled, "Impressora de talões:", printer,
                "Ligação da gaveta:", pin, "", actions});
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.add(form, BorderLayout.NORTH);
        add(card, BorderLayout.NORTH);
        load();
    }

    private void load() {
        CashDrawer.printers().forEach(printer::addItem);
        DrawerSettings settings = DesktopLocalSettings.drawerSettings();
        enabled.setSelected(settings.enabled());
        printer.setSelectedItem(settings.printer());
        pin.setSelectedIndex(settings.pin() == 1 ? 1 : 0);
    }

    private DrawerSettings current() {
        return new DrawerSettings(enabled.isSelected(), printer.getSelectedItem() == null ? "" : printer.getSelectedItem().toString().trim(),
                pin.getSelectedIndex() == 1 ? 1 : 0);
    }

    private void save() {
        try {
            DesktopLocalSettings.saveDrawerSettings(current());
            ToastManager.success(this, enabled.isSelected() ? "Gaveta ligada à impressora " + current().printer() + "." : "Gaveta desligada neste posto.");
        } catch (RuntimeException error) {
            ToastManager.show(this, FeedbackType.ERROR, error.getMessage());
        }
    }

    private void test() {
        DrawerSettings settings = current();
        if (!settings.hasPrinter()) {
            ToastManager.show(this, FeedbackType.WARNING, "Escolha primeiro a impressora de talões.");
            return;
        }
        UIHelper.runWithProgress(this, "A abrir a gaveta…", () -> {
            CashDrawer.open(settings);
            return null;
        }, ok -> ToastManager.success(this, "Comando enviado. Se a gaveta não abriu, experimente o outro pino."),
                error -> ToastManager.show(this, FeedbackType.ERROR, error.getMessage()));
    }
}
