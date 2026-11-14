package mz.multicore.erp.gui.components;

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.Icon;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tab icons in one quiet colour (muted, accent when selected) instead of a different colour per tab. */
final class UiTabIcons {

    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    private UiTabIcons() {}

    /** Same glyph in another colour; icons that are not font glyphs are returned unchanged. */
    static Icon tinted(Icon icon, Color color) {
        if (!(icon instanceof FontIcon font) || color == null) return icon;
        String key = font.getIkon().getDescription() + '|' + font.getIconSize() + '|' + color.getRGB();
        return CACHE.computeIfAbsent(key, k -> {
            FontIcon copy = FontIcon.of(font.getIkon(), font.getIconSize());
            copy.setIconColor(color);
            return copy;
        });
    }
}
