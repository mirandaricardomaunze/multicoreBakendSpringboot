package mz.multicore.erp.gui.components;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class UiIconAndImageSupport {

    private static volatile List<Image> cachedAppIcons = null;

    private UiIconAndImageSupport() {}

    static Icon icon(String code, int size, Color color) {
        Color resolvedColor = (color != null) ? color : Color.WHITE;
        int resolvedSize = size > 0 ? size : UIHelper.ICON_MD;
        try {
            if (code == null || code.isBlank()) {
                return fallbackIcon(resolvedSize, resolvedColor);
            }
            FontAwesomeSolid fa = FontAwesomeSolid.valueOf(toFaEnum(code));
            FontIcon fi = FontIcon.of(fa);
            fi.setIconSize(resolvedSize);
            fi.setIconColor(resolvedColor);
            return fi;
        } catch (Exception ex) {
            return fallbackIcon(resolvedSize, resolvedColor);
        }
    }

    private static Icon fallbackIcon(int size, Color color) {
        try {
            FontIcon fi = FontIcon.of(FontAwesomeSolid.QUESTION_CIRCLE);
            fi.setIconSize(size > 0 ? size : UIHelper.ICON_MD);
            fi.setIconColor(color != null ? color : UIHelper.TEXT_LIGHT);
            return fi;
        } catch (Exception ignored) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(color != null ? color : Color.GRAY);
                    g2.drawOval(x, y, getIconWidth() - 1, getIconHeight() - 1);
                    g2.dispose();
                }
                @Override public int getIconWidth() { return size > 0 ? size : 16; }
                @Override public int getIconHeight() { return size > 0 ? size : 16; }
            };
        }
    }

    static Color semanticColorFor(String iconCode) {
        if (iconCode == null || iconCode.isBlank()) return UIHelper.ACCENT_BLUE;
        String code = iconCode.toLowerCase().trim();
        if (code.contains("boxes") || code.contains("box") || code.contains("warehouse") || code.contains("cubes") || code.contains("tag") || code.contains("barcode") || code.contains("dolly")) {
            return UIHelper.MODULE_STOCK;
        }
        if (code.contains("user") || code.contains("users") || code.contains("id-card") || code.contains("calendar") || code.contains("clock") || code.contains("briefcase")) {
            return UIHelper.MODULE_HR;
        }
        if (code.contains("percent") || code.contains("tax") || code.contains("calculator") || code.contains("receipt") || code.contains("file-invoice") || code.contains("balance-scale")) {
            return UIHelper.MODULE_FISCAL;
        }
        if (code.contains("cart") || code.contains("cash") || code.contains("store") || code.contains("credit-card") || code.contains("shopping")) {
            return UIHelper.MODULE_POS;
        }
        if (code.contains("truck") || code.contains("shipping") || code.contains("shopping-basket")) {
            return UIHelper.MODULE_COMPRAS;
        }
        if (code.contains("chart") || code.contains("money") || code.contains("coins") || code.contains("wallet")) {
            return UIHelper.MODULE_FINANCEIRO;
        }
        if (code.contains("handshake") || code.contains("address-book") || code.contains("headset")) {
            return UIHelper.MODULE_CRM;
        }
        if (code.contains("shield") || code.contains("lock") || code.contains("key") || code.contains("user-shield")) {
            return UIHelper.ACCENT_CYAN;
        }
        if (code.contains("exclamation") || code.contains("trash") || code.contains("times") || code.contains("ban")) {
            return UIHelper.REJECTED_RED;
        }
        if (code.contains("check") || code.contains("check-circle")) {
            return UIHelper.APPROVED_GREEN;
        }
        return UIHelper.ACCENT_BLUE;
    }

    private static String toFaEnum(String code) {
        String s = code.startsWith("fas-") ? code.substring(4) : code;
        return s.replace('-', '_').toUpperCase();
    }

    static Image iconImage(String code, int size, Color color) {
        int resolvedSize = size > 0 ? size : UIHelper.ICON_MD;
        Icon ic = icon(code, resolvedSize, color);
        BufferedImage img = new BufferedImage(resolvedSize, resolvedSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ic.paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    static List<Image> getAppIcons() {
        if (cachedAppIcons != null) return cachedAppIcons;
        List<Image> icons = new ArrayList<>();
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        for (int s : sizes) {
            try {
                URL url = UIHelper.class.getResource("/icons/icon-" + s + ".png");
                if (url != null) {
                    BufferedImage img = ImageIO.read(url);
                    if (img != null) icons.add(img);
                }
            } catch (Exception ignored) {}
        }
        if (icons.isEmpty()) {
            try {
                URL mainUrl = UIHelper.class.getResource("/icons/app-icon.png");
                if (mainUrl != null) {
                    BufferedImage img = ImageIO.read(mainUrl);
                    if (img != null) icons.add(img);
                }
            } catch (Exception ignored) {}
        }
        if (icons.isEmpty()) {
            icons.add(iconImage("fas-cube", 64, UIHelper.ACCENT));
        }
        cachedAppIcons = Collections.unmodifiableList(icons);
        return cachedAppIcons;
    }

    static Image getAppIcon(int size) {
        List<Image> icons = getAppIcons();
        for (Image img : icons) {
            if (img.getWidth(null) == size) return img;
        }
        if (!icons.isEmpty()) {
            Image best = icons.get(icons.size() - 1);
            if (best.getWidth(null) == size) return best;
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(best, 0, 0, size, size, null);
            g2.dispose();
            return scaled;
        }
        return iconImage("fas-cube", size, UIHelper.ACCENT);
    }

    static byte[] readScaledImage(File file, int maxDim) {
        try {
            BufferedImage src = ImageIO.read(file);
            if (src == null) return null;
            int w = src.getWidth(), h = src.getHeight();
            double scale = Math.min(1.0, (double) maxDim / Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(dst, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            return null;
        }
    }

    static ImageIcon imageIconFromBytes(byte[] data, int w, int h) {
        if (data == null || data.length == 0) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return null;
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (IOException ex) {
            return null;
        }
    }

    static String iconForTitle(String title) {
        String t = title == null ? "" : title.toLowerCase();
        if (t.contains("transfer")) return "fas-exchange-alt";
        if (t.contains("fatura")) return "fas-file-invoice";
        if (t.contains("recibo") || t.contains("recebimento")) return "fas-receipt";
        if (t.contains("nota de crédito") || t.contains("nota de credito")) return "fas-file-invoice-dollar";
        if (t.contains("nota de débito") || t.contains("nota de debito")) return "fas-file-invoice-dollar";
        if (t.contains("fornecedor")) return "fas-truck";
        if (t.contains("encomenda")) return "fas-file-signature";
        if (t.contains("compra") || t.contains("entrada")) return "fas-download";
        if (t.contains("ajuste") || t.contains("contagem") || t.contains("invent")) return "fas-clipboard-list";
        if (t.contains("categoria")) return "fas-tags";
        if (t.contains("lote") || t.contains("validade")) return "fas-boxes";
        if (t.contains("cliente")) return "fas-address-book";
        if (t.contains("produto")) return "fas-boxes";
        if (t.contains("armazém") || t.contains("armazem")) return "fas-warehouse";
        if (t.contains("promo")) return "fas-percent";
        if (t.contains("funcionário") || t.contains("funcionario") || t.contains("colaborador")) return "fas-users";
        if (t.contains("salário") || t.contains("salario") || t.contains("recibo de")) return "fas-file-invoice-dollar";
        if (t.contains("falta")) return "fas-user-times";
        if (t.contains("férias") || t.contains("ferias")) return "fas-umbrella-beach";
        if (t.contains("despesa")) return "fas-receipt";
        if (t.contains("imposto") || t.contains("iva") || t.contains("taxa") || t.contains("fiscal")) return "fas-percent";
        if (t.contains("utilizador") || t.contains("user")) return "fas-user-plus";
        if (t.contains("editar")) return "fas-edit";
        if (t.contains("pagar") || t.contains("pagamento")) return "fas-money-bill-wave";
        if (t.contains("novo") || t.contains("nova") || t.contains("cadastrar")
                || t.contains("registar") || t.contains("adicionar") || t.contains("criar")) return "fas-plus";
        return "fas-file-alt";
    }

    private static JComponent iconBadge(String iconCode, int badgeSize, int iconSize) {
        final Icon ic = icon(iconCode, iconSize, Color.WHITE);
        JComponent badge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIHelper.ACCENT_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UIHelper.RADIUS_MD, UIHelper.RADIUS_MD);
                int ix = (getWidth() - ic.getIconWidth()) / 2;
                int iy = (getHeight() - ic.getIconHeight()) / 2;
                ic.paintIcon(this, g2, ix, iy);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        Dimension d = new Dimension(badgeSize, badgeSize);
        badge.setPreferredSize(d);
        badge.setMinimumSize(d);
        badge.setMaximumSize(d);
        return badge;
    }

    static JComponent buildPremiumHeader(String iconCode, String title, String subtitle) {
        String code = iconCode != null ? iconCode : iconForTitle(title);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 19));
        titleLbl.setForeground(UIHelper.TEXT_LIGHT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(titleLbl);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subLbl = new JLabel(subtitle);
            subLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            subLbl.setForeground(UIHelper.TEXT_MUTED);
            subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            subLbl.setBorder(new EmptyBorder(2, 0, 0, 0));
            texts.add(subLbl);
        }

        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(iconBadge(code, 46, 22));
        row.add(badgeWrap, BorderLayout.WEST);
        row.add(texts, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(row, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIHelper.GRID),
                new EmptyBorder(0, 0, 14, 0)));
        return header;
    }
}
