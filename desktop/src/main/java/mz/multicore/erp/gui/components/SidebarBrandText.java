package mz.multicore.erp.gui.components;

/** Duas linhas em vez de reticências: empresas do mesmo grupo diferem muitas vezes no fim do nome. */
final class SidebarBrandText {

    private static final int TEXT_WIDTH = 118;

    private SidebarBrandText() {
    }

    static String wrapped(String name) {
        String safe = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:" + TEXT_WIDTH + "px'>" + safe + "</div></html>";
    }
}
