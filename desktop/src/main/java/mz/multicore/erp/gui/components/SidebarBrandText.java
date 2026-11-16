package mz.multicore.erp.gui.components;

/** Uses two lines because companies in the same group often differ only at the end of their names. */
final class SidebarBrandText {

    /** Fits before the collapse button; at 118 pixels the name extended beneath the button. */
    private static final int TEXT_WIDTH = 104;

    private SidebarBrandText() {
    }

    static String wrapped(String name) {
        String safe = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:" + TEXT_WIDTH + "px'>" + safe + "</body></html>";
    }
}
