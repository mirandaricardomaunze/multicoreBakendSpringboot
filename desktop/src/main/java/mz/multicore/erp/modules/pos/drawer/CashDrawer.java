package mz.multicore.erp.modules.pos.drawer;

import mz.multicore.erp.desktop.config.DesktopLocalSettings;
import mz.multicore.erp.desktop.config.DesktopLocalSettings.DrawerSettings;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.util.ArrayList;
import java.util.List;

/**
 * Cash drawer of this workstation ({@code docs/GAVETA_DINHEIRO_SPEC.md}). Drawers are wired to the receipt printer
 * and open on the ESC/POS pulse {@code ESC p m t1 t2}. The drawer opens by itself after a sale paid in cash; it is
 * never opened for card, transfer or credit sales. Without a configured printer nothing happens.
 */
public final class CashDrawer {

    private CashDrawer() {
    }

    /** ESC p, pin (0 = pin 2, 1 = pin 5), pulse on 25 x 2 ms, off 250 x 2 ms. */
    public static byte[] pulse(int pin) {
        return new byte[]{0x1B, 0x70, (byte) (pin == 1 ? 1 : 0), 0x19, (byte) 0xFA};
    }

    /** Whether a finished sale with this payment method should open the drawer. */
    public static boolean opensFor(String paymentMethod, DrawerSettings settings) {
        return settings != null && settings.enabled() && settings.hasPrinter() && "CASH".equalsIgnoreCase(paymentMethod);
    }

    /** Opens the drawer in the background after a cash sale; never blocks or fails the sale. */
    public static void afterSale(String paymentMethod) {
        DrawerSettings settings;
        try {
            settings = DesktopLocalSettings.drawerSettings();
        } catch (RuntimeException unreadable) {
            return;
        }
        if (!opensFor(paymentMethod, settings)) return;
        Thread worker = new Thread(() -> {
            try {
                open(settings);
            } catch (RuntimeException ignored) {
                // A drawer that does not open must never undo or delay a finished sale.
            }
        }, "multicore-cash-drawer");
        worker.setDaemon(true);
        worker.start();
    }

    /** Sends the pulse to the configured printer now; throws with a clear message when it cannot. */
    public static void open(DrawerSettings settings) {
        PrintService printer = find(settings.printer());
        if (printer == null) throw new IllegalStateException("Impressora '" + settings.printer() + "' não encontrada neste computador.");
        try {
            DocPrintJob job = printer.createPrintJob();
            job.print(new SimpleDoc(pulse(settings.pin()), DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
        } catch (Exception error) {
            throw new IllegalStateException("A impressora não aceitou o comando da gaveta: " + error.getMessage(), error);
        }
    }

    public static List<String> printers() {
        List<String> names = new ArrayList<>();
        for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) names.add(service.getName());
        return names;
    }

    private static PrintService find(String name) {
        if (name == null || name.isBlank()) return null;
        for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (service.getName().equalsIgnoreCase(name.trim())) return service;
        }
        return null;
    }
}
